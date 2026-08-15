# 前端规范

- 技术栈：Vue 3、TypeScript strict、Vite、Vue Router、Pinia、TanStack Vue Query、UnoCSS。
- `web` 和 `admin-web` 独立构建、独立路由、独立会话与环境变量。
- 使用生成的 OpenAPI 类型和薄 API Client；页面不得手写重复响应类型。
- Vue Query 管理远端数据、请求状态和失效；Pinia 管理认证视图状态、宠物状态和本地偏好。
- 组件默认使用 `<script setup lang="ts">`；组合式函数命名 `useXxx`，事件和 Props 必须显式类型化。
- 所有页面提供 loading、empty、error、permission denied 状态；错误不得只写入控制台。
- ID 全程按字符串处理；时间按 UTC 接收，在界面按用户时区格式化。
- 敏感 Token 不写 localStorage；用户与管理端会话 Cookie 和请求客户端完全隔离。
- 遵循 WCAG 基础要求：语义标签、可见焦点、键盘操作、文本对比度和 reduced-motion。
- 品牌为 Streamora，不复制 Bilibili 的商标、素材和界面视觉。

