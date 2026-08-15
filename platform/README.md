# 平台与部署目录

阶段 1 起在此维护：

- `compose/`：infra、core、full 三套本地运行配置。
- `observability/`：Prometheus、Grafana、Loki、Tempo。
- `gateway/`：边缘代理与部署路由资源。
- `scripts/`：可重复的环境检查、启动和运维脚本。

应用源码不放入本目录，第三方密钥和生产凭据不得提交。

