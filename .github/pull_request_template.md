## 变更说明

<!-- 做了什么、为什么；关联 issue / 需求编号 -->

## 自检

- [ ] `./gradlew testDebugUnitTest` 通过
- [ ] `./gradlew detekt` 通过
- [ ] `./gradlew verifyUnitTestCoverage` 通过（需先跑 `unitTestCoverageReport`）
- [ ] 涉及 UI 的改动已在真机或模拟器上自测
- [ ] 涉及模型分层（新增 `*Dto` / 实体 / mapper 层）时，已对照 [`docs/adr/0001-shared-protocol-model.md`](docs/adr/0001-shared-protocol-model.md) §5 的触发条件并附证据

## 依赖升级（仅升级 PR 填写）

升级验证固定三步，结论附在本节，未填写不予合入：

- [ ] 联动版本（AGP / Kotlin / KSP / Compose BOM）在同一次提交内完成，未单独抬升其中一个
- [ ] 第 1 步：`./gradlew testDebugUnitTest` 通过
- [ ] 第 2 步：`./gradlew assembleDebug` 通过
- [ ] 第 3 步：真机冒烟 —— 登录 / 列表分页 / WebView / 扫码 / 图片编辑

## 风险与回滚

<!-- 可能的回归点、如何回滚；无风险可写「无」 -->
