# API 契约

```yaml
version: v1
externalPrefix: /api/v1
adminPrefix: /admin-api/v1
timeZone: UTC
idJsonType: string
pagination: cursor
```

## HTTP 约定

- 用户和管理流量都经过 `gateway-service`；管理请求再由 `admin-service` 编排。
- JSON 使用 camelCase；时间使用 ISO 8601 UTC，例如 `2026-08-15T10:30:00Z`。
- 所有数据库 BIGINT、雪花 ID 和 UUID 在 JSON 中使用字符串。
- 创建成功返回 `201`，异步任务返回 `202`，无响应体删除返回 `204`。
- 每个请求携带或生成 `X-Request-Id`；响应和日志使用同一 requestId/traceId。
- 幂等创建或确认使用 `Idempotency-Key`；同一键和不同请求摘要返回 `409`。

成功响应：

```json
{
  "data": {},
  "requestId": "01J..."
}
```

错误响应：

```json
{
  "error": {
    "code": "VIDEO_NOT_FOUND",
    "message": "视频不存在或不可访问",
    "details": []
  },
  "requestId": "01J..."
}
```

游标列表：

```json
{
  "data": {
    "items": [],
    "nextCursor": null,
    "hasMore": false
  },
  "requestId": "01J..."
}
```

## 外部资源归属

| 路径组 | 所有者 | 说明 |
|---|---|---|
| `/api/v1/auth/**` | identity-service | 用户登录、刷新、登出 |
| `/api/v1/users/**` | user-service | 资料与创作者信息 |
| `/api/v1/videos/**` | video-service | 视频元数据、详情、创作者管理 |
| `/api/v1/media/uploads/**` | media-service | 分片上传和处理状态 |
| `/api/v1/playback/**` | playback-service | 播放清单、会话和进度 |
| `/api/v1/danmaku/**` | danmaku-service | 弹幕查询、写入和连接 |
| `/api/v1/comments/**` | comment-service | 评论和回复 |
| `/api/v1/engagement/**` | engagement-service | 点赞、收藏和关注 |
| `/api/v1/feed/**` | feed-service | 首页和关注流 |
| `/api/v1/search/**` | search-service | 搜索 |
| `/api/v1/pets/**` | pet-service | 宠物状态、偏好和资产读取 |
| `/api/v1/agent/**` | agent-service | 会话、SSE、记忆、工具提案 |
| `/api/v1/reports/**` | moderation-service | 用户举报入口 |
| `/api/v1/notifications/**` | notification-service | 通知与偏好 |
| `/admin-api/v1/**` | admin-service | 独立管理 BFF，不透传领域 Cookie |

## Agent SSE

流地址规划为 `GET /api/v1/agent/conversations/{conversationId}/events`，请求使用 `Accept: text/event-stream`。每个帧使用事件名和单调递增序号：

```text
id: 42
event: message.delta
data: {"streamId":"...","conversationId":"...","sequence":42,"occurredAt":"...","payload":{}}
```

事件名固定为：`message.delta`、`pet.state`、`tool.started`、`tool.completed`、`action.proposed`、`memory.updated`、`usage.completed`、`error`。客户端用 `Last-Event-ID` 恢复；服务端只重放当前活动流的短期缓冲，完整历史从消息查询接口读取。

`action.proposed` 必须包含 `proposalId`、`actionType`、`targetId`、`argumentDigest`、`impactSummary`、`expiresAt` 和可展示参数；确认接口必须携带 `Idempotency-Key`。提案默认五分钟过期、一次性使用，并绑定用户、会话、动作和参数摘要。

## 内部接口

- Dubbo Triple 使用 Protobuf；包名按 `streamora.<domain>.v1`。
- 请求必须携带调用主体、traceId、deadline；管理写请求还携带管理员 ID、权限、原因和复验令牌引用。
- 不在 Protobuf 中暴露数据库 Entity、表名或 ORM 类型。
- 只读调用可按 deadline 做有限重试；写调用只有带幂等键且服务端支持去重时才能重试。

