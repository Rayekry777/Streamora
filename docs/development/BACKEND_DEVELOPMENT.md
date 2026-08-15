# Streamora 后端开发与设计契约

> 本文档是后端范围、架构、接口状态、数据演进和验证记录的维护入口。OpenAPI 是 HTTP 契约真源，Protobuf 是内部同步契约真源，Flyway 历史是已执行数据库结构真源。

```yaml
version: 2
updatedAt: 2026-08-16
scope: Streamora 17 个后端运行单元
reviewStatus: pending
implementationStatus: 阶段 2 已完成；阶段 3 进行中，视频读取契约已预置
```

## 1. 当前工程事实

- 阶段 0、1、2 已完成；阶段 3 已进入上传、转码、审核、发布与播放主链路实施。
- 目标基线：Java 21、Spring Boot 3.5.16、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 内部同步：Dubbo 3.3.6 Triple + Protobuf；异步：RocketMQ 5.3.1。
- 服务发现与配置：Nacos 3.0.3；流量保护：Sentinel 1.8.9。
- 数据：PostgreSQL + pgvector、Redis、MinIO；媒体处理：FFmpeg HLS。
- AI：Spring AI Alibaba Agent Framework，Qwen Responses Provider 和 Mock Provider；业务层通过 `ModelProvider`、`AgentRuntime` 隔离框架。
- 可观测性：OpenTelemetry、Prometheus、Grafana、Loki、Tempo。
- 所有新方案先遵循 [开源方案优先政策](OPEN_SOURCE_ADOPTION_POLICY.md) 完成调研、评估与记录；无可采纳方案才允许自研。

## 2. 架构与数据流

- 服务职责以 [服务边界](../architecture/SERVICE_BOUNDARIES.md) 为真源。
- 数据所有权以 [数据归属](../data/DATA_OWNERSHIP.md) 为真源。
- 外部 HTTP 经过 `gateway-service`；管理请求进入 `admin-service` BFF 后调用领域服务。
- 同步内部接口使用 Dubbo/Protobuf；异步领域事件使用 RocketMQ。
- 跨服务一致性默认使用本地事务、Outbox、幂等消费者和补偿，不默认引入 Seata。
- 上传链路：分片预签名直传 MinIO，完成后发事件，FFmpeg 转 HLS/关键帧/字幕，再审核、发布并更新搜索和推荐投影。
- Agent 链路：浏览器 SSE 连接 `agent-service`，运行时组合宠物、会话、记忆和工具；写工具先提案，用户确认后调用领域服务。

## 3. 接口状态

### 已实现

- 用户注册、登录、会话读取与 CSRF 注销：`/api/v1/auth/**`。
- 管理员登录、会话读取、CSRF 注销与 RBAC 概览：`/admin-api/v1/**`。
- 公共/个人活动宠物选择：`/api/v1/pets/active`。
- identity 会话内部契约：Dubbo Triple + Protobuf `IdentitySessionService`。
- identity/admin/pet 三个服务的 Flyway V1 和 H2 PostgreSQL 兼容测试。
- 阶段 3 视频读取 OpenAPI：`/api/v1/home/feed`、`/api/v1/videos/{videoId}`、`/api/v1/videos/{videoId}/playback`；仅完成契约与生成类型，尚未由 feed、video、playback 服务实现。
- 阶段 3 媒体上传 OpenAPI：`POST /api/v1/media/uploads`、`POST /api/v1/media/uploads/{uploadId}/complete`；media-service 已实现本地可测试的上传会话、幂等完成、转码任务和 Outbox，真实 S3 与 Identity RPC 适配待补齐。
- 阶段 3 内部转码契约：Protobuf/Dubbo Triple `MediaTranscodeJobService` 支持领取、完成和失败回报。`media-service` 以租约和条件更新保持唯一写入；`transcode-worker` 通过 Jaffree 编排 FFmpeg 生成 HLS 与封面，当前本地工作区适配器仅用于可测试开发。

### 开发中

- 管理端密码复验、五分钟一次性作用域令牌与刷新令牌轮换将在出现首个高风险写接口时接入；当前没有高风险管理写 API。

### 未实现

| 能力 | 目标边界 | 状态 |
|---|---|---|
| 外部用户 API | 其他领域 `/api/v1` API | 部分实现 |
| 管理 API | 审核、用户治理、媒体、AI 等领域操作 | 部分实现 |
| 内部 RPC | 后续领域 Dubbo Triple + Protobuf | 部分实现 |
| 领域事件 | RocketMQ 事件信封、Outbox、幂等 | 未实现 |
| Agent SSE | 8 类标准事件、断线和降级 | 未实现 |

## 4. 身份、权限与敏感信息

- 用户会话和管理会话使用不同 Cookie 名称、受众、刷新令牌族和客户端。
- 管理 Cookie 为 Secure、HttpOnly、SameSite=Strict，并启用 CSRF 防护。
- 高风险管理操作要求影响摘要、原因、密码复验及五分钟一次性动作作用域令牌。
- 管理审计不可由业务 API 修改或删除；日志和遥测必须脱敏。
- API Key、密码、Cookie、Token 和私密记忆不得进入仓库或普通日志。

