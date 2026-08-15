# Streamora

部署、自动修复与阶段发布流程见 [docs/自动修复部署.md](docs/自动修复部署.md) 和 [docs/acceptance/PHASE_RELEASE_CHECKLIST.md](docs/acceptance/PHASE_RELEASE_CHECKLIST.md)。

Streamora 是面向封闭演示 MVP 的视频社区与 AI 宠物项目。用户端提供视频上传、播放、社区互动和跨页面 Live2D 宠物；独立管理端提供内容、用户、媒体、宠物资产和 AI 运营能力。

## 项目导航

- [项目路线图](docs/project/PROJECT_ROADMAP.md)
- [仓库目录规范](docs/project/REPOSITORY_STRUCTURE.md)
- [系统架构](docs/architecture/SYSTEM_ARCHITECTURE.md)
- [服务边界](docs/architecture/SERVICE_BOUNDARIES.md)
- [后端开发契约](docs/development/BACKEND_DEVELOPMENT.md)
- [前端开发契约](docs/development/FRONTEND_DEVELOPMENT.md)
- [数据库结构与规划](docs/data/DATABASE_SCHEMA.md)
- [API 契约](docs/contracts/API_CONVENTIONS.md)
- [事件契约](docs/contracts/EVENT_CONTRACTS.md)
- [数据归属](docs/data/DATA_OWNERSHIP.md)
- [阶段 0 验收](docs/acceptance/PHASE_0_ACCEPTANCE.md)
- [阶段 1 验收](docs/acceptance/PHASE_1_ACCEPTANCE.md)
- [后端容器部署](docs/deployment/DOCKER_BACKEND_DEPLOYMENT.md)
- [前端容器部署](docs/deployment/DOCKER_FRONTEND_DEPLOYMENT.md)

当前阶段 1 已完成代码侧出口并处于“待验收”：两个前端、17 个后端运行单元、公共契约与三档本地基础设施配置均已建立。由于本机尚未安装 Docker，容器启动验证仍需安装 Docker Desktop 后执行。

## 顶层目录

| 目录 | 内容 |
|---|---|
| `.agents/skills/` | Streamora 交付、后端和前端开发 Skill |
| `apps/` | 用户端 `web` 与管理端 `admin-web` |
| `services/` | 17 个后端运行单元 |
| `packages/` | OpenAPI/Protobuf 生成代码、前端共享令牌等受控共享包 |
| `platform/` | Docker Compose、网关、可观测性和部署资源 |
| `docs/` | 项目、开发、架构、契约、数据与验收文档 |
