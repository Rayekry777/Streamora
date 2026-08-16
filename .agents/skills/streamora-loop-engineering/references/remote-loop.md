# 远端交付规则

## 状态顺序

```text
用户推送 -> 功能验证 -> 阶段验收（阶段 PR） -> 用户确认 -> squash 合并 -> 晋升校验 -> 手动部署 -> 结果汇报
```

- 普通 PR 只需要功能验证。
- 阶段 PR 必须是 `feature/phase-<N>-<主题>`，当前 Head SHA 必须通过“功能验证门禁”，PR 非 Draft 且 `master...Head` 的 `behind_by` 为 0。
- 阶段验收由“验证”工作流成功事件自动触发，拒绝旧 SHA、Fork PR 和旧阶段证据。
- `stage-promotion.yml` 只校验验收候选树与 squash 后 `master` 树一致，并登记健康 SHA；不自动合并或部署。
- `deploy-core.yml` 只接受 `workflow_dispatch`。Codex 只有在用户确认后才触发，并等待部署和浏览器 E2E 的最终结论。

## 失败处理

任何功能、阶段、晋升或部署失败都进入停止状态。保留 GitHub 日志和脱敏 Artifact，向用户报告运行 URL、失败类别、SHA 和下一步所需的本地修复；不创建 PR、不自动提交、不自动推送、不自动重试。
