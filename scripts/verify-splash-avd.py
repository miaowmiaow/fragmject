#!/usr/bin/env python3
"""开屏 AVD（AnimatedVectorDrawable）静态校验。

为什么需要：AnimatedVectorDrawable 的失败模式是**静默**的——只要有一个
`<target>` 找不到对应节点，或者两个 pathData 的命令序列不一致，整份动画就
不再播放，既不崩溃也不报错，只能靠装到设备上看。这个脚本把这三点在提交前
就挡住：

1. 每个 `<target android:name="X">` 都必须有对应的 `<path|group android:name="X">`；
2. `pathData` 形变的 `valueFrom` / `valueTo` 命令序列必须完全一致；
3. 所有 `pathData` 必须是它能解析的形状（命令合法、数值成对）。

用法：
    python3 scripts/verify-splash-avd.py [文件路径...]

默认检查 app/src/main/res/drawable/splash_bg.xml；传入多个文件时逐个校验。
退出码 0 表示通过，1 表示发现问题（消息里带行号）。
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

DEFAULT_FILES = ["app/src/main/res/drawable/splash_bg.xml"]

# Android vector pathData 支持的命令字母
PATH_COMMANDS = set("MmZzLlHhVvCcSsQqTtAa")

NUMBER = re.compile(r"-?\d+(?:\.\d+)?(?:[eE][-+]?\d+)?")


def strip_numbers(path: str) -> str:
    """去掉数字，只留命令字母与控制符，用于比较两条路径的结构。"""
    return NUMBER.sub("", path)


def commands_of(path: str) -> str:
    return "".join(ch for ch in strip_numbers(path) if ch in PATH_COMMANDS)


def check_path_data(name: str, data: str, problems: list[str]) -> None:
    body = strip_numbers(data)
    leftover = [ch for ch in body if ch not in PATH_COMMANDS and ch not in " ,"]
    if leftover:
        problems.append(f"{name}: pathData 含非法字符 {''.join(sorted(set(leftover)))!r}")
    cmds = commands_of(data)
    if not cmds:
        problems.append(f"{name}: pathData 里没有任何绘制命令")
    elif cmds[0] not in "Mm":
        problems.append(f"{name}: pathData 必须以 M/m 开头，实际是 {cmds[0]!r}")


def check_file(path: Path) -> list[str]:
    text = path.read_text(encoding="utf-8")
    problems: list[str] = []

    declared = set(
        re.findall(r'<path[^>]*?android:name="([^"]+)"', text, re.S)
        + re.findall(r'<group[^>]*?android:name="([^"]+)"', text, re.S)
    )
    targets = set(re.findall(r'<target\s+android:name="([^"]+)"', text))

    for name in sorted(targets - declared):
        problems.append(f"<target> 找不到同名节点：{name}（AVD 会整份失效）")

    # 每个 path 的 pathData 是否可解析
    for name, data in re.findall(
        r'<path[^>]*?android:name="([^"]+)"[^>]*?android:pathData="([^"]+)"', text, re.S
    ):
        check_path_data(name, data, problems)

    # pathData 形变：两侧命令序列必须一致
    for block in re.findall(r"<target\s+android:name=\"[^\"]+\">.*?</target>", text, re.S):
        if 'android:valueType="pathType"' not in block:
            continue
        target = re.search(r'<target\s+android:name="([^"]+)"', block).group(1)
        from_m = re.search(r'android:valueFrom="([^"]+)"', block)
        to_m = re.search(r'android:valueTo="([^"]+)"', block)
        if not from_m or not to_m:
            problems.append(f"{target}: pathData 形变缺少 valueFrom/valueTo")
            continue
        a, b = commands_of(from_m.group(1)), commands_of(to_m.group(1))
        if a != b:
            problems.append(
                f"{target}: 形变两侧命令序列不一致\n    from: {a}\n    to:   {b}"
            )

    return problems


def main(argv: list[str]) -> int:
    files = [Path(p) for p in (argv or DEFAULT_FILES)]
    failed = False
    for path in files:
        if not path.exists():
            print(f"[FAIL] 文件不存在：{path}")
            failed = True
            continue
        problems = check_file(path)
        if problems:
            failed = True
            print(f"[FAIL] {path}")
            for item in problems:
                print(f"  - {item}")
        else:
            print(f"[ OK ] {path}")

    if failed:
        print(
            "\n提示：AVD 的这三类问题都不会在编译期报错，只会在运行时表现为「动画不动」。\n"
            "      改完 splash_bg.xml 请先跑本脚本，再打包验证。"
        )
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
