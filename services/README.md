# 后端服务目录

阶段 1 在此创建 17 个运行单元。目录名称、职责和数据所有权必须与 [服务边界](../docs/architecture/SERVICE_BOUNDARIES.md) 一致。

服务之间共享 Protobuf/OpenAPI 契约，不共享 Entity、Repository、Flyway 迁移或数据库账号。

