# Streamora 后端容器部署说明

## 1. 已检测工程事实

- Java 21、Spring Boot 3.5.16，使用仓库 Maven Wrapper 3.9.11。
- Maven Reactor 包含公共 Protobuf 模块和 17 个可运行 Spring Boot 单元。
- 端口固定在 8080–8096；所有服务提供 Actuator liveness、readiness 和 Prometheus 端点。
- 本地默认关闭 Nacos，`compose` Profile 连接 `nacos:8848` 并使用 Dubbo Triple。
- 当前没有业务表或 Flyway 迁移；PostgreSQL 初始化脚本只创建 pgvector 扩展、15 个有状态服务的独立角色与 schema。
- 当前机器未安装 Docker，因此本轮已完成 Maven 运行测试和 Compose/YAML 静态校验，尚未执行镜像与容器运行验证。

## 2. 文件映射

| 文件 | 用途 |
|---|---|
| `platform/docker/backend.Dockerfile` | 通过 `SERVICE` 参数构建任意后端单元的 Java 21 非 root 镜像 |
| `.dockerignore` | 排除构建产物、本地配置、密钥、日志、媒体缓存和 Git 数据 |
| `platform/compose/compose.yml` | 唯一 Compose 入口，提供 `infra`、`core`、`full` 三档 Profile |
| `platform/compose/.env.example` | 本地端口、镜像标签、密码和内存限制模板 |
| `platform/compose/postgres/init/001-init-streamora.sh` | pgvector 与独立数据库角色/schema 初始化 |
| `platform/observability/` | Prometheus、Grafana、Loki、Tempo 配置 |

## 3. 本地构建与测试

在仓库根目录执行：

```powershell
.\mvnw.cmd -B -ntp test
.\mvnw.cmd -B -ntp -DskipTests package
```

构建单个镜像，以身份服务为例：

```powershell
docker build --file platform/docker/backend.Dockerfile `
  --build-arg SERVICE=identity-service `
  --tag streamora/identity-service:dev .
```

Dockerfile 确定性复制 `services/<service>/target/<service>.jar`，不会用通配符选择产物。

## 4. Compose 三档环境

先安装 Docker Desktop，并建议将 Docker 数据目录放到 D 盘。然后：

```powershell
Set-Location platform/compose
Copy-Item .env.example .env
```

编辑 `.env`，替换全部 `replace-with-...` 值。三档启动命令：

```powershell
docker compose --profile infra up -d
docker compose --profile core up -d --build
docker compose --profile full up -d --build
```

- `infra`：PostgreSQL/pgvector、Redis、MinIO、Nacos、RocketMQ、Sentinel 和可观测性组件。
- `core`：在 infra 上增加两个前端与 10 个 MVP 核心后端单元。
- `full`：启动全部 17 个后端、两个前端和基础设施。

不要同时启动三条命令；选择一档即可。查看状态和日志：

```powershell
docker compose --profile core ps
docker compose --profile core logs --tail 200 gateway-service
```

## 5. 运行配置与秘密信息

- 密码只写入未跟踪的 `platform/compose/.env` 或生产秘密管理系统。
- 服务间使用 Compose DNS 名称，例如 `nacos:8848`，不得使用 `localhost`。
- PostgreSQL 和 Redis 只在内部网络暴露，不发布宿主机端口。
- Nacos、MinIO、Prometheus 和 Grafana 仅绑定 `127.0.0.1`，不能直接用于公网部署。
- Nacos 在封闭本地演示中关闭鉴权；生产部署必须开启鉴权、配置高强度身份参数，并置于内部网络。

## 6. 健康验证

容器运行后可在容器内部检查：

```powershell
docker compose exec gateway-service wget -qO- http://127.0.0.1:8080/actuator/health/liveness
docker compose exec identity-service wget -qO- http://127.0.0.1:8082/actuator/health/readiness
```

用户入口默认是 `http://127.0.0.1:3000`，管理入口默认是 `http://127.0.0.1:3001`。

## 7. JVM、停止与资源

- 镜像默认使用 `MaxRAMPercentage=75`，这是 JVM 堆指导值，不代表进程总内存。
- Compose 默认给每个 Java 单元 768MB，可通过 `BACKEND_MEMORY_LIMIT` 调整。
- Spring 使用优雅关闭，Compose 给出 30 秒停止宽限期。
- full 环境建议 20GB 内存、10–12 核 CPU 和至少 150GB 可用磁盘。

## 8. 数据迁移、备份与回滚

- 业务表必须由各服务自己的 Flyway 历史创建，禁止跨 schema 修改。
- 发布前先备份 PostgreSQL、MinIO 和 RocketMQ 持久卷，并单独评审不可逆迁移。
- 生产镜像使用 Git SHA 或发布版本等不可变标签，不使用 `latest`。
- 回滚时修改 `STREAMORA_IMAGE_TAG` 并重新部署。回滚镜像不会回滚数据库结构或数据。

## 9. 发布检查

- Maven 测试和打包通过，17 个确定命名 JAR 均存在。
- `docker compose config`、镜像构建和容器健康检查通过。
- `.env`、Key、Token、Cookie 与用户记忆未进入镜像和日志。
- 网关、数据库和 Redis 未向公网直接发布。
- Prometheus 能抓取服务指标，Grafana 数据源可访问。

## 10. 常见问题

- 镜像构建找不到 JAR：检查 `SERVICE` 是否与 `services/` 目录和 `<finalName>` 完全一致。
- 服务不健康：先看服务日志，再检查 Nacos DNS、Profile 和 Actuator 端口。
- 容器内连接失败：连接地址必须使用 Compose 服务名，不能使用宿主机的 `127.0.0.1`。
- 内存溢出：同时检查容器上限、非堆内存和线程数，不能只提高 Java 堆百分比。
- 持久卷权限错误：确认 Docker Desktop 共享目录和卷权限，不要以 root 身份运行应用规避问题。

