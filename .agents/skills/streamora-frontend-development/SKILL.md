---
name: streamora-frontend-development
description: 设计、实现、修复、审查和验证 Streamora 的 Vue 3 用户端 web、运营管理端 admin-web、视频播放器和全局 Live2D AI 宠物。用于 Vue、TypeScript、Vite、Pinia、Vue Query、UnoCSS、xgplayer、路由、RBAC、SSE、播放器反应事件或前端联调任务。
---

# Streamora 前端开发

## 开始前

1. 读取 `docs/project/PROJECT_ROADMAP.md`、当前阶段验收文件和相关 API 契约。
2. 判断目标属于 `web`、`admin-web` 或共享契约；禁止让管理端依赖宠物运行时。
3. 读取 [前端规范](references/frontend-standards.md)；涉及宠物时再读取 [全局宠物契约](references/global-pet-contract.md) 与 [反应事件](references/reaction-events.md)；涉及管理端时读取 [管理端规范](references/admin-frontend-standards.md)。
4. 检查现有组件、路由、状态、测试和用户改动后再实现。

## 实现流程

1. 契约未接通时使用与 OpenAPI 同形的 Mock；联调后删除该路径的临时分支。
2. API 数据放入 Vue Query，纯客户端 UI 状态放入 Pinia；禁止重复缓存同一服务端实体。
3. 用户端和管理端共享设计令牌与生成的 API 类型，不共享页面布局、会话状态或领域 Store。
4. 保持页面可访问性、键盘操作、响应式布局、加载/空/错状态和 reduced-motion。
5. 按 [前端验证](references/frontend-verification.md) 执行类型、单元、组件、端到端和性能验证。

## 宠物强制约束

- `GlobalPetHost` 位于路由出口之外，整个用户端只创建一个渲染实例。
- 使用 `PetRenderer` 接口隔离 Cubism，实现静态占位和 WebGL 降级。
- 全屏时通过 Teleport 移入播放器全屏容器，不重新创建宠物。
- 宠物外壳默认 `pointer-events: none`，仅拖动和按钮热区可交互。
- 反应优先级固定为用户直接操作、播放状态、视频语义、空闲行为。
