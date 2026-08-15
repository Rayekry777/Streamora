# Streamora 前端开发契约

```yaml
version: 1
updatedAt: 2026-08-15
implementationStatus: 阶段 1 待验收
```

## 当前工程事实

- `apps/web`：用户端、创作者入口、播放器边界与唯一 `GlobalPetHost`。
- `apps/admin-web`：桌面优先运营端，拥有独立布局和路由，不加载任何宠物运行时。
- 两端使用 Vue 3、TypeScript、Vite、Pinia、Vue Query、Vue Router、UnoCSS 和 Vitest。
- `packages/ui-tokens` 只共享视觉令牌；用户状态、管理状态、会话与权限不得跨应用共享。
- OpenAPI 类型生成到 `packages/openapi/generated/`，源文件是 `packages/openapi/streamora-v1.yaml`。

## 全局宠物不变量

- `GlobalPetHost` 位于 `RouterView` 外，路由切换不能销毁宿主。
- 当前阶段使用静态占位渲染；`PetRenderer` 定义后续 Live2D/WebGL 与静态降级边界。
- 管理端测试明确断言不存在 `global-pet-host`。
- 阶段 5 才实现拖动、安全区、Teleport 全屏和减少动画模式的完整行为。

## 验证记录

| 日期 | 检查 | 结果 |
|---|---|---|
| 2026-08-15 | 两端 `vue-tsc` | 通过 |
| 2026-08-15 | 两端 ESLint | 通过，0 error/0 warning |
| 2026-08-15 | 两端 Vitest | 2 个测试文件、2 个测试通过 |
| 2026-08-15 | 两端 Vite 生产构建 | 通过 |

## 后续约束

- 外部请求只使用 `/api/v1` 或 `/admin-api/v1`。
- BIGINT 在 TypeScript 中按字符串处理，列表使用游标分页。
- 管理路由和按钮权限必须同时校验；前端隐藏不代替后端授权。
- SSE 事件、播放器事件和宠物动作使用判别联合类型，禁止散落魔法字符串。

