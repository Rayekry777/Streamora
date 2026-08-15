# Streamora 数据库结构、归属与迁移规划

```yaml
version: 1
updatedAt: 2026-08-15
currentMigrationVersion: none
implementedTables: 0
status: 未实现
```

## 1. 当前结构

当前没有数据库、迁移或业务表。下文是阶段 1–7 的规划，不代表已实现结构。实际结构建立后以各服务 Flyway 历史为真源。

## 2. 隔离规则

- 本地开发可共享一个 PostgreSQL 集群，但每个有状态服务使用独立数据库用户和 schema。
- 服务只能读写自己的 schema；禁止跨 schema 查询、外键和事务。
- 跨服务 ID 仅作为逻辑引用保存，并通过 RPC 或事件校验和同步。
- 每个写模型表使用 BIGINT/雪花 ID 或 UUIDv7；对外统一序列化为字符串。
- 有异步发布的服务建立 `outbox_event`；有异步消费的服务建立 `consumed_event` 或等价幂等记录。

## 3. 计划 schema 与核心表

| 所有者 | schema | 计划核心表 | 隔离与说明 |
|---|---|---|---|
| admin-service | `admin` | `admin_role`、`admin_permission`、`admin_role_permission`、`admin_subject_role`、`admin_audit_log`、`admin_read_model_job` | 管理 RBAC、不可变审计、聚合任务 |
| identity-service | `identity` | `account`、`credential`、`user_session`、`admin_session`、`refresh_token_family`、`reauth_challenge` | 用户与管理员会话物理区分 |
| user-service | `user_profile` | `user_profile`、`user_setting`、`creator_profile` | 个人资料和隐私设置；不保存互动关系 |
| video-service | `video` | `video`、`video_revision`、`video_tag`、`publish_record` | 视频元数据、版本和发布状态 |
| media-service | `media` | `media_asset`、`multipart_upload`、`transcode_job`、`subtitle_track`、`semantic_cue` | 原始对象、处理任务与分析产物 |
| playback-service | `playback` | `playback_manifest`、`watch_progress`、`playback_session` | 播放授权、清单和进度 |
| danmaku-service | `danmaku` | `danmaku_message`、`danmaku_segment` | 弹幕写入与时间分片索引 |
| comment-service | `comment` | `comment`、`comment_counter` | 评论树与聚合计数 |
| engagement-service | `engagement` | `video_like`、`video_favorite`、`user_follow`、`engagement_counter` | 点赞、收藏、关注唯一写入方 |
| feed-service | `feed` | `feed_item`、`feed_cursor_state` | 推荐/关注流只读投影 |
| search-service | `search` | `search_document`、`index_checkpoint` | 搜索投影；后续可替换专用搜索引擎 |
| pet-service | `pet` | `pet_instance`、`pet_preference`、`pet_asset`、`pet_asset_version`、`pet_inventory` | 宠物权威状态与版本化资产 |
| agent-service | `agent` | `conversation`、`message`、`memory_item`、`memory_embedding`、`action_proposal`、`prompt_version`、`model_route`、`usage_record` | 会话、可撤销记忆、工具提案、成本 |
| moderation-service | `moderation` | `report`、`moderation_case`、`moderation_decision`、`sanction` | 举报、审核和处罚记录 |
| notification-service | `notification` | `notification`、`notification_preference`、`delivery_attempt` | 站内通知与投递状态 |

`gateway-service` 和 `transcode-worker` 无业务数据库；前者无状态路由，后者通过媒体服务领取和回写任务。基础设施元数据不计入业务表清单。

## 4. 初始化数据

阶段 2 通过迁移写入五个内置管理角色：`SUPER_ADMIN`、`CONTENT_MODERATOR`、`USER_OPERATOR`、`AI_OPERATOR`、`SYSTEM_OPERATOR`。不得在迁移中写入真实密码、API Key 或真实管理员凭据。

## 5. 迁移记录

| 迁移 | 所属服务 | 内容 | 状态 |
|---|---|---|---|
| 无 | - | 尚未创建 Flyway 迁移 | 未实现 |
