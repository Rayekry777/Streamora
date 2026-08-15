# 宠物反应事件

统一前端事件：

- `page.context.changed`
- `player.playback.changed`
- `player.position.changed`
- `subtitle.cue.changed`
- `danmaku.burst.detected`
- `user.engagement.changed`
- `video.semantic.cue.reached`

每个事件包含 `eventId`、`type`、`occurredAt`、`source`、`payload`。调度器按以下顺序处理：

1. 用户直接操作。
2. 播放器状态变化。
3. 视频语义时间点。
4. 空闲行为。

相同事件去重；高优先级可中断低优先级动作；文字反应使用独立冷却，默认活跃档约每 2–4 分钟一次，并允许用户降低或关闭。规则反应不等待网络，语义反应允许异步预取。

