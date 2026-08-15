# 数据归属与访问边界

## 权威数据规则

- 每类业务状态只有一个写入服务。
- 其他服务需要查询时，低频强一致查询走 Dubbo；高频查询通过领域事件建立本地只读投影。
- 本地投影可以重建，不得被当作权威状态回写源服务。
- 管理端通过领域服务执行管理动作，不获得其他 schema 凭据。
- 跨服务不声明数据库外键；删除和失效通过事件传播。

## 权威数据矩阵

| 数据 | 唯一写入方 | 允许的读取方式 | 禁止行为 |
|---|---|---|---|
| 凭据、用户会话、管理员会话 | identity-service | 鉴权 RPC、签名主体上下文 | 其他服务保存密码或刷新令牌 |
| 管理角色、权限、审计 | admin-service | 管理鉴权 RPC、只读审计 API | 修改或删除审计记录 |
| 用户与创作者资料 | user-service | RPC、公开资料事件 | engagement 修改用户资料 |
| 视频元数据和发布状态 | video-service | RPC、发布事件投影 | media 或 moderation 直接更新视频表 |
| 原始媒体、HLS、字幕、关键帧、语义提示 | media-service | 预签名 URL、RPC、产物事件 | video 保存对象存储凭据 |
| 播放会话和观看进度 | playback-service | 用户 API、聚合事件 | video 直接记录播放进度 |
| 弹幕 | danmaku-service | HTTP/WS、审核事件 | comment 混用弹幕表 |
| 评论 | comment-service | HTTP、评论事件 | moderation 直接删除评论行 |
| 点赞、收藏、关注 | engagement-service | HTTP/RPC、互动事件 | user 或 video 维护第二套关系真源 |
| 推荐流投影 | feed-service | Feed API | 反向修改视频和互动状态 |
| 搜索投影 | search-service | Search API | 反向修改被索引状态 |
| 宠物实例、成长、资产、库存 | pet-service | Pet API/RPC、资产事件 | Agent 自行增加经验或物品 |
| 会话、消息、记忆、提案、提示词、路由、AI 用量 | agent-service | Agent API、脱敏管理 API | pet 保存完整对话或模型密钥 |
| 举报、审核、处罚 | moderation-service | 管理 API/RPC、决定事件 | 直接跨库删除内容 |
| 通知和投递状态 | notification-service | Notification API | 业务服务维护第二份已读状态 |

## 缓存与对象存储

- Redis 不是权威数据库；缓存失效后必须能从所有者恢复。
- MinIO 对象由 `media-service` 登记和授权，其他服务只保存逻辑 assetId 或限时 URL。
- Agent 短期上下文可存 Redis；完整消息和授权长期记忆必须落在 agent schema。
- pgvector 向量行必须关联可删除的 `memory_item`，删除记忆时同步删除向量和缓存。

