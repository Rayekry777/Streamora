# 后端服务边界

本文件是 17 个运行单元职责的唯一真源。服务之间共享契约，不共享 Entity、Repository、迁移或数据库访问凭据。

| 运行单元 | 唯一职责与权威状态 | 明确禁止 | 主要集成 |
|---|---|---|---|
| `gateway-service` | 外部入口、路由、限流、关联 ID、统一边缘安全 | 业务编排、领域数据、认证凭据存储 | HTTP 转发、Nacos、Sentinel |
| `admin-service` | 管理 BFF、RBAC、影响摘要、不可变审计、管理读模型 | 跨服务数据库写入、用户会话、领域规则复制 | 调用 identity 及全部受管领域服务 |
| `identity-service` | 账户、凭据、用户/管理员隔离会话、刷新令牌族、复验挑战 | 用户资料、业务角色页面逻辑、领域封禁决定 | 网关鉴权、admin RBAC 主体、user 档案 |
| `user-service` | 用户资料、隐私设置、创作者资料 | 密码、会话、关注关系、视频数据 | identity 主体、engagement 关系 |
| `video-service` | 视频元数据、版本、标签、可见性和发布状态 | 二进制上传、转码执行、播放会话、审核规则 | media 产物、moderation 决定、发布事件 |
| `media-service` | 分片上传、对象资产、处理任务、字幕、关键帧、语义提示 | 视频发布、审核决定、FFmpeg 进程执行 | MinIO、transcode worker、moderation、video |
| `transcode-worker` | 领取媒体任务并执行 FFmpeg、探测和产物回报 | 持有业务数据库、直接发布视频 | media RPC/事件、MinIO、FFmpeg |
| `playback-service` | 播放授权、HLS 清单、播放会话和观看进度 | 视频元数据编辑、媒体转码、互动计数 | video 可见性、media 产物 |
| `danmaku-service` | 弹幕写入、时间分片、实时分发和查询 | 评论树、视频审核、推荐排序 | playback 上下文、moderation 举报 |
| `comment-service` | 评论树、回复和评论计数 | 弹幕、点赞收藏、处罚执行 | video 可见性、moderation 举报 |
| `engagement-service` | 点赞、收藏、关注及其权威计数 | 用户资料、视频元数据、推荐算法 | user/video 逻辑引用、事件发布 |
| `feed-service` | 首页/关注流排序和本地只读投影 | 修改视频、用户或互动权威状态 | 消费发布、互动、关注事件 |
| `search-service` | 搜索文档、索引进度和查询 | 修改被索引领域数据 | 消费视频和用户公开信息事件 |
| `pet-service` | 宠物实例、偏好、资产版本、装扮库存和权威成长状态 | LLM 会话、AI 自行修改成长状态 | agent 读取、media 资产、admin 发布 |
| `agent-service` | 会话、消息、授权记忆、提示词、模型路由、工具提案和 AI 用量 | 直接修改其他领域数据、权威宠物成长、整段视频上传模型 | Qwen/Mock、pet、各领域工具、Redis/pgvector |
| `moderation-service` | 举报、审核案件、决定和处罚记录 | 直接删除领域数据、管理 RBAC | 调用内容所有者执行决定、发布审核事件 |
| `notification-service` | 站内通知、偏好、投递尝试和提醒 | 业务决定、外部营销自动化 | 消费领域事件、Agent 提醒工具 |

## 管理端调用规则

- `/admin-api/v1/**` 由网关路由至 `admin-service`。
- `admin-service` 校验管理会话、RBAC、复验令牌和原因，然后调用数据所有者。
- 数据所有者再次校验动作上下文并执行写入，返回结果供 `admin-service` 写审计。
- 审计记录只允许追加；业务 API 不提供更新和删除审计接口。

## 边界变更规则

转移职责时必须同时更新本文件、`docs/data/DATA_OWNERSHIP.md`、`docs/development/BACKEND_DEVELOPMENT.md`、相关 API/事件契约和迁移计划，并为旧消费者保留明确兼容窗口。
