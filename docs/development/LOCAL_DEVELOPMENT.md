# Streamora 本地 IDEA 开发

## 运行边界

Windows IDEA 只启动当前正在调试的 Java 微服务。Docker 不运行 Java 服务；dev VM 只运行 PostgreSQL、Redis、Nacos、RocketMQ 和 MinIO。sim VM 是独立的已部署 Core 环境，用于阶段验收和浏览器 E2E。

服务器说明：

- [Dev 基础设施服务器](../operations/DEV_INFRA_SERVER.md)
- [Sim Core 服务器](../operations/SIM_SERVER.md)

## 开发依赖

dev VM 固定为 `192.168.126.129`，Compose 项目为 `streamora-dev`。在 Windows 仓库根目录执行：

```powershell
.\scripts\ops\Start-StreamoraInfra.ps1
.\scripts\ops\Test-StreamoraInfra.ps1
```

停止时执行 `Stop-StreamoraInfra.ps1`；脚本只停止容器，不删除命名卷。

## IDEA 配置

IDEA 服务使用 `compose,idea` Profile：

```text
SPRING_PROFILES_ACTIVE=compose,idea
NACOS_SERVER_ADDR=192.168.126.129:8848
STREAMORA_DEV_INFRA_HOST=192.168.126.129
SPRING_DATASOURCE_PASSWORD=<私有 dev 服务密码>
STREAMORA_SECURE_COOKIE=false
```

`identity-service`、`admin-service`、`pet-service`、`media-service`、`video-service`、`playback-service` 各自使用 `application-idea.yml`。其余服务只需启用 `compose` 和 Nacos 地址。服务数据源的 schema 不能互换。

Dubbo 3.3.6 在当前 Windows 环境不接受 `127.0.0.1` 作为 `DUBBO_IP_TO_BIND`。默认留空，让
Dubbo 选择当前有效 Windows IPv4；需要固定地址时，将 `DUBBO_IP_TO_BIND` 和
`DUBBO_IP_TO_REGISTRY` 设为该 IPv4。无论哪种方式，`NACOS_SERVER_ADDR` 都只能指向 dev
`.129`，因此本地服务不会注册到 sim。

## 验证层次

1. Maven 单测和集成测试：不要求 Docker，默认使用测试配置。
2. IDEA 联调：真实 PostgreSQL、Nacos 和 Dubbo 连接 dev VM；只有实际接入 Redis、RocketMQ 或 MinIO 的服务才记录对应联调通过。
3. PR 功能验证：GitHub Actions 在独立临时 Compose 环境执行。
4. 阶段验收与部署 E2E：在 sim VM 执行，通过 SSH 隧道从 Windows 查看。

IDEA 断点调试不能代替 Maven、GitHub 功能验证或部署后浏览器 E2E。