## 5. 数据库演进

阶段 2 已为 identity、admin、pet 建立三份 Flyway V1，共 10 张表。Compose 使用独立数据库角色和 schema；服务之间只通过 Protobuf RPC 传递主体与会话结果，不跨 schema 查询。

## 6. 实施阶段

| 阶段 | 后端交付 | 状态 |
|---|---|---|
| 0 | 服务边界、API/事件契约、数据归属 | 已完成 |
| 1 | 17 个运行单元、公共 BOM、配置发现和基础设施 | 已完成 |
| 2 | 用户/管理登录、RBAC、公共宠物切换个人宠物 | 已完成 |
| 3 | 分片上传、转码、审核、HLS 播放与媒体管理 | 进行中 |
| 4–8 | 见 [项目路线图](../project/PROJECT_ROADMAP.md) | 未实现 |

## 7. 验收条件

- 能力只有在实现、权限、校验、测试、OpenAPI/Protobuf、编译和文档一致时标记“已实现”。
- 不使用跨 schema 读写或跨服务数据库外键。
- 管理端不直接修改领域服务数据库。
- Agent 未确认不能执行写工具，长期记忆默认关闭并可查看、编辑、删除。

## 8. 验证记录

| 日期 | 范围 | 命令或证据 | 结果 |
|---|---|---|---|
| 2026-08-15 | 环境与空工作区 | 工作区扫描、Java/Maven/Node/pnpm 版本检查 | 通过；Docker 未安装 |
| 2026-08-15 | 阶段 0 契约 | Skill 官方校验、占位符扫描、17 服务集合检查、本地链接检查 | 通过 |
| 2026-08-15 | 仓库目录分类 | 根目录审计、全部 Markdown 本地链接检查、三套 Skill 复验 | 通过；根目录仅保留 README |
| 2026-08-15 | 17 个运行单元 | Maven Reactor 测试、随机端口 HTTP liveness、可执行 JAR 打包 | 17/17 通过 |
| 2026-08-15 | 依赖对齐 | 移除会覆盖 Spring 6.2 的 Dubbo 全量 BOM，仅管理 Starter 版本 | Spring Boot 3.5.16 + Dubbo 3.3.6 上下文通过 |
| 2026-08-15 | 基础设施配置 | 5 个 YAML 解析、30 个 Compose 服务、17 个后端单元及 3 个 Profile 检查 | 静态校验通过；Docker 运行待安装后验证 |
| 2026-08-15 | identity-service | 用户注册/会话/注销、CSRF、用户与管理员受众隔离 | 4 个测试通过 |
| 2026-08-15 | admin-service | Strict Cookie、RBAC、权限拒绝、用户 Cookie 隔离、审计 | 4 个测试通过 |
| 2026-08-15 | pet-service | 匿名公共宠物、管理员 Cookie 忽略、个人宠物复用 | 4 个测试通过 |
| 2026-08-15 | 契约 | Redocly Lint、OpenAPI 类型生成、Dubbo Protobuf 代码生成 | 通过；保留 1 个阶段 1 游标组件未使用警告 |
| 2026-08-15 | 封闭演示管理员登录 | `admin / 123456` 的管理端请求校验与 admin-service 集成测试 | 通过；生产环境必须改用强密码 |
| 2026-08-16 | 阶段 3 开源方案与上传契约 | 官方资料、源仓库与许可证调研；OpenAPI lint 与 TypeScript 生成 | AWS SDK for Java 2.x + SeaweedFS + FFmpeg 进入实现基线；上传契约校验通过，保留既有未使用组件警告 |
| 2026-08-16 | media-service 上传主链路 | H2 PostgreSQL 兼容模式下的 Flyway、幂等会话、完成、转码任务和 Outbox 集成测试 | 通过；真实 S3、Identity RPC、RocketMQ 与 FFmpeg 集成待 Docker 环境补充 |
| 2026-08-16 | 转码任务租约与 Worker | Protobuf Triple 生成，media-service V1-V2 Flyway、任务领取、完成回报、`media.asset.transcoded.v1` Outbox 与 Worker 编排测试 | 通过；本机未安装 FFmpeg 和 Docker，尚未执行真实媒体转码或跨进程 Dubbo |

## 9. 已确定风险

- Docker Desktop 尚未安装，阶段 1 可生成配置和完成本机构建，但容器运行验收需要先安装 Docker。
- 正式 Live2D 模型未提供，只能先验证渲染接口与占位降级。
- 微服务数量对 MVP 偏多，阶段 1 必须提供 infra/core/full 配置以控制本地资源。
- Qwen 与 Spring AI Alibaba 适配器必须被项目接口隔离，避免供应商和框架升级扩散到业务层。
