# 平台与部署目录

本目录维护：

- `compose/compose.yml`：通过 `infra`、`core`、`full` Profile 提供唯一编排入口。
- `docker/`：17 个后端运行单元共享的参数化镜像构建文件。
- `observability/`：Prometheus、Grafana、Loki、Tempo。
- `gateway/`：边缘代理与部署路由资源。
- `scripts/`：可重复的环境检查、启动和运维脚本。

应用源码不放入本目录，第三方密钥和生产凭据不得提交。

启动前复制 `compose/.env.example` 为 `compose/.env` 并替换占位密码。完整命令见 `docs/deployment/`。
