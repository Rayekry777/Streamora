# Streamora Sim Core 服务器

## 服务器身份

| 项目 | 值 |
| --- | --- |
| 地址 | `192.168.126.128` |
| 主机名 | `streamora-sim` |
| SSH 用户 | `ub001` |
| Compose 项目 | `streamora` |
| 代码目录 | `/opt/streamora` |
| Runner 标签 | `self-hosted`, `linux`, `x64`, `streamora-core` |

这台 VM 是已部署 Core 环境，运行前端、Gateway、核心 Java 服务和生产形态中间件，并承载阶段验收与部署 Runner。不要把 IDEA 服务注册到 sim 的 Nacos。

## 项目与数据

sim 继续使用现有 Compose 项目名 `streamora`，不要改成 `streamora-sim`，以免改变命名卷名称并造成现有 PostgreSQL、Redis、MinIO 数据无法复用。阶段验收使用独立项目名和 `compose.stage-clean.yml` 的临时卷，不删除 sim 持久卷。

查看 sim Core 容器：

```bash
docker ps -a --filter label=com.docker.compose.project=streamora
```

sim 上可能还有 Ray、YApi 等无关容器，查看时必须使用上面的 Compose 项目过滤条件。

## 部署与 E2E

- GitHub Actions 的 `deploy-core.yml` 在本机 Runner 上执行，不由 Windows Docker 或 dev VM 代替。
- 阶段 PR 的功能门禁通过后，GitHub 自动运行升级证据和独立干净安装证据；验收失败只保留诊断并停止。
- 阶段验收成功后，只有用户明确确认，Codex 才合并并以实际 squash SHA 触发 Core 部署和已部署浏览器 E2E。
- 部署失败由现有部署脚本尝试安全回滚；Codex 只报告诊断，不创建自动修复分支或重试。

Windows 查看浏览器环境：

```powershell
.\scripts\ops\open-streamora-tunnel.ps1
```

随后访问 `http://127.0.0.1:3000` 和 `http://127.0.0.1:3001`。隧道目标是 sim `.128`，不是 dev `.129`。

## 边界

- sim 的 Nacos、PostgreSQL、Redis、RocketMQ、MinIO 只供已部署 Core 使用。
- 本地 IDEA 使用 dev `.129` 的中间件。
- 不在 sim 上手工启动本地开发 Java 服务，不把 sim 容器当作 dev 容器调试。
