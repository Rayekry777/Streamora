# 领域事件契约

## 事件信封

所有 RocketMQ 领域事件使用以下字段：

```json
{
  "eventId": "01J...",
  "eventType": "video.video.published.v1",
  "schemaVersion": 1,
  "aggregateId": "193...",
  "occurredAt": "2026-08-15T10:30:00Z",
  "traceId": "...",
  "producer": "video-service",
  "payload": {}
}
```

- 名称使用 `<domain>.<aggregate>.<past-action>.v<n>`。
- 生产端在同一数据库事务写业务状态和 `outbox_event`，独立发布器投递消息。
- 消费端先以 `eventId` 判断幂等，再在本地事务更新投影和消费记录。
- 消费者不得依赖消息严格有序；需要顺序时按 `aggregateId` 比较领域版本。
- 新增可选字段保持同版本；删除、改名、语义改变或类型改变发布新版本。
- 消费失败进入有界重试和死信队列，管理端提供查看、重放和审计能力。

## MVP 事件目录

| 事件 | 生产者 | 主要消费者 | 用途 |
|---|---|---|---|
| `media.upload.completed.v1` | media | media/transcode | 创建转码任务 |
| `media.transcode.completed.v1` | media | video、moderation、playback | 通知媒体产物可用 |
| `media.semantic-cues.generated.v1` | media | agent、pet | 视频时间码语义提示可用 |
| `moderation.case.decided.v1` | moderation | video、comment、danmaku、admin | 执行审核决定并更新读模型 |
| `video.video.published.v1` | video | feed、search、notification | 发布可发现内容 |
| `video.video.unpublished.v1` | video | feed、search、playback | 撤下内容和投影 |
| `comment.comment.created.v1` | comment | moderation、notification | 审核与回复通知 |
| `danmaku.message.created.v1` | danmaku | moderation | 异步风险检查 |
| `engagement.video-liked.v1` | engagement | feed、notification | 推荐信号和通知 |
| `engagement.video-favorited.v1` | engagement | feed | 推荐信号 |
| `engagement.user-followed.v1` | engagement | feed、notification | 关注流和通知 |
| `pet.asset.published.v1` | pet | admin、web 投影 | 宠物资产版本发布 |
| `agent.memory.updated.v1` | agent | admin 成本/安全读模型 | 用户授权记忆变更审计 |
| `notification.delivery-requested.v1` | 领域服务 | notification | 创建站内通知或提醒 |

`transcode-worker` 的任务领取优先使用媒体服务的租约式 RPC；RocketMQ 只通知“有任务可领取”，避免消息重复直接导致同一对象被并发转码。

