# Streamora Dev 基础设施服务器

## 服务器身份

| 项目 | 值 |
| --- | --- |
| 地址 | `192.168.126.129` |
| 主机名 | `streamora-dev` |
| SSH 用户 | `ub001` |
| Docker | 29.5.2 |
| Docker Compose | v5.1.4 |
| Compose 项目 | `streamora-dev` |
| 代码目录 | `/home/ub001/streamora` |

这台 VM 只运行开发依赖，不运行 Streamora Java 微服务、前端或 GitHub Runner。Windows IDEA 运行当前正在调试的服务，并通过 NAT 地址访问这里的中间件。

## 固定中间件版本

| 服务 | 镜像 |
| --- | --- |
| PostgreSQL + pgvector | `pgvector/pgvector:0.8.1-pg16` |
| Redis | `redis:7.4.5-alpine` |
| Nacos | `nacos/nacos-server:v3.0.3` |
| RocketMQ | `apache/rocketmq:5.3.1` |
| MinIO | `minio/minio:RELEASE.2025-06-13T11-33-47Z` |

## 端口

端口只绑定 `192.168.126.129`：

| 服务 | 端口 |
| --- | --- |
| PostgreSQL | `5432` |
| Redis | `6379` |
| Nacos HTTP/gRPC | `8848/9848/9849` |
| RocketMQ NameServer | `9876` |
| RocketMQ Broker | `10909/10911/10912` |
| MinIO API/Console | `9000/9001` |

RocketMQ Broker 必须使用 `platform/compose/rocketmq/broker-dev-vm.conf`，其 `brokerIP1` 固定为 `192.168.126.129`，否则 Windows 客户端会收到不可访问的回环地址。

## 启动与检查

在 Windows 仓库根目录执行：

```powershell
.\scripts\ops\Start-StreamoraInfra.ps1
.\scripts\ops\Test-StreamoraInfra.ps1
.\scripts\ops\Get-StreamoraEnvironmentStatus.ps1
```

脚本通过 `C:\Users\<用户名>\.ssh\streamora_vm` 连接 VM，不保存密码。手工在 VM 内执行时，Compose 文件为 `compose.yml` + `compose.dev-vm.yml`，启动项目名必须为 `streamora-dev`，只指定 PostgreSQL、Redis、Nacos、RocketMQ、MinIO 及初始化容器。

查看 dev 容器：

```bash
docker ps -a --filter label=com.docker.compose.project=streamora-dev
```

停止使用 `stop`，不使用 `down -v`，不删除 PostgreSQL、Redis、MinIO 或 RocketMQ 数据卷。

## 验证记录

| 日期 | 实际执行 | 结果 |
| --- | --- | --- |
| 2026-08-17 | `Start-StreamoraInfra.ps1`、`Test-StreamoraInfra.ps1` | PostgreSQL、Redis、Nacos、RocketMQ、MinIO 均健康；Windows 到全部规定端口和 Nacos readiness 均可达。 |
| 2026-08-17 | Windows 以 `compose,idea` 启动 `identity-service` | 真实 PostgreSQL 的 Flyway 已完成，`/actuator/health` 为 `UP`，Dubbo Provider 已注册到 dev Nacos；验证进程随后停止。 |

## IDEA 环境

本地服务使用 `SPRING_PROFILES_ACTIVE=compose,idea`：

```text
NACOS_SERVER_ADDR=192.168.126.129:8848
STREAMORA_DEV_INFRA_HOST=192.168.126.129
SPRING_DATASOURCE_PASSWORD=<dev .env 中的 STREAMORA_SERVICE_DB_PASSWORD>
STREAMORA_SECURE_COOKIE=false
```

六个有数据库的服务使用各自的 `application-idea.yml` schema。单测不启用 `idea` Profile，继续使用默认 H2 配置。
Dubbo 的绑定与注册地址默认留空并由 Windows 自动选择有效 IPv4；不要设置为 `127.0.0.1`，
当前 Dubbo 版本会拒绝该绑定地址。若需要固定，两个变量均使用当前 Windows IPv4，Nacos 仍只使用 dev `.129`。

## 故障排查

- Nacos 不可用：检查 `8848`、`9848`、`9849` 三个端口和 readiness。
- Windows RocketMQ 客户端连接失败：检查 Broker 日志中宣告的 IP 是否为 `.129`。
- PostgreSQL 权限失败：先确认 `postgres-privileges-init` 已成功完成，再重启对应 IDEA 服务。
- 磁盘空间不足：不要删除数据卷；先查看 `docker system df` 和 VM 根分区使用率。
