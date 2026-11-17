#!/usr/bin/env bash
# 第 8 项：签名门禁端到端检查（改签阻断）。
# 警告：本脚本会卸载目标应用，清除其本地数据。需显式确认：CONFIRM_CLEAR_DATA=yes
# 用法：CONFIRM_CLEAR_DATA=yes [ADB_SERIAL=<serial>] scripts/check-signature-guard.sh [release.apk]
# 退出码：0=通过（改签包被阻断），1=失败
set -euo pipefail

if [[ "${CONFIRM_CLEAR_DATA:-}" != "yes" ]]; then
    echo "拒绝执行：本脚本会卸载应用并清除数据。确认后设置 CONFIRM_CLEAR_DATA=yes 再运行。"
    exit 2
fi

PKG="com.example.fragment.project"
ACT="com.example.fragmject.app.MainActivity"
APK="${1:-app/build/outputs/apk/release/app-release.apk}"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BT="$(ls -d "$SDK"/build-tools/*/ | sort -V | tail -1)"
ADB=(adb)
[[ -n "${ADB_SERIAL:-}" ]] && ADB=(adb -s "$ADB_SERIAL")
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# 1) 用一次性测试证书改签（不复用任何仓库内密钥，避免误用）
keytool -genkeypair -keystore "$WORK/t.jks" -storepass tamper123 -keypass tamper123 \
    -alias tamper -keyalg RSA -keysize 2048 -validity 1 -dname "CN=signature-guard-check" >/dev/null 2>&1
"$BT/zipalign" -f -p 4 "$APK" "$WORK/aligned.apk"
"$BT/apksigner" sign --ks "$WORK/t.jks" --ks-pass pass:tamper123 --key-pass pass:tamper123 \
    --ks-key-alias tamper --out "$WORK/tampered.apk" "$WORK/aligned.apk"

# 2) 卸载原包并安装改签包
"${ADB[@]}" uninstall "$PKG" >/dev/null 2>&1 || true
"${ADB[@]}" install "$WORK/tampered.apk" | tr -d '\r' | tail -1
"${ADB[@]}" logcat -c

# 3) 冷启动并等待命中
"${ADB[@]}" shell am start -S -W -n "$PKG/$ACT" >/dev/null 2>&1 || true
hit=0; native=0; killed=0
for _ in $(seq 1 20); do
    sleep 1
    LOG="$("${ADB[@]}" logcat -d -v brief 2>/dev/null | tr -d '\r')"
    grep -q "security check hit: \[integrity.signature\]" <<<"$LOG" && hit=1
    grep -q "native signature mismatch" <<<"$LOG" && native=1
    grep -q -E "jniKillSelf invoked|terminating after" <<<"$LOG" && killed=1
    if [[ $killed -eq 1 && ( $hit -eq 1 || $native -eq 1 ) ]]; then break; fi
done

# 4) 结论：检测来源可以是 Java 层（integrity.signature）或 native 守护（独立终止），两者任一即可；必须有终止证据
echo "evidence: java_hit=$hit native_mismatch=$native kill=$killed"
if [[ ( $hit -eq 1 || $native -eq 1 ) && $killed -eq 1 ]]; then
    echo "PASS: 改签包被签名门禁识别并终止"
    status=0
else
    echo "FAIL: 改签包未被阻断，签名门禁可能失效"
    status=1
fi
echo "提示：测试完成后请重新安装正式 release 包（数据已被清除）。"
exit $status
