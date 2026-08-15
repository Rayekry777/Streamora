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

1. 最小修复读取最新诊断，在原 PR 分支修改，执行精确失败检查和相关回归，提交并推送。
2. 默认连续三次失败后才允许根因子 Agent；最小修复 Agent 无法生成安全变更时可立即升级。子 Agent 使用新会话和隔离工作区，实际修改并验证；受限编排步骤只提交和推送其工作树中的实际变更。
3. 三轮最小修复仍失败时，或最小修复 Agent 无法生成安全变更时，启动一次独立根因子 Agent。仍失败时创建 `agent:blocked` Issue，不再自动重试。

## 自动合并

正常 PR 与修复 PR 都只能在最新 SHA 全部必需检查成功后 squash 合并。合并工作流必须查询实际状态，不能只依赖旧事件结论。
