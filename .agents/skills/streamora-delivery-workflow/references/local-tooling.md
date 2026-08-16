# 本机工具定位

在 `D:\JavaPro\bilibili\Streamora` 的 Windows 开发环境中，直接使用以下命令，不再探测可执行文件位置：

| 用途 | 固定调用方式 | 说明 |
| --- | --- | --- |
| GitHub CLI | `C:\Users\32889\AppData\Local\Programs\GitHub CLI\bin\gh.exe` | 用于 PR、Actions、Issue 和远端状态查询。 |
| pnpm | `C:\Users\32889\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin\fallback\pnpm.cmd` | 用于前端、OpenAPI 和治理命令。 |
| Maven | `.\mvnw.cmd` | 必须优先使用仓库 Maven Wrapper，保证与 CI 一致。 |
| Maven 后备 | `D:\Maven\apache-maven-3.9.11-bin\apache-maven-3.9.11\bin\mvn.cmd` | 仅在 Wrapper 不可用时使用。 |

## 固定验证命令

```powershell
& 'C:\Users\32889\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin\fallback\pnpm.cmd' typecheck
& 'C:\Users\32889\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin\fallback\pnpm.cmd' lint
& 'C:\Users\32889\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin\fallback\pnpm.cmd' test
& 'C:\Users\32889\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin\fallback\pnpm.cmd' build
.\mvnw.cmd -B -ntp verify
```

GitHub Actions 运行在 Linux Runner，工作流中只能使用 `pnpm/action-setup`、`actions/setup-java` 和 `bash ./mvnw`；禁止把本机 Windows 路径写入 `.github/workflows`。
