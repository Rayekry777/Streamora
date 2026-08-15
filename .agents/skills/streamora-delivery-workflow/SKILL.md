---
name: streamora-delivery-workflow
description: 管理 Streamora 项目的阶段推进、里程碑门禁、验收记录和交付状态。仅在用户明确要求开始、继续、验收或汇报某个 Streamora 里程碑，或显式调用该 Skill 时使用；不要因普通编码任务自动推进阶段。
---

# Streamora 交付工作流

## 执行顺序

1. 完整读取 `docs/project/PROJECT_ROADMAP.md`、`docs/development/BACKEND_DEVELOPMENT.md` 和当前阶段验收文件。
2. 检查工作区、已有实现、验证记录和用户未提交改动，以事实校正阶段状态。
3. 只执行当前里程碑；按“契约、后端主链路、用户端与管理端、联调、验收”推进。
4. 使用 [里程碑门禁](references/milestone-gates.md) 判断是否达到出口条件。
5. 使用 [状态规则](references/status-rules.md) 更新状态，禁止跳过“待验收”。
6. 按 [交付报告](references/delivery-report.md) 写入阶段验收文件并向用户报告。

## 强制约束

- 用户端与管理端按领域切片同步推进，后端契约领先半步。
- 未得到用户验收，不得把里程碑标记为“已完成”，也不得主动进入下一里程碑。
- 一个阶段未完成时，优先补齐本阶段缺口；不得用后续阶段的占位实现伪装完成。
- 所有验证记录必须写明实际执行内容、结果和未验证项。
- 不代替用户提交或推送 Git，除非用户在当前请求中明确授权。
