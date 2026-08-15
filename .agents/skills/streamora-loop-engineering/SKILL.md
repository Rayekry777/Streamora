---
name: streamora-loop-engineering
description: 管理 Streamora 的自动 Git 提交、首次推送门禁、PR 创建和自动合并、GitHub 验证、CI 或部署失败修复及根因子 Agent 循环。用于用户要求提交、推送、PR、合并、自动修复、部署修复、Issue 自动开发或 Loop Engineering 时。
---

# Streamora Loop Engineering

将任务推进到可验证终态，而不是只生成一次改动。先读取 [远端循环](references/remote-loop.md)；本地提交规则读取 `../streamora-delivery-workflow/references/commit-loop-engineering.md`。

## 本地循环

1. 为任务生成 `Loop-Id`，按用户端、管理端、后端、契约或基础设施划分提交边界。
2. 检查当前分支、工作区和活动任务。仅当分支非受保护、无无关改动且属于当前任务时复用；否则创建 `agent/<loop-id>-<slug>`。
3. 实现后启动独立质量 Agent。质量 Agent 可在相同范围内修复并复验，但不得提交或推送。
4. 通过门禁后自动创建中文 Conventional Commit，包含四个正文区块和 Loop Trailer。
5. 对话任务在首次推送前请求用户确认；Issue 的 `agent:ready` 标签即为首次推送授权。

## 远端循环

- PR 验证失败时，在原 PR 分支执行至多三轮最小修复。每轮必须是“修改、验证、提交、推送、重新验证”。
- 前三轮失败由 DeepSeek 受限补丁代理执行；第三轮仍失败后启动独立 Codex 根因子 Agent。Codex 在隔离 Runner 工作树实际修改受限源码，外层编排只对边界校验后的实际改动创建第四次提交并推送原 PR 分支。
- 第四轮无法通过，或碰到禁止范围时，停止写入，设置 `agent:blocked` 并创建含 Artifact 链接的 Issue。
- PR 的最新提交通过全部必需检查后自动 squash 合并。部署修复使用独立 `deploy-repair/*` 分支；普通 CI 修复回推原 PR 分支。

## 不可突破的边界

- 不直接写入 `master`、`main` 或其他受保护分支，不绕过状态检查。
- 不读取、输出或修改密钥、Token、权限、工作流治理、数据库迁移、依赖锁、持久卷删除逻辑和安全策略。
- 每次循环检查 `Loop-Id`、根提交、尝试次数与并发锁；不重复处理过期结果。
- 验证失败不允许写成通过。外部凭据、Runner 或基础设施阻塞必须如实记录。
