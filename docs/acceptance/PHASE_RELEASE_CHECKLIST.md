# 阶段发布验收清单

每个阶段使用一个到 `master` 的 Draft PR。功能完成时做本地与托管 Runner 轻量验证；只有阶段出口满足后才占用虚拟机。部署端口仍只绑定虚拟机 `127.0.0.1`。

## GitHub 仓库设置

- `master` 继续禁止直接推送，只允许 PR squash 合并，并启用“合并前分支必须为最新”。
- 必需检查固定为 `功能验证门禁` 和 `阶段门禁`；不要把按路径选择的子任务设为必需检查。
- 启用仓库自动合并，阶段机使用 `stage-vm` Environment 与 `streamora-core` 自托管 Runner 标签。
- 首次合入本计划后由仓库管理员在 Ruleset/Branch protection 中核对以上设置；工作流不能自行降低或覆盖现有保护规则。

## 阶段分支与功能验证

1. 同步 `master` 后创建 `feature/phase-<阶段>-<主题>`；自动 Issue 使用 `agent/<loop-id>-phase-<阶段>-<主题>`。
2. 首次推送前请求用户确认；自动 Issue 任务必须显式使用 `-AllowFirstPush`，并立即创建 Draft PR。后续功能提交继续进入同一个阶段 PR且无需重复授权。
3. 每个提交保持单一交付边界，前端与后端不混入同一个功能提交。
4. 本地按改动范围运行 OpenAPI、类型、Lint、单元/集成测试、构建和 Maven 验证。
5. GitHub“验证”工作流对变更分类；Docker、迁移和跨服务改动在托管 Runner 启动临时依赖容器。
6. “功能验证门禁”必须在当前 Head SHA 上成功；纯文档 PR 会明确跳过不相关任务，不会留下 Pending 检查。

## 阶段授权与虚拟机验收

1. 阶段出口测试与文档准备完毕后，将 PR 转为 Ready for review，并同步到最新 `master`。
2. 由具有写权限的用户添加 `stage:ready`。标签只授权当前 Head SHA；后续提交会使旧验收结果失效。
3. “阶段验收”先验证写权限、分支名、普通 CI、Draft 状态和 `master` 差异，再进入串行的 `stage-vm` Environment。
4. 阶段清单固定声明阶段号、Compose 服务、依赖容器、后端测试、Playwright 测试目录、测试数据版本和最低资源；阶段 8 才使用完整 `full` Profile。
5. 虚拟机先验证上一健康阶段到候选 SHA 的升级路径，再用独立项目名、独立端口和 tmpfs 执行干净安装。
6. Compose 配置、容器健康、网关与服务 readiness、真实跨服务流程和累计 Playwright E2E 全部通过后，“阶段门禁”才成功。
7. 阶段 3 必须存在真实 `@phase-3` 上传、转码、审核、发布和 HLS 播放 E2E；缺少该证据时清单校验会拒绝验收。
8. 阶段门禁成功后，本机守护对同一 Head SHA 启用 squash 自动合并；“阶段晋升关联”会校验合并提交与候选源码树完全一致，再把验收记录关联到新的 `master` SHA，不重复运行完整验收。
9. 合并后的阶段状态为“待验收”，只有用户确认后改为“已完成”。

## 失败、证据与恢复

- 功能验证失败：在原阶段分支完成最小修复、本地复验和推送，不创建部署修复分支。
- 阶段验收失败：上传脱敏 Compose 日志、健康检查、镜像清单以及 Playwright 视频、截图和 Trace；同一根提交最多修复三轮。
- 无迁移变化时恢复上一健康镜像；迁移变化或恢复证据不足时停止自动写入并进入 `agent:blocked`，不得删除持久卷伪造恢复。
- 隔离干净安装只使用 tmpfs，清理时不执行 `docker compose down -v` 或通用卷删除。
- `deploy-repair/*` 仅用于已经合并版本的手动部署或晋升故障。

## 本地浏览器查看虚拟机

首次为 `ub001` 配好私钥后，在 Windows PowerShell 保持以下命令运行：

```powershell
.\scripts\ops\open-streamora-tunnel.ps1
```

然后访问 `http://127.0.0.1:3000`（用户端）和 `http://127.0.0.1:3001`（管理端）。关闭命令窗口或按 `Ctrl+C` 即关闭隧道。
