# 本机工具定位

在 `D:\JavaPro\bilibili\Streamora` 的 Windows 开发环境中，先加载 `scripts\ops\Initialize-StreamoraToolchain.ps1`。本机自动化只从 `D:\aitool` 使用工具，不再探测或调用旧目录。

| 用途 | 固定调用方式 | 说明 |
| --- | --- | --- |
| GitHub CLI | `D:\aitool\gh\bin\gh.exe` | 用于 PR、Actions、Issue 和远端状态查询。 |
| Node / pnpm | `D:\aitool\node\node.exe` / `D:\aitool\node\pnpm.cmd` | pnpm 由 Node Corepack 固定为 `11.19.0`，缓存位于 `D:\aitool\pnpm`。 |
| Maven | `.\mvnw.cmd` | 必须优先使用仓库 Maven Wrapper，保证与 CI 一致。 |
| Maven 后备 | `D:\aitool\maven\bin\mvn.cmd` | 仅在 Wrapper 不可用时使用；依赖与 Wrapper 缓存位于 `D:\aitool\maven-cache`。 |

## 固定验证命令

```powershell
. .\scripts\ops\Initialize-StreamoraToolchain.ps1
pnpm typecheck
pnpm lint
pnpm test
pnpm build
.\mvnw.cmd -B -ntp verify
```

GitHub Actions 运行在 Linux Runner，工作流中只能使用 `pnpm/action-setup`、`actions/setup-java` 和 `bash ./mvnw`；禁止把本机 Windows 路径写入 `.github/workflows`。
