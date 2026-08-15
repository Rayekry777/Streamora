# Streamora 后端开发与设计契约

> 本文档是后端范围、架构、接口状态、数据演进和验证记录的维护入口。OpenAPI 是 HTTP 契约真源，Protobuf 是内部同步契约真源，Flyway 历史是已执行数据库结构真源。

```yaml
version: 1
updatedAt: 2026-08-15
scope: Streamora 17 个后端运行单元
reviewStatus: pending
implementationStatus: 未实现
```

## 1. 当前工程事实

- 当前工作区只有阶段 0 设计与 Skill，没有 Maven 模块、源码、迁移或可运行服务。
- 目标基线：Java 21、Spring Boot 3.5.16、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 内部同步：Dubbo 3.3.6 Triple + Protobuf；异步：RocketMQ 5.3.1。
- 服务发现与配置：Nacos 3.0.3；流量保护：Sentinel 1.8.9。
- 数据：PostgreSQL + pgvector、Redis、MinIO；媒体处理：FFmpeg HLS。
- AI：Spring AI Alibaba Agent Framework，Qwen Responses Provider 和 Mock Provider；业务层通过 `ModelProvider`、`AgentRuntime` 隔离框架。
- 可观测性：OpenTelemetry、Prometheus、Grafana、Loki、Tempo。

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

无。

### 开发中

无。阶段 0 只完成设计。

### 未实现

| 能力 | 目标边界 | 状态 |
|---|---|---|
| 外部用户 API | `/api/v1`，统一响应、错误和游标分页 | 未实现 |
| 管理 API | `/admin-api/v1`，RBAC、复验和审计 | 未实现 |
| 内部 RPC | Dubbo Triple + Protobuf，不共享 Entity | 未实现 |
| 领域事件 | RocketMQ 事件信封、Outbox、幂等 | 未实现 |
| Agent SSE | 8 类标准事件、断线和降级 | 未实现 |

## 4. 身份、权限与敏感信息

- 用户会话和管理会话使用不同 Cookie 名称、受众、刷新令牌族和客户端。
- 管理 Cookie 为 Secure、HttpOnly、SameSite=Strict，并启用 CSRF 防护。
- 高风险管理操作要求影响摘要、原因、密码复验及五分钟一次性动作作用域令牌。
- 管理审计不可由业务 API 修改或删除；日志和遥测必须脱敏。
- API Key、密码、Cookie、Token 和私密记忆不得进入仓库或普通日志。

## 5. 数据库演进

当前没有迁移。阶段 1 为各有状态服务建立独立 schema、用户和 Flyway 基线；已执行迁移只增不改。

## 6. 实施阶段

| 阶段 | 后端交付 | 状态 |
|---|---|---|
| 0 | 服务边界、API/事件契约、数据归属 | 待验收 |
| 1 | 17 个运行单元、公共 BOM、配置发现和基础设施 | 未实现 |
| 2–8 | 见 [项目路线图](../project/PROJECT_ROADMAP.md) | 未实现 |

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

## 9. 已确定风险

- Docker Desktop 尚未安装，阶段 1 可生成配置和完成本机构建，但容器运行验收需要先安装 Docker。
- 正式 Live2D 模型未提供，只能先验证渲染接口与占位降级。
- 微服务数量对 MVP 偏多，阶段 1 必须提供 infra/core/full 配置以控制本地资源。
- Qwen 与 Spring AI Alibaba 适配器必须被项目接口隔离，避免供应商和框架升级扩散到业务层。
