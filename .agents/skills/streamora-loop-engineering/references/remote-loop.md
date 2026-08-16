# 远端循环规则

## 状态

使用 `agent:ready`、`agent:running`、`agent:repairing`、`agent:root-cause`、`agent:blocked`、`agent:done`。Issue 或 PR 的隐藏状态注释保存 Loop-Id、根提交、分支、尝试、运行 URL 和终止原因。

## 提交元数据

每个自动提交必须有“功能明细”“验证结果”“未运行项”“阶段状态”四个正文区块，并追加：

```text
Streamora-Loop-Id: <id>
Streamora-Loop-Root: <sha>
Streamora-Loop-Attempt: <0-4，功能根提交为 0>
Streamora-Loop-Mode: <feature|ci-repair|deploy-repair|root-cause>
```

## 修复顺序

1. GitHub Actions 失败后上传脱敏诊断；本机守护下载最新诊断并拒绝过期 SHA。
2. 本机 Codex CLI 在 `D:\aitool\loop-state` 隔离副本中生成最小修复，执行禁止路径与受影响本地验证，再提交推送。
3. 首次失败立即修复，同一根提交最多三轮。无安全补丁、边界/本地验证失败或第三轮仍失败时创建 `agent:blocked` Issue，不再自动重试。

## 自动合并

正常 PR 与修复 PR 都只能在最新 SHA 全部必需检查成功后由本机守护启用 squash 自动合并。守护必须查询实际状态，不能只依赖旧事件结论。
