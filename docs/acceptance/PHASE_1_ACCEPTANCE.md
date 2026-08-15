# 阶段 1 验收

- 状态：待验收
- 更新日期：2026-08-15
- 目标：建立两个前端、17 个后端运行单元、公共契约及本地基础设施骨架。

## 交付物

- 已生成：Maven 聚合工程、Wrapper、公共依赖管理与 17 个 Spring Boot 运行单元。
- 已生成：`web` 和 `admin-web` 两个独立 Vue 应用；用户端包含路由外唯一宠物宿主，管理端无宠物。
- 已生成：OpenAPI 3.1、Protobuf 公共信封、健康 RPC 与 TypeScript 类型生成链路。
- 已生成：单一 Compose 入口的 infra/core/full Profile、数据库初始化及 Prometheus/Grafana/Loki/Tempo 配置。
- 已生成：后端通用 Java 21 镜像、两个前端 Nginx 镜像和中文部署说明。

## 验证证据

| 检查项 | 实际执行 | 结果 |
|---|---|---|
| Git 基线 | 检查分支、工作区和首次提交 | `master`，提交 `2b63cec`，开始阶段时干净 |
| 本机工具链 | Java、Maven、Node、pnpm、Docker 版本检查 | 前四项满足；Docker 未安装 |
| 后端测试 | `.\mvnw.cmd -B -ntp test` | 20 个 Reactor 模块成功，17/17 服务随机端口 HTTP liveness 通过 |
| 后端打包 | `.\mvnw.cmd -B -ntp -DskipTests package` | 17/17 确定命名可执行 JAR 生成 |
| 前端类型 | `pnpm typecheck` | 两个应用通过 |
| 前端规范 | `pnpm lint` | 两个应用 0 error、0 warning |
| 前端测试 | `pnpm test` | 2 个测试文件、2 个测试通过 |
| 前端构建 | `pnpm build` | 两个 Vite 生产构建通过 |
| HTTP 契约 | Redocly lint 与 openapi-typescript | OpenAPI 有效并成功生成类型；空路径阶段有 2 个未引用组件警告 |
| 配置静态校验 | Python YAML 解析与集合检查 | 5 个 YAML 有效；Compose 30 个服务、17 个后端、infra/core/full 三个 Profile |
| 文档完整性 | 16 个 Markdown 本地链接检查 | 0 个断链 |
| 敏感信息 | 工程文本签名与真实 `.env` 文件扫描 | 0 个凭据特征、0 个真实环境文件 |

## 出口条件

| 条件 | 状态 | 证据 |
|---|---|---|
| 17 个运行单元均可编译并通过健康检查 | 已达到 | 17/17 liveness HTTP 测试和 JAR 打包通过 |
| 两个前端均可安装、检查、测试和构建 | 已达到 | 类型、Lint、Vitest、Vite 全部通过 |
| 公共契约可生成或解析 | 已达到 | Protobuf 编译 12 个 Java 源；OpenAPI 类型生成成功 |
| Compose 和可观测性配置静态校验通过 | 已达到 | 5 个 YAML 解析成功，服务与 Profile 数量匹配 |

## 未验证项与风险

- Docker 未安装，不能执行镜像构建、Compose 启动和容器内健康探测。
- Sentinel Dashboard 使用可替换的第三方镜像默认值；安装 Docker 后需验证镜像可拉取性和健康策略，生产环境应评审可信来源。
- 正式 Live2D 模型未提供，本阶段只建立前端宿主和静态占位边界。

## 用户验收

- 结论：待确认
- 备注：代码侧出口已达到；请用户决定是否以“Docker 运行验证后补”为条件通过阶段 1。未收到明确验收前不进入阶段 2。
