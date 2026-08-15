# 阶段 3 验收

- 状态：进行中
- 更新日期：2026-08-16
- 目标：实现分片上传、转码、审核、发布、HLS 播放和媒体任务管理的可演示闭环。

## 交付边界

| 子链路 | 所有者 | 阶段 3 交付 |
|---|---|---|
| 上传会话与原始资产 | `media-service` | 创建、补传、完成和取消分片上传；登记资产与处理任务 |
| 转码执行 | `transcode-worker` | 领取媒体任务、调用 FFmpeg、回报 HLS、封面和字幕产物；不持有业务数据库 |
| 视频生命周期 | `video-service` | 草稿、待审核、已发布和拒绝状态；只保存媒体逻辑引用 |
| 内容审核 | `moderation-service` | 处理视频审核决定并发布决定事件；不得直接写视频表 |
| 播放授权 | `playback-service` | 对公开视频返回限时 HLS 清单和字幕轨道；记录播放会话与进度 |
| 管理媒体任务 | `admin-service` | 以 RBAC BFF 方式读取任务和审核操作结果；不跨库写入 |

## 契约与事件基线

- 外部视频读取契约已预置在 `packages/openapi/streamora-v1.yaml`：首页内容流、视频详情和播放清单。
- 上传、视频生命周期、审核和媒体管理接口必须先补充 OpenAPI 后实现。
- 跨服务事件采用标准信封，并至少定义 `media.upload.completed.v1`、`media.asset.transcoded.v1`、`moderation.video.decided.v1`、`video.published.v1`。
- `media-service` 是 `media_asset`、`multipart_upload`、`transcode_job`、字幕和语义产物的唯一写入方；`video-service` 是视频元数据和可见性的唯一写入方。

## 开源方案决策

| 需求 | 候选与证据 | 结论 |
|---|---|---|
| S3 兼容对象存储客户端 | [MinIO Java SDK](https://github.com/minio/minio-java) 支持 S3 兼容对象存储且为 Apache-2.0；[AWS SDK for Java 2.x](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_s3_code_examples.html) 提供分片上传、预签名 URL 和异步客户端 | 采用 AWS SDK for Java 2.x 的 S3 API，通过 `ObjectStore` 端口隔离，兼容 SeaweedFS、Ceph RGW 和云 S3 |
| 自托管对象存储 | [SeaweedFS](https://github.com/seaweedfs/seaweedfs) 为 Apache-2.0 并提供 S3 API；[Ceph RGW](https://docs.ceph.com/en/latest/radosgw/index.html) 提供 S3 兼容网关；现有 MinIO 社区服务端为 AGPLv3 且仓库已归档 | 容器联调前将 MinIO 替换为 SeaweedFS；Ceph 作为生产规模化候选。MinIO 不纳入默认封闭演示运行栈 |
| HLS 转码 | [FFmpeg](https://ffmpeg.org/documentation.html) 提供媒体处理和 HLS muxer；[Jaffree](https://github.com/kokorin/Jaffree) 是 Apache-2.0 的 Java FFmpeg 命令包装器 | 采用 FFmpeg，Worker 通过项目 `TranscodeExecutor` 端口调用。正式镜像使用 LGPL 兼容构建，禁止引入 GPL 编码库；Jaffree 作为命令编排候选，先在 Worker PoC 验证后决定是否引入 |

隔离方式：业务层不依赖 AWS、SeaweedFS 或 FFmpeg 类型；对象存储和转码均通过基础设施适配器接入。当前主机无 Docker，S3/FFmpeg 适配器的真实集成验证待容器环境可用后补充。

## 验证证据

| 检查项 | 实际执行 | 结果 |
|---|---|---|
| 开源方案调研 | 调研对象存储客户端、S3 兼容对象存储与 HLS 转码候选的官方文档、源仓库与许可证 | AWS SDK for Java 2.x、SeaweedFS 与 FFmpeg 满足当前基线；MinIO 社区服务端因 AGPLv3 和归档状态不作为默认运行栈 |
| 上传 HTTP 契约 | 为创建上传会话和完成上传新增 OpenAPI `operationId`、用户会话安全声明、幂等键、输入校验与响应模型 | 通过 |
| OpenAPI 校验 | `pnpm contract:lint` | 通过；保留既有 `CursorPage` 未使用警告 |
| 类型生成 | `pnpm contract:generate` | 通过；用户端和管理端可使用新的生成类型 |
| media-service 上传主链路 | `mvn "-Dtest=MediaUploadFlowIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" -pl services/media-service -am test` | 通过；H2 PostgreSQL 兼容模式完成 Flyway V1、重复创建、重复完成、唯一转码任务与单个 Outbox 事件验证 |

## 验收出口

| 条件 | 状态 | 证据 |
|---|---|---|
| OpenAPI、Protobuf、事件和数据边界一致 | 进行中 | 视频读取和分片上传/完成契约已定义；视频生命周期、审核与事件 Protobuf 待补齐 |
| 分片上传完成后创建可幂等的转码任务 | 部分达成 | media-service Flyway V1、上传会话、完成操作、唯一转码任务和 `media.upload.completed.v1` Outbox 已实现并通过服务内集成测试；真实 S3 上传与 RocketMQ 投递待补齐 |
| 转码产出 HLS、封面和字幕并回报媒体服务 | 未开始 | 待实现 Worker 和 FFmpeg 集成测试 |
| 审核通过后发布，用户端可取得 HLS 清单 | 未开始 | 待实现跨服务事件处理与端到端测试 |
| 管理端可以查看任务并执行受权限保护的审核操作 | 未开始 | 待实现管理 API 与前端联调 |

## 已知限制

- 当前设备未安装 Docker，不能进行 MinIO、PostgreSQL、RocketMQ、Nacos 和 FFmpeg 容器的真实跨进程联调。
- 封闭演示可先使用 H2 PostgreSQL 兼容模式和可替换的对象存储/转码适配器覆盖服务内集成测试；容器化联调必须在 Docker 可用后补充。

## 用户验收

- 结论：待确认
- 备注：仅当所有出口条件具有自动化证据后，本阶段才能标记为“待验收”。
