# 阶段发布验收清单

每个阶段使用一个以 `master` 为目标的 `feature/phase-<阶段>-<主题>` PR。开发、调试和最小范围验证在 Windows IDEA 与 dev VM 中完成；sim 只用于真实阶段验收和已部署浏览器 E2E。

## GitHub 仓库设置

- `master` 禁止直接推送，只允许 PR squash 合并，并要求合并前分支为最新。
- 必需检查仅为 `功能验证门禁`；不要把按路径选择的子任务、阶段验收或晋升校验设为全局必需检查。
- 阶段验收和部署使用 `stage-vm` Environment 与 `streamora-core` 自托管 Runner。
- 管理员首次合入本流程后核对 Ruleset/Branch protection；工作流不自行修改保护规则。

## 本地与推送

1. IDEA 启动当前 Java 服务，连接 dev `.129` 中间件；开发者可以手动运行单模块测试和断点调试。
2. Codex 按实际改动运行受影响 Maven 模块、前端包、Compose 和必要联调验证，并如实记录未运行项。
3. 阶段本地完成后，Codex 创建中文 Conventional Commit；不会自动推送。
4. 由用户决定何时推送并告知 Codex PR 编号。GitHub 随即运行按改动分类的功能验证。

## 自动阶段验收与确认发布

1. 当前 Head SHA 的“功能验证门禁”成功后，`workflow_run` 自动检查阶段分支、非 Draft 状态和与 `master` 的同步状态。
2. 条件满足时，sim 执行升级路径、独立干净安装、健康检查和累计 Playwright E2E；验收结果与失败诊断作为 Artifact 保存。
3. Codex 只汇报当前 SHA 的阶段结果。阶段失败后停止，等待用户在本地修复、验证并再次推送。
4. 阶段成功后，用户明确发出“合并并部署”指令；Codex squash 合并并等待阶段晋升校验候选树与实际 `master` 树一致。
5. 晋升成功后，Codex 以实际 squash SHA 手动触发 Core 部署，等待已部署浏览器 E2E，并只汇报最终结论。

## 失败、证据与恢复

- 功能验证、阶段验收、晋升或部署失败时，不创建修复分支、不自动提交、不自动推送、不自动重试。
- 失败运行保留脱敏日志、容器状态、Playwright 报告和部署诊断；Codex 报告运行链接、SHA 和失败类别。
- 部署脚本继续在无迁移变化时恢复上一健康镜像；迁移变化或缺少健康记录时停止，绝不删除持久卷伪造恢复。
- 阶段验收的干净安装只使用独立项目、端口与 tmpfs，不执行 `docker compose down -v` 或通用卷删除。

## 本地浏览器查看 sim

```powershell
.\scripts\ops\open-streamora-tunnel.ps1
```

随后访问 `http://127.0.0.1:3000`（用户端）和 `http://127.0.0.1:3001`（管理端）。
