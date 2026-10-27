# ADR-0001：共享协议模型单层落点（不拆 DTO / 领域模型 / UI 模型）

- **状态**：已接受（Accepted）
- **归档日期**：2026-10-26
- **适用范围**：`core:model` 及其所有消费方（domain / data-contract / data-repository / network / ui / feature）
- **决策落点**：`docs/layering.md` §一、`dependencies-policy.md` §5、`ArchitectureTest` 规则 29

## 1. 决策

1. `core:model` 是**共享协议模型**（服务端响应体形状）的唯一落点。`Article` / `ArticleData` / `Banner` / `User` 等类直接作为协议模型使用，不额外引入 `*Dto` 层。
2. 不为同一份服务端响应在 `core:domain` 复制一份同构「领域模型」，也不在 `core:data-repository` 建立 DTO→领域模型的逐字段 mapper。
3. 唯一允许的第二层模型是 **UI 模型**：`core:ui` 的 `ArticleFeedMapper` 把 `Article` 收敛为 `FeedCardUIState`，Composable 只消费 UI 模型。

> 一句话：**一层协议模型 + 一层 UI 模型**，不做三套模型与两段映射。

## 2. 约束依据（不是偏好，是硬约束）

| 约束 | 出处 | 后果 |
|---|---|---|
| `:core:domain` 只允许依赖 `:core:model` | `FragmjectAndroidDependencyGuardPlugin.CORE_ALLOWED_DEPENDENCIES` | DTO 移出 `model` 后，`domain` 无法引用，7 个 Repository 端口签名 `Flow<PagingData<Article>>` 立刻断裂 |
| `:core:data-contract` 只允许依赖 `:core:model` | 同上 | DTO 放 data-contract 也无法被 domain 看到 |
| `feature:*:impl` 的 core 白名单 9 项，不含 `data-contract` / `network` | `FEATURE_ALLOWED_CORE_DEPENDENCIES` | DTO 即便下沉，feature 侧也不可见，仍需经 model 转手 |
| `core:domain` → 数据模块禁止 | Gradle R1 + `ArchitectureTest` 规则 1 | 让 domain 直接依赖 data-contract 的写法会被双重拦截 |

**推论**：任何「DTO 移出 `core:model`」的方案，实际形态必然是「在 `core:domain` 再造一个 `Article`」——即两份 36 字段的同构类 + 一段逐字段 mapper。这正是 `dependencies-policy.md` §5 明确否决的「机械三套映射」。

## 3. 被否决的方案

### B1（否决）：`Article`/`ArticleData` 下沉为 `core:network`（或 `data-contract`）DTO + mapper

- 形态：`ArticleDto` 承载 wire 形状，`core:model` 保留领域 `Article`，`data-repository` 加 36 字段 mapper。
- 否决理由：
  1. **架构上不自洽**：`domain` 只能依赖 `model`，领域端口无法表达 DTO；要么违规依赖 data-contract，要么在 domain 复制模型。
  2. **改动面与收益失衡**：`Article` 命中 63 个文件 / 163 处引用（domain 10、data-repository 19、feature 24、ui 5、data-contract 1、network 1 等），而新增的隔离在 UI 侧早已由 `FeedCardUIState` 提供。
  3. **静默数据丢失风险**：36 字段手抄 mapper，服务端新增字段时只会「读到默认值」，编译器与测试都不报错。
  4. **守卫成本**：`compose_stability_config.conf` 以 `core.model.Article` / `ArticleData` 登记稳定性，类一移动，`FragmjectAndroidStabilityCheckPlugin` 的幽灵类检查即失败，需连带重排。

### B2（否决）：DTO 放 `network`，`domain` 内自建 `Article`

- 否决理由：与 B1 相同成本，且额外产生两个同名类（`core:model.Article` 与 `core:domain.Article`），import 极易错选，评审与排障成本长期存在。

### B3（不否决，但不建议）：仅把分页信封 `ArticleData` 下沉到 `core:data-contract`

- 形态：`ArticleData` 移入 data-contract，`Article` 留在 `core:model`。
- 评价：与 `docs/layering.md` 表格「data-contract 承载 HTTP 响应模型」一致，36 处 / 6 文件，domain 与 feature 零引用，**技术上可行**。
- 不列为当前动作的理由：收益仅为「`core:model` 少一个纯 wire 信封类」，不改变任何能力边界，也不解决本文 §4 的真实问题；如要做，属独立小 PR，不需要重开架构讨论。

## 4. 实测数据（成本与收益的量化基线）

统计口径：`--include="*.kt"`，排除 `build/`，整词匹配。

**快照日期：2026-10-26。** 数字会随代码演进；结论只依赖量级（100+ 引用、60+ 文件、13 个死字段），不依赖精确值——量级若发生数量级变化，按 §5 重新评估。

