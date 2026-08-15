# 后端规范

- 基线：Java 21、Spring Boot 3.5、Spring Cloud 2025、Spring Cloud Alibaba 2025。
- 外部 API 前缀 `/api/v1`，管理 API 前缀 `/admin-api/v1`；时间使用 UTC，BIGINT 在 JSON 中序列化为字符串。
- DTO、查询条件和无状态 VO 优先使用 `record`；Entity 使用普通类；禁止 Entity 跨服务或作为公开契约。
- Controller 负责协议、校验和权限入口；Application Service 编排用例；Domain 承载规则；Repository/Adapter 处理外部依赖。
- 使用统一错误信封、稳定错误码和游标分页；Controller 必须维护 OpenAPI operationId、响应和安全声明。
- 使用 SLF4J 占位符日志；禁止输出密码、Cookie、Token、API Key、完整隐私字段和大请求体。
- 生产凭据只从环境变量或密钥系统读取；仓库仅提交示例值。
- 每个有状态服务拥有独立 PostgreSQL 用户和 schema；禁止跨 schema 外键、查询和写入。
- 同步内部调用使用 Dubbo Triple/Protobuf；异步调用使用 RocketMQ。
- 超时、重试、熔断必须按幂等性配置；禁止对非幂等写操作进行无界自动重试。
- 所有消费端以 `eventId` 做幂等；生产端在本地事务内写业务数据和 Outbox。

