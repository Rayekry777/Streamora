# Streamora 前端开发契约

```yaml
version: 3
updatedAt: 2026-08-16
implementationStatus: 阶段 2 已完成；阶段 3 上传与播放 API 契约进行中
```

## 当前工程事实

- `apps/web`：用户端、创作者入口、播放器边界与唯一 `GlobalPetHost`。
- `apps/admin-web`：桌面优先运营端，拥有独立布局和路由，不加载任何宠物运行时。
- 两端使用 Vue 3、TypeScript、Vite、Pinia、Vue Query、Vue Router、UnoCSS 和 Vitest。
- `packages/ui-tokens` 只共享视觉令牌；用户状态、管理状态、会话与权限不得跨应用共享。
- OpenAPI 类型生成到 `packages/openapi/generated/`，源文件是 `packages/openapi/streamora-v1.yaml`。
- 用户端首页与播放页采用内容优先的视频社区信息架构；xgplayer 只在播放页动态加载，Lucide 用于操作图标。
- 用户端视觉主题为 Streamora 自有的 `Vice Coast Night`：深夜蓝黑表面、霓虹青导航状态、珊瑚粉创作操作、洋红 AI 宠物提示；不得使用 GTA 商标、角色、截图、字体或其他可识别资产。
- `web` 通过 `VITE_USE_REMOTE_VIDEO_API=true` 切换至阶段 3 真实 API；默认使用与 OpenAPI 同形的合法演示数据，正式联调后不再使用该后备路径。
- 阶段 3 已生成分片上传会话与完成上传类型；用户端上传页在 media-service 实现后接入真实 API，当前不伪造上传成功。

## 全局宠物不变量

- `GlobalPetHost` 位于 `RouterView` 外，路由切换不能销毁宿主。
- 当前阶段使用静态占位渲染；`PetRenderer` 定义后续 Live2D/WebGL 与静态降级边界。
- 管理端测试明确断言不存在 `global-pet-host`。
- 阶段 5 才实现拖动、安全区、Teleport 全屏和减少动画模式的完整行为。
- 阶段 2 已通过活动宠物查询键绑定用户主体；登录后在同一宿主 DOM 上从 `PUBLIC` 原位切换到 `PERSONAL`。

## 身份与权限现状

- `web` 使用 `user-auth` Pinia Store，只调用 `/api/v1/auth/**`，登录前展示公共宠物，登录后展示个人宠物。
- `admin-web` 使用独立 `admin-auth` Store，只调用 `/admin-api/v1/auth/**`，不读取用户 Store 或用户 Cookie。
- 管理路由声明所需权限，路由守卫负责拒绝直接访问，侧栏仅展示当前角色拥有的模块；后端仍执行最终 RBAC 校验。
- 两端 CSRF Token 只保存在内存会话状态中，身份 Cookie 由浏览器 HttpOnly 管理。

## 验证记录

| 日期 | 检查 | 结果 |
|---|---|---|
| 2026-08-15 | 两端 `vue-tsc` | 通过 |
| 2026-08-15 | 两端 ESLint | 通过，0 error/0 warning |
| 2026-08-15 | 两端 Vitest | 3 个测试文件、4 个测试通过；包含全局宠物宿主原位切换和管理路由越权拒绝 |
| 2026-08-15 | 两端 Vite 生产构建 | 通过 |
| 2026-08-15 | 阶段 2 类型与规范 | 用户/管理 API、Store、路由守卫与登录页通过 `vue-tsc` 和 ESLint | 通过，0 error/0 warning |
| 2026-08-16 | 首页与播放页改造 | `web` 类型检查、ESLint、Vitest、生产构建、桌面与移动截图 | 通过；4 个测试文件、5 个测试，播放器动态分包 |

## 后续约束

- 外部请求只使用 `/api/v1` 或 `/admin-api/v1`。
- BIGINT 在 TypeScript 中按字符串处理，列表使用游标分页。
- 管理路由和按钮权限必须同时校验；前端隐藏不代替后端授权。
- SSE 事件、播放器事件和宠物动作使用判别联合类型，禁止散落魔法字符串。
## 开源方案优先

所有新增 UI、播放器、渲染器、SDK 和交互能力都必须先遵循 [开源方案优先政策](OPEN_SOURCE_ADOPTION_POLICY.md) 完成调研、评估与记录；无可采纳方案才允许自研。
