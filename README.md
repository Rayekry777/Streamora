# Streamora

Streamora 是面向封闭演示 MVP 的视频社区与 AI 宠物项目。用户端提供视频上传、播放、社区互动和跨页面 Live2D 宠物；独立管理端提供内容、用户、媒体、宠物资产和 AI 运营能力。

## 项目导航

- [项目路线图](docs/project/PROJECT_ROADMAP.md)
- [仓库目录规范](docs/project/REPOSITORY_STRUCTURE.md)
- [系统架构](docs/architecture/SYSTEM_ARCHITECTURE.md)
- [服务边界](docs/architecture/SERVICE_BOUNDARIES.md)
- [后端开发契约](docs/development/BACKEND_DEVELOPMENT.md)
- [数据库结构与规划](docs/data/DATABASE_SCHEMA.md)
- [API 契约](docs/contracts/API_CONVENTIONS.md)
- [事件契约](docs/contracts/EVENT_CONTRACTS.md)
- [数据归属](docs/data/DATA_OWNERSHIP.md)
- [阶段 0 验收](docs/acceptance/PHASE_0_ACCEPTANCE.md)

当前处于阶段 0，尚未创建可运行服务。阶段 1 才会初始化两个前端、17 个后端运行单元和本地基础设施配置。

## 顶层目录

| 目录 | 内容 |
|---|---|
| `.agents/skills/` | Streamora 交付、后端和前端开发 Skill |
| `apps/` | 用户端 `web` 与管理端 `admin-web` |
| `services/` | 17 个后端运行单元 |
| `packages/` | OpenAPI/Protobuf 生成代码、前端共享令牌等受控共享包 |
| `platform/` | Docker Compose、网关、可观测性和部署资源 |
| `docs/` | 项目、开发、架构、契约、数据与验收文档 |
