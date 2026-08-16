# 平台与部署目录

本目录维护：

- `compose/compose.yml`：通过 `infra`、`core`、`full` Profile 提供基础编排入口。
- `compose/compose.dev-vm.yml`：仅用于 dev VM，向 `192.168.126.129` 公开开发中间件端口。
- `docker/`：17 个后端运行单元共享的参数化镜像构建文件。
- `observability/`：Prometheus、Grafana、Loki、Tempo。
- `gateway/`：边缘代理与部署路由资源。
- `scripts/`：可重复的环境检查、启动和运维脚本。

应用源码不放入本目录，第三方密钥和生产凭据不得提交。

阶段和部署环境使用 `compose/.env.example` 的同结构私有 `.env`。dev VM 使用私有
`compose/.env.dev`，由 Windows 脚本通过 SSH 管理：

```powershell
.\scripts\ops\Start-StreamoraInfra.ps1
.\scripts\ops\Test-StreamoraInfra.ps1
```

这只启动 PostgreSQL、Redis、Nacos、RocketMQ 和 MinIO。Java 服务由 IDEA 按需启动；停止时运行
`Stop-StreamoraInfra.ps1`，该脚本不会删除命名卷。服务器边界和端口见
`docs/operations/DEV_INFRA_SERVER.md` 与 `docs/operations/SIM_SERVER.md`。
