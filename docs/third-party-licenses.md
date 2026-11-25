# 第三方组件与许可清单

随 APK 分发与仅构建期使用的第三方组件、版本与许可证。事实来源是 `gradle/libs.versions.toml`
（本表于 2026-11-21 与 toml 核对）；两者不一致时以 toml 为准，并在同一次改动里同步本表。

维护规则见 [`dependencies-policy.md` §7](./dependencies-policy.md)。

## 一、随 APK 分发（运行时依赖）

| 组件 | 版本 | 许可证（SPDX） |
|---|---|---|
| AndroidX Compose BOM（runtime / ui / material3 / animation / icons / tooling-preview） | 2026.11.01 | Apache-2.0 |
| androidx.activity（含 activity-compose） | 1.13.0 | Apache-2.0 |
| androidx.core:core-ktx | 1.19.1 | Apache-2.0 |
| androidx.core:core-splashscreen | 1.2.0 | Apache-2.0 |
| androidx.lifecycle（runtime-compose / viewmodel-compose / viewmodel-ktx / viewmodel-navigation3） | 2.11.1 | Apache-2.0 |
| androidx.navigation3（runtime / ui） | 1.2.2 | Apache-2.0 |
| androidx.paging（common / runtime / compose） | 3.5.3 | Apache-2.0 |
| androidx.room3（runtime） | 3.0.3 | Apache-2.0 |
| androidx.camera（core / camera2 / lifecycle / view / mlkit-vision） | 1.7.0 | Apache-2.0 |
| androidx.hilt:hilt-lifecycle-viewmodel-compose | 1.4.0 | Apache-2.0 |
| androidx.profileinstaller | 1.4.1 | Apache-2.0 |
| Kotlin 标准库 / 反射（kotlin-stdlib / kotlin-reflect） | 2.4.21 | Apache-2.0 |
| kotlinx.coroutines（core / android） | 1.11.0 | Apache-2.0 |
| Dagger / Hilt（hilt-android） | 2.60.1 | Apache-2.0 |
| javax.inject | 1 | Apache-2.0 |
| Gson | 2.15.0 | Apache-2.0 |
| OkHttp | 5.5.1 | Apache-2.0 |
| Retrofit（retrofit + converter-gson） | 3.0.0 | Apache-2.0 |
| Coil（compose / gif / svg / video） | 2.7.0 | Apache-2.0 |
| Google ML Kit barcode-scanning（bundled） | 17.3.0 | **闭源**，Google ML Kit Terms of Service |

## 二、仅构建与测试（不进产物）

| 组件 | 版本 | 许可证（SPDX） |
|---|---|---|
| Android Gradle Plugin（application / library / test） | 9.5.1 | Apache-2.0 |
| Kotlin Gradle Plugin / Compose Compiler Plugin | 2.4.21 | Apache-2.0 |
| KSP | 2.3.6 | Apache-2.0 |
| Hilt Gradle Plugin | 2.60.1 | Apache-2.0 |
| Room3 Gradle Plugin | 3.0.3 | Apache-2.0 |
| androidx.baselineprofile Gradle Plugin | 1.5.0 | Apache-2.0 |
| Detekt | 1.23.8 | Apache-2.0 |
| ASM / asm-tree（字符串加密插桩） | 9.11.1 | BSD-3-Clause |
| Konsist（架构规则测试） | 0.18.1 | Apache-2.0 |
| JaCoCo（Gradle 内置覆盖率插件） | 随 Gradle | EPL-2.0 |
| JUnit 4 | 4.13.2 | EPL-1.0 |
| Robolectric | 4.18.1 | MIT |
| Roborazzi（roborazzi / -compose / -junit-rule） | 1.79.0 | Apache-2.0 |
| androidx.appcompat:appcompat | 1.7.0 | Apache-2.0 |
| AndroidX Test（core / runner / ext-junit / uiautomator） | 1.7.0 / 1.7.0 / 1.3.0 / 2.4.1 | Apache-2.0 |
| Compose ui-test（junit4 / manifest）与 ui-tooling | 随 Compose BOM | Apache-2.0 |
| androidx.benchmark:benchmark-macro-junit4（仅基准测试 APK） | 1.5.0 | Apache-2.0 |

## 三、需要单独说明的组件

### 3.1 Google ML Kit barcode-scanning

- **不是开源组件**：适用 Google ML Kit Terms of Service，不得按 Apache-2.0 处理。
- 使用的是 bundled 变体：识别模型随 APK 分发，条码识别全程在设备端完成，不上传相机图像、
  不发起网络请求（`core/qrscanner` 内无网络调用）。
- 对外分发时必须在隐私政策中单列该 SDK，并说明「相机画面仅本地处理」。

### 3.2 Apache-2.0 的署名与声明义务

- 以二进制形式分发时，需要保留各组件的版权声明、许可证文本与免责声明。
- 本项目的履约方式：README「主要开源库」+ 本清单。若未来提供正式对外分发包，
  必须把本清单随包附带，或在应用内提供可访问的第三方许可页面。

## 四、维护规则

- 新增运行时依赖（`implementation` / `api`）：同一 PR 内登记本表；仅构建与测试的依赖也要登记，
  并归入第二节。
- 许可证列写 SPDX 标识；闭源 SDK 必须写明适用条款，不得含糊成「开源」。
- 引入新许可证前先确认与本项目 Apache-2.0 的分发条款兼容（GPL / AGPL 类 copyleft 会改变分发义务）。
- 当前**没有**自动校验（未接 license-report 任务），靠本表与 `libs.versions.toml` 对账 + PR 评审兜底。
