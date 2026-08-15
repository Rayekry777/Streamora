# Streamora 仓库目录规范

## 顶层结构

```text
Streamora/
├─ .agents/skills/       项目专用工作流与规范
├─ apps/                 用户端与管理端
├─ services/             17 个后端运行单元
├─ packages/             受控共享契约与生成代码
├─ platform/             本地基础设施、部署与可观测性
├─ docs/                 分类项目文档
└─ README.md             项目总入口
```

## 目录职责

### `apps/`

- `apps/web/`：用户端、创作者端、播放器和全局宠物。
- `apps/admin-web/`：独立运营管理端，不加载宠物运行时。

### `services/`

每个运行单元一个目录，名称与 `SERVICE_BOUNDARIES.md` 完全一致。服务目录只包含自身源码、配置、测试和 Flyway 迁移；不得包含其他服务的 Entity 或数据库访问代码。

### `packages/`

- `packages/proto/`：Dubbo Triple 的 Protobuf 真源及生成配置。
- `packages/openapi/`：聚合或导出的 OpenAPI 契约与生成客户端配置。
- `packages/ui-tokens/`：两个前端可共享的设计令牌，不共享页面状态。

共享包不得放置数据库 Entity、Repository、领域 Service、用户会话 Store 或管理会话 Store。

### `platform/`

- `platform/compose/`：`infra`、`core`、`full` Compose 配置。
- `platform/observability/`：Prometheus、Grafana、Loki、Tempo 配置。
- `platform/gateway/`：仅与边缘代理、证书和静态路由有关的部署资源。
- `platform/scripts/`：可重复的本地启动、检查和运维脚本。

### `docs/`

| 子目录 | 内容 |
|---|---|
| `project/` | 路线图、仓库结构和项目级决策 |
| `development/` | 开发状态、验证记录和工程契约 |
| `architecture/` | 系统架构和服务边界 |
| `contracts/` | HTTP、RPC、SSE 和事件约定 |
| `data/` | 数据库结构与数据归属 |
| `acceptance/` | 各阶段验收证据 |

## 命名规则

- 顶层工程目录使用小写 kebab-case。
- Java 服务目录使用计划中的 `<domain>-service` 名称；worker 保留 `transcode-worker`。
- 文档使用大写 snake case，便于与源码目录区分。
- 新文件必须放入负责它的最窄目录；根目录只保留 `README.md` 和必要的仓库级工具配置。

