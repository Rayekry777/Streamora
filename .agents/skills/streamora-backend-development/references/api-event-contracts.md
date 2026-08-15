# 接口与事件规则

- HTTP 契约以各服务 OpenAPI 为真源；Dubbo 契约以共享的 Protobuf API 模块为真源。
- 外部响应使用 `data`、`requestId`；错误使用 `error.code`、`error.message`、`error.details`、`requestId`。
- 列表使用 `items`、`nextCursor`、`hasMore`，不得新增页码分页。
- 事件信封必须包含 `eventId`、`eventType`、`schemaVersion`、`aggregateId`、`occurredAt`、`traceId` 和 `payload`。
- 事件名称使用过去式 `<domain>.<aggregate>.<action>.v<n>`，例如 `video.asset.transcoded.v1`。
- 破坏性事件变化发布新版本；消费者在迁移窗口内兼容上一版本。
- Agent SSE 使用 `message.delta`、`pet.state`、`tool.started`、`tool.completed`、`action.proposed`、`memory.updated`、`usage.completed`、`error`。
- 写工具确认必须校验用户、动作类型、目标、参数摘要、过期时间、一次性状态和幂等键。

