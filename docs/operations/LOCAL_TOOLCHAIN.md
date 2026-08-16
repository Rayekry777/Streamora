# 本机工具链

Streamora 本机自动化只从 `D:\aitool` 读取以下工具：

- `node`：`D:\aitool\node`
- `pnpm`：由同目录的 Corepack 固定为 `11.19.0`；Corepack、store 与下载缓存均位于 `D:\aitool\pnpm`
- `mvn`：`D:\aitool\maven`
- `gh`：`D:\aitool\gh`
- `codex`：`D:\aitool\codex`

## 首次迁移收尾

Maven 和 GitHub CLI 已直接迁移。若 Node 源目录仍受 `SYSTEM` 所有权保护，请在**管理员 PowerShell** 中于仓库根目录执行一次：

```powershell
.\scripts\ops\Move-StreamoraNode.ps1
```

该脚本仅处理 `E:\Nodejsnew` 到 `D:\aitool\node` 的迁移，删除已确认为空的失败目标目录，随后启用 Corepack 的 `pnpm@11.19.0` 并替换用户 PATH 中的旧 Node 项。

在 PowerShell 会话或本机守护任务中先加载：

```powershell
. .\scripts\ops\Initialize-StreamoraToolchain.ps1 -RequireCodex
```

验证工具位置、版本和缓存位置：

```powershell
.\scripts\ops\Test-StreamoraToolchain.ps1 -RequireCodex
```

完成 Node 迁移后，在同一已初始化会话中执行 `codex login`，按浏览器打开的 ChatGPT 授权页完成独立 CLI 登录；随后再运行上面的验证命令。不要移动或修改 WindowsApps 中的桌面 Codex 和 `C:\Users\32889\.codex`。

本机 Loop Engineering 使用 `Start-StreamoraLoop.ps1 -Pr <编号>`、`-Issue <编号>` 或 `-Watch`。它只在 `D:\aitool\loop-state` 的隔离副本写入，GitHub Actions 与 Ubuntu Runner 不使用该目录。

GitHub Actions 和 Ubuntu 部署 Runner 使用 Linux 工具链，不使用 Windows 的 `D:\aitool` 路径。Maven Wrapper `mvnw` 仍在仓库中；本机加载工具链后，它的缓存位于 `D:\aitool\maven-cache`。
