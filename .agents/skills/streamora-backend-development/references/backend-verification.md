# 后端验证

按风险从小到大执行：

1. Maven 版本和相关模块依赖树。
2. 相关单元测试与契约测试。
3. 相关模块 `compile`。
4. 涉及认证、迁移、消息或外部适配器时执行集成测试。
5. 修改 HTTP 时验证生成的 OpenAPI、错误响应和权限。
6. 修改事件时验证 Outbox、重复投递、乱序或重启恢复。
7. 修改 Agent 时验证真实 Provider、Mock 降级、SSE 断开、工具确认与用量事件。

阶段 1 前不生成发布包；进入部署阶段后再执行 package、镜像和 Compose 验证。把实际结果写入 `docs/development/BACKEND_DEVELOPMENT.md` 与对应阶段验收文件。
