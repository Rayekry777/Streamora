---
name: streamora-backend-development
description: 设计、实现、修复、审查和验证 Streamora 的 Java 21、Spring Boot、Spring Cloud Alibaba、Dubbo、RocketMQ、PostgreSQL/pgvector 微服务。用于处理 17 个后端运行单元、HTTP/OpenAPI、Dubbo/Protobuf、领域事件、数据库迁移、Outbox、幂等、权限、Agent 服务或后端联调任务。
---

# Streamora 后端开发

## 开始前

1. 读取 [本机工具定位](../streamora-delivery-workflow/references/local-tooling.md)，本地 Maven 验证直接使用 `.\mvnw.cmd`。
2. 完整读取 `docs/development/BACKEND_DEVELOPMENT.md` 和 `docs/data/DATABASE_SCHEMA.md`。
3. 读取与任务相关的分类契约：服务边界、API、事件和数据归属。
4. 检查相关源码、配置、迁移、测试和 Git 状态，保留用户改动。
5. 按 [服务边界导航](references/service-boundaries.md) 确认唯一写入方。
6. 涉及新的框架、库、基础设施、中间件、媒体处理或安全方案时，完整读取并执行 [开源方案优先政策](../../../docs/development/OPEN_SOURCE_ADOPTION_POLICY.md)。先从官方资料和源仓库调研成熟方案；无可采纳方案才可自研，并把结论写入阶段验收或设计记录。

## 实现流程

1. 先修改契约或确认现有契约足够，再实现代码。
2. 使用 OpenAPI 定义外部 HTTP；使用 Protobuf 定义内部 Dubbo 接口；禁止共享 Entity。
3. 按 [后端规范](references/backend-standards.md) 实现分层、校验、日志、异常和配置。
4. 数据结构只通过新 Flyway 迁移演进；同步更新 `docs/data/DATABASE_SCHEMA.md`。
5. 跨服务业务流程使用 [接口与事件规则](references/api-event-contracts.md)，默认 Outbox 加幂等消费者，不默认引入分布式事务。
6. 按 [后端验证](references/backend-verification.md) 执行最小充分验证并记录结果。
7. 仅当实现、权限、校验、测试、契约、编译和文档全部一致时标记“已实现”。

## 本地联调

- Windows 本地开发使用 dev Ubuntu VM（`192.168.126.129`）运行 PostgreSQL、Redis、Nacos、RocketMQ 和 MinIO；IDEA 只启动当前调试的 Java 服务。sim Core VM（`192.168.126.128`）仅用于已部署环境、阶段验收和浏览器 E2E。
- 真实 PostgreSQL、Nacos 和 Dubbo 联调使用 `compose,idea` Profile 与 dev VM 地址；未实际接入 Redis、RocketMQ 或对象存储的服务不得把容器可用写成业务联调已通过。
- 容器、跨进程 RPC 或真实媒体验证失败时，记录真实阻塞并保留服务内 Maven 验证，不以 H2 测试替代真实环境结论。

## Agent 特有约束

- 业务层只依赖 `ModelProvider` 和 `AgentRuntime` 项目接口。
- Spring AI Alibaba、Qwen 和 Mock 适配器只能位于基础设施层。
- 写操作先产生 `action.proposed`；确认令牌验证成功后才调用领域服务。
- Agent 不直接修改亲密度、经验、装扮库存或其他宠物权威状态。
- 完整会话存 PostgreSQL，短期状态存 Redis，用户主动开启的长期记忆存 pgvector。
