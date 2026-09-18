# 依赖版本策略（Bleeding-edge Policy）

> 本文件记录 fragmject 的依赖版本选型与升级约定，避免「版本冒进」演变为不可控的技术债。

## 1. 当前策略

项目刻意采用**前沿版本**（Bleeding-edge），以第一时间跟进 Android / Kotlin / Compose 生态的新 API：

| 依赖 | 现状 | 说明 |
|---|---|---|
| AGP | 9.4.0 | 高于正式版，使用 Canary/RC 分支 |
| Kotlin | 2.4.20 | 跟随官方最新 |
| KSP | 2.3.6 | 需与 Kotlin 版本对齐 |
| Room3 | 3.0.3 | `androidx.room3` 尚处 alpha，API 演进频繁 |
| Compose BOM | 2026.09.00 | 跟随月度 BOM |
| Media3 | 1.11.1 | 跟随官方发布 |

## 2. 升级原则

1. **KSP 与 Kotlin 必须联动**：KSP 版本与 Kotlin 编译器版本强绑定，升级时先对齐再发布。
2. **Room3 单独 review**：Room3 处于 alpha，每次升级必须人工 review 迁移 API（如 `suspend fun migrate(connection: SQLiteConnection)` 签名变化），并在 CI 中跑通 `schemas/` 的 migration 校验。
3. **AGP 升 Canary 需谨慎**：新 AGP 可能改变默认行为或引入 breaking change，升级后需全量构建验证。
4. **Compose BOM 月度跟进**：BOM 由 Google 统一管理子版本，仅需升级 BOM 版本，禁止单独 pin 子模块版本。

## 3. 自动化（Renovate）

- 配置文件：`.github/renovate.json`
- 默认策略：`rangeStrategy = bump`，每周一自动开 PR。
- 已分组：`Room3 (alpha)`、`AGP`、`Kotlin & KSP`、`Compose BOM`，便于分组 review 与回滚。

## 4. 升级 checklist

- [ ] `libs.versions.toml` 中版本号与注释同步更新
- [ ] `./gradlew build` 全量构建通过
- [ ] Room3 升级时，核对 `schemas/` 下各版本 json 是否存在、auto-migration 是否有兜底
- [ ] 升级后跑一遍 `:app:compileFreeDebugKotlin` 确认 Hilt KSP 依赖图无误
- [ ] 若涉及 Compose 稳定性，同步更新 `compose_stability_config.conf` 或对应 `@Immutable` 注解
