# 阶段发布验收清单

每个阶段都按下面的闭环推进。部署环境只运行 `core` Profile，端口仅绑定到虚拟机 `127.0.0.1`。

## 开发与合并

1. 从 `master` 创建 `feature/phase-<阶段>-<主题>` 分支。
2. 在本地执行 `pnpm contract:lint`、`pnpm contract:generate`、`pnpm typecheck`、`pnpm lint`、`pnpm test` 与 `./mvnw -B -ntp verify`。
3. 推送分支并创建到 `master` 的 PR；GitHub 的“验证”工作流必须全部通过。
4. 合并 PR 到 `master`。不要在虚拟机的 `/opt/streamora` 手工拉取代码。

## 部署与浏览器联调

1. `master` 的验证通过后，“部署 Core”会由 `streamora-core` Runner 检出同一 SHA、构建 Compose 镜像并启动服务。
2. 脚本先检查 Compose、前端 `/healthz`、网关与 identity readiness、容器运行状态。
3. 接着在 Runner 上以 Playwright Chromium 访问实际用户端和管理端，覆盖用户注册、公共宠物切换个人宠物、管理员登录、运营概览和双向会话隔离。
4. 只有全部检查通过，才把本次 SHA 写为最后健康版本。
5. 浏览器联调报告和失败视频、截图、追踪会作为 `deployed-e2e-report` artifact 保留 14 天。

## 失败处理

- 未涉及 `db/migration`：部署或浏览器联调失败后，自动恢复到最后健康镜像，并保留诊断 artifact。
- 涉及 Flyway 迁移：不自动回滚数据库；保留诊断并进入自动修复流程或人工处理。
- 自动修复最多三轮；每轮以 PR 方式重新验证、合并并触发完整部署和浏览器联调。

## 本地浏览器查看虚拟机

首次为 `ub001` 配好私钥后，在 Windows PowerShell 保持以下命令运行：

```powershell
.\scripts\ops\open-streamora-tunnel.ps1
```

然后访问 `http://127.0.0.1:3000`（用户端）和 `http://127.0.0.1:3001`（管理端）。关闭命令窗口或按 `Ctrl+C` 即关闭隧道。
