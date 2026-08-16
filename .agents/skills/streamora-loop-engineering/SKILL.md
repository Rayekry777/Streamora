---
name: streamora-loop-engineering
description: 管理 Streamora 的自动 Git 提交、首次推送门禁、PR 创建和自动合并、GitHub 验证、CI 或部署失败修复及根因子 Agent 循环。用于用户要求提交、推送、PR、合并、自动修复、部署修复、Issue 自动开发或 Loop Engineering 时。
---

# Streamora Loop Engineering

将任务推进到可验证终态，而不是只生成一次改动。先读取 [远端循环](references/remote-loop.md)；本地提交规则读取 `../streamora-delivery-workflow/references/commit-loop-engineering.md`。

## 本地循环

1. 先读取 [本机工具定位](../streamora-delivery-workflow/references/local-tooling.md)，GitHub 操作直接使用固定 GitHub CLI 路径。
2. `master` 只跟踪 `origin/master`：先执行 `git fetch origin --prune` 与 `git pull --ff-only`，禁止在 `master` 上开发、提交、merge 或 rebase。若已分叉，先归档或移出未合并工作再同步主线。
3. 普通人工功能从同步后的主线创建 `feature/<领域>-<主题>`；阶段交付使用唯一的 `feature/phase-<N>-<主题>` Draft PR。本机自动任务使用 `agent/<loop-id>-<slug>`，自动阶段任务在名称中追加 `phase-<N>`，部署修复保留 `deploy-repair/*`。PR 只能以这三类来源分支合并到 `master`。
4. 为任务生成 `Loop-Id`，按用户端、管理端、后端、契约或基础设施划分提交边界。
5. 检查当前分支、工作区和活动任务。仅当分支非受保护、无无关改动且属于当前任务时复用；否则创建 `agent/<loop-id>-<slug>`。
6. 实现后启动独立质量 Agent。质量 Agent 可在相同范围内修复并复验，但不得提交或推送。
7. 通过门禁后自动创建中文 Conventional Commit，包含四个正文区块和 Loop Trailer。
8. 对话任务在首次推送前请求用户确认；`agent:ready` 只授权实现，Issue 自动任务还必须由用户显式传入 `-AllowFirstPush` 才能首次推送。

## 远端循环

- GitHub Actions 只运行验证、部署、浏览器联调、回滚与脱敏诊断；不在 GitHub 内调用模型或写入修复。
- 功能 PR 先通过改动分类后的“功能验证门禁”；阶段分支还必须由写权限用户对当前 SHA 添加 `stage:ready`，通过串行的“阶段门禁”后才能自动合并。
- 本机 `Start-StreamoraLoop.ps1` 读取最新 Artifact，在 `D:\aitool\loop-state` 隔离副本中用已认证 Codex CLI 修复。首次失败立即开始，每轮必须是“修改、边界验证、本地验证、提交、推送、重新验证”。
- 同一根提交最多三次本机修复；无安全补丁、验证失败或第三次仍失败时，停止写入并创建 `agent:blocked` Issue。
- PR 的最新提交通过全部必需检查后由本机守护启用 squash 自动合并。部署修复使用独立 `deploy-repair/*` 分支；普通 CI 修复回推原 PR 分支。

## 不可突破的边界

- 不直接写入 `master`、`main` 或其他受保护分支，不绕过状态检查。
- 不读取、输出或修改密钥、Token、权限、工作流治理、数据库迁移、依赖锁、持久卷删除逻辑和安全策略。
- 每次循环检查 `Loop-Id`、根提交、尝试次数与并发锁；不重复处理过期结果。
- 验证失败不允许写成通过。外部凭据、Runner 或基础设施阻塞必须如实记录。
