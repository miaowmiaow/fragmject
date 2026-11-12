#!/usr/bin/env bash
#
# 壳包冒烟校验：装包 → 启动 → 断言壳链路与安全库都工作 → 断言没有被误杀。
#
# 用法：
#   scripts/verify-shell-apk.sh [APK 路径] [adb 序列号]
#
# 默认 APK：app/build/outputs/apk/release/app-release-shell.apk
# 默认设备：${ANDROID_SERIAL}，未设置则由 adb 自行选择（仅一台设备时）。
#
# 为什么写成脚本而不是内联在 CI 里：本地（arm64 模拟器）与 CI（x86_64 模拟器）
# 需要跑**完全相同**的断言；脚本化之后两边命令一致，改一处即可。
set -euo pipefail

APK="${1:-app/build/outputs/apk/release/app-release-shell.apk}"
SERIAL="${2:-${ANDROID_SERIAL:-}}"
PKG="com.example.fragment.project"
ACTIVITY="$PKG/com.example.fragmject.app.MainActivity"
LOGDIR="${LOGDIR:-build/verify-logs}"

if [ -n "$SERIAL" ]; then
  ADB=(adb -s "$SERIAL")
else
  ADB=(adb)
fi

fail() {
  echo "❌ $*" >&2
  "${ADB[@]}" logcat -d > "$LOGDIR/full-logcat.txt" 2>/dev/null || true
  "${ADB[@]}" logcat -d -b crash > "$LOGDIR/crash.txt" 2>/dev/null || true
  echo "日志已保存到 $LOGDIR/" >&2
  exit 1
}

mkdir -p "$LOGDIR"
test -f "$APK" || { echo "找不到 APK：$APK" >&2; exit 1; }

echo "== 1/4 校验 APK 结构 =="
# 清单只导出一次：`unzip -l | grep -q` 在 pipefail 下会因 SIGPIPE 返回 141（实测踩过）
unzip -l "$APK" > "$LOGDIR/apk-listing.txt"
grep -q "assets/payload/dex-0.bin" "$LOGDIR/apk-listing.txt" || fail "缺少载荷 dex-0.bin"
grep -q "assets/payload/sum.txt" "$LOGDIR/apk-listing.txt" || fail "缺少载荷摘要 sum.txt"
grep -q "assets/payload/shell.txt" "$LOGDIR/apk-listing.txt" || fail "缺少壳 dex 清单 shell.txt"
if grep -q "classes2.dex" "$LOGDIR/apk-listing.txt"; then
  fail "业务 dex 未被抽走（存在 classes2.dex）"
fi
DEX_SIZE=$(awk '$4=="classes.dex"{print $1}' "$LOGDIR/apk-listing.txt")
test -n "$DEX_SIZE" || fail "找不到 classes.dex"
test "$DEX_SIZE" -lt 100000 || fail "壳 dex 体积异常：$DEX_SIZE"
echo "   cls.dex=${DEX_SIZE}B ✅"

echo "== 2/4 安装 =="
"${ADB[@]}" install -r "$APK" | tail -1

echo "== 3/4 启动 =="
"${ADB[@]}" logcat -c || true
"${ADB[@]}" shell am force-stop "$PKG" || true
"${ADB[@]}" shell am start -W -n "$ACTIVITY" > "$LOGDIR/am-start.txt" 2>&1 || true
grep -q "Status: ok" "$LOGDIR/am-start.txt" || fail "Activity 未成功启动：$(cat "$LOGDIR/am-start.txt")"
# 等首轮检测 + 可能的 1.2s 阻断窗口过去
sleep 10

echo "== 4/4 断言 =="
"${ADB[@]}" logcat -d -s DexShell:V AppSecurity:V FragmjectSecurity:V > "$LOGDIR/security.txt" 2>&1 || true
# 期望值用正则：JNI 注册个数会随能力增加（13 → 14），写死数字会让脚本随功能变化而假失败
for expect in "shell dex check ok" "payload sum ok" "registered [0-9]+ methods" "process hardened"; do
  grep -qE "$expect" "$LOGDIR/security.txt" || fail "日志缺少「${expect}」"
  echo "   $expect ✅"
done

PID=$("${ADB[@]}" shell pidof "$PKG" 2>/dev/null | tr -d '\r' || true)
test -n "$PID" || fail "进程已退出（应存活）"
echo "   pid=$PID ✅"

if grep -q "check hit" "$LOGDIR/security.txt"; then
  fail "出现意料之外的命中（误杀）：$(grep "check hit" "$LOGDIR/security.txt" | head -1)"
fi
echo "   no check hit ✅"

# PLT/GOT 检测只在 -PenablePltCheck=true 的构建里开启；开启时断言「没有被劫持」。
# 不同系统版本上 dyn 段解析可能失败（native 返回 -1、不打这行日志），因此这里是
# 「有则必须 hooks=0」的条件断言，而不是强制存在。
if grep -q "plt check: slots=" "$LOGDIR/security.txt"; then
  PLT=$(grep -o "plt check: slots=[0-9]* hooks=[0-9]*" "$LOGDIR/security.txt" | tail -1)
  HOOKS=$(echo "$PLT" | sed 's/.*hooks=//')
  test "$HOOKS" = "0" || fail "PLT/GOT 检测报告 $HOOKS 处劫持（需确认是误报还是真被 hook）：$PLT"
  echo "   plt hooks=0 ✅（${PLT}）"
else
  echo "   plt check 未执行（该构建未开启或 native 解析不可用，跳过）"
fi

echo "✅ 壳包冒烟通过：${APK}"
