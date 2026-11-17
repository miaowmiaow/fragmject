#!/usr/bin/env bash
# 第 7 项：release mapping 完整性检查。
# 关键反射注册点（Firebase 组件发现）与 native 方法名不得被 R8 重命名。
# 用法：scripts/check-release-keep.sh [mapping.txt]    退出码 0=通过，1=失败
set -euo pipefail

MAPPING="${1:-app/build/outputs/mapping/release/mapping.txt}"
if [[ ! -f "$MAPPING" ]]; then
    echo "FAIL: mapping 不存在：$MAPPING（先执行 ./gradlew :app:assembleRelease）"
    exit 1
fi

# 类内成员被重命名的判定：getComponents / native 方法行的新名必须与原名一致
check_members() {
    local cls="$1"; shift
    local members=("$@")
    local block
    block=$(awk -v k="$cls" 'index($0, k " ->") == 1 { p = 1; next } /^[^ #]/ { p = 0 } p' "$MAPPING")
    if [[ -z "$block" ]]; then
        echo "FAIL: $cls 不在 mapping 中（类被裁剪或改名）"
        return 1
    fi
    local bad=0
    for m in "${members[@]}"; do
        local renamed
        renamed=$(echo "$block" | awk -v m="$m(" '
            index($0, m) > 0 && / -> / {
                n = split($0, a, " -> "); name = a[n]; sub(/[[:space:]]+$/, "", name)
                if (name != "'"$m"'") print name
            }')
        if [[ -n "$renamed" ]]; then
            echo "FAIL: $cls.$m 被重命名为：$(echo "$renamed" | head -1)"
            bad=1
        fi
    done
    return $bad
}

fail=0
check_members "com.google.mlkit.vision.barcode.internal.BarcodeRegistrar" getComponents || fail=1
check_members "com.google.mlkit.vision.common.internal.VisionCommonRegistrar" getComponents || fail=1
check_members "com.google.mlkit.common.internal.CommonComponentRegistrar" getComponents || fail=1

NATIVE_METHODS=(
    installAntiPtrace readTracerPid countAnonymousExecSegments findFridaInMaps findRootInMaps
    findFridaThreads findFridaFiles findRootFiles verifyLibcTextCrc verifySelfSoTextCrc
    verifySelfSoDataCrc killSelf hardenProcess countPltHooks verifyApkSignature
)
check_members "com.example.security.SecurityNative" "${NATIVE_METHODS[@]}" || fail=1

if [[ $fail -eq 0 ]]; then
    echo "PASS: 关键注册类与 native 方法名在 mapping 中保持不变"
else
    echo "FAIL: 存在被 R8 重命名的关键成员，请检查 app/proguard-rules.pro 与 security/consumer-rules.pro"
fi
exit $fail