| 指标 | 数值 |
|---|---|
| `Article` 引用 | 163 行 / 63 文件 |
| ↳ 分布 | domain 10、data-repository 19、feature 24、ui 5、contract 1、network 1、model 3 |
| `ArticleData` 引用 | 36 行 / 6 文件（**domain / feature 零引用**，仅限适配器层） |
| `Article` 字段总数 | 36 |
| 生产代码零引用字段 | 13：`apkLink` `audit` `canEdit` `descMd` `niceShareDate` `projectLink` `publishTime` `realSuperChapterId` `selfVisible` `shareDate` `superChapterId` `tags` `viewType` |
| UI 侧隔离 | `FeedCard` 只消费 `FeedCardUIState`，wire 字段不进 Composable 状态 |

**结论**：「协议字段污染领域」的真实成因是 `Article` 里有 13 个死字段，而不是缺少 DTO 层。对症处理是瘦身，不是加层。

## 5. 重启讨论的触发条件（缺一不可）

只有在**满足以下任一条**时，才重新开启本议题；不满足时，评审直接引用本 ADR 关闭：

1. **出现第二个协议源**：同一领域模型需要同时承载多种 wire 形态（GraphQL / 本地 mock 协议 / 多后端），单模型无法表达。
2. **字段语义冲突**：服务端字段语义变更，导致领域语义与 wire 语义无法共存（如 `id` 类型变化、必填性变化、同一字段两义）。
3. **物理隔离需求**：`core:model` 需要独立发布或对外复用（`dependencies-policy.md` §6.2 条件③）。
4. **协议模型膨胀到无法维护**：`core:model` 中单个模型类的**零引用字段占比 ≥ 50%**，且已尝试字段瘦身 / UI 投影仍无法缓解。

> 条件 4 的定标说明：不能用「字段总数 ÷ 领域使用字段数」做判据——那样当前 `Article`（36 字段 vs 约 15 个参与 UI 映射）已达 2.4 倍，会与 §3 的否决结论自相矛盾。零引用字段占比是**死重**的直接度量，当前为 13/36 ≈ 36%，未触发。

## 6. 提案门槛（举证责任转移）

本议题此前反复出现的根因是「提出成本为零」。自本 ADR 起：

- 提案人必须在本文件 §3 追加一行「被否决/新增方案」，并**针对 §5 的某一条触发条件给出证据**（具体接口、字段、需求编号）。
- 未提供触发条件证据的提案，review 中引用本 ADR 即可关闭，不需要重新论证。
- 提案一旦被接受，本文件状态改为 `Superseded by ADR-XXXX`，并保留原文（历史不可改写）。

**评审可直接复用的一句话**：
> 该问题已定案为 ADR-0001：`core:model` 为共享协议模型唯一落点，`domain` 只能依赖 `model` 使 DTO 拆分必然产生双份同构模型（163 处引用 / 63 文件）。按 §5，需提供触发条件证据才重开讨论。

## 7. 执行方式（防止决议漂移）

| 手段 | 位置 | 作用 |
|---|---|---|
| 代码锚点 | `core:model/.../Article.kt` 类 KDoc | 读代码的人第一眼看到决策与 ADR 链接 |
| 可执行守卫 | `ArchitectureTest` 规则 29「no dto variants of shared protocol models」 | 新增 `*Dto`/`*DTO` 类直接 fail，把「重新提案」变成一次显式的守卫变更 |
| 评审入口 | `.github/pull_request_template.md` 自检项 | 模型分层变更在 PR 提交时就被要求对照 §5 |
| 单一论述源 | `dependencies-policy.md` §5、`docs/layering.md` §一 | 只保留指向本 ADR 的引用，不再各自复述，避免两处口径漂移 |

## 8. 后果（主动接受的成本）

本决策不是零成本，以下代价是有意接受的，评审时不必再作为新问题提出：

| 代价 | 表现 | 缓解方式 |
|---|---|---|
| 协议膨胀通道 | `core:model` 里可有 13 个零引用字段（`apkLink` 等），服务端加字段会直接进领域模型 | 字段瘦身是独立小 PR（本 ADR 不覆盖）；§5 条件 4 为膨胀设了 50% 死字段红线 |
| 协议变更波及 UI | 服务端字段改名会直接影响 `ArticleFeedMapper` | 该文件是唯一翻译点，改动集中；补 json fixture 契约测试优于加 DTO 层 |
| 稳定性配置需人工同步 | 新增 model 类若未登记进 `compose_stability_config.conf`，稳定性/强跳过策略会静默退化 | 现有插件只做「配置→源码」幽灵类检查，反向检查缺失——**已登记为待办**，不在本 ADR 范围 |
| 语义双职 | 同一类同时承担 wire 形状与领域语义 | `Article` / `ArticleData` 的 KDoc 标注 wire 来源；出现语义冲突即命中 §5 条件 2 |
