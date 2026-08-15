CREATE TABLE video.video (
    id VARCHAR(64) PRIMARY KEY,
    owner_subject_id VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    cover_url VARCHAR(2048) NOT NULL,
    duration_seconds INTEGER NOT NULL,
    creator_display_name VARCHAR(120) NOT NULL,
    creator_avatar_url VARCHAR(2048) NOT NULL,
    category_id VARCHAR(32) NOT NULL,
    category_label VARCHAR(64) NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    visibility VARCHAR(24) NOT NULL,
    lifecycle_status VARCHAR(24) NOT NULL,
    media_asset_id VARCHAR(64),
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (duration_seconds >= 0),
    CHECK (view_count >= 0),
    CHECK (visibility IN ('PUBLIC', 'PRIVATE', 'UNLISTED')),
    CHECK (lifecycle_status IN ('DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED'))
);

CREATE INDEX idx_video_public_feed ON video.video
    (visibility, lifecycle_status, category_id, published_at DESC, id DESC);

CREATE TABLE video.video_tag (
    video_id VARCHAR(64) NOT NULL,
    tag VARCHAR(64) NOT NULL,
    PRIMARY KEY (video_id, tag),
    CONSTRAINT fk_video_tag_video FOREIGN KEY (video_id) REFERENCES video.video (id)
);

INSERT INTO video.video (id, owner_subject_id, title, description, cover_url, duration_seconds,
                         creator_display_name, creator_avatar_url, category_id, category_label, view_count,
                         visibility, lifecycle_status, media_asset_id, published_at, created_at, updated_at)
VALUES
('city-pet-journey', 'streamora-studio', '和宠物一起探索城市的安静角落', '这是一段用于 Streamora 封闭演示的公开视频内容。真实的视频信息会在上传、审核与发布链路完整接入后由视频服务维护。', 'https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?auto=format&fit=crop&w=1200&q=85', 584, 'Streamora Studio', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=160&q=80', 'life', '生活', 12840, 'PUBLIC', 'PUBLISHED', 'asset-city-pet-journey', TIMESTAMP WITH TIME ZONE '2026-08-14 09:30:00+00', TIMESTAMP WITH TIME ZONE '2026-08-14 09:30:00+00', TIMESTAMP WITH TIME ZONE '2026-08-14 09:30:00+00'),
('morning-breakfast', 'morning-kitchen', '三分钟做一份会让人慢下来的治愈早餐', '一份轻松的早餐记录，适合在忙碌前让节奏慢下来。', 'https://images.unsplash.com/photo-1498837167922-ddd27525d352?auto=format&fit=crop&w=1200&q=85', 206, '清晨厨房', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=160&q=80', 'life', '生活', 9650, 'PUBLIC', 'PUBLISHED', 'asset-morning-breakfast', TIMESTAMP WITH TIME ZONE '2026-08-14 02:15:00+00', TIMESTAMP WITH TIME ZONE '2026-08-14 02:15:00+00', TIMESTAMP WITH TIME ZONE '2026-08-14 02:15:00+00'),
('coast-diary', 'coast-diary', '第一次带它去看海，风比想象中温柔', '一次关于海风、散步和陪伴的影像日记。', 'https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=1200&q=85', 424, '海边日记', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=160&q=80', 'pets', '萌宠', 23890, 'PUBLIC', 'PUBLISHED', 'asset-coast-diary', TIMESTAMP WITH TIME ZONE '2026-08-13 11:40:00+00', TIMESTAMP WITH TIME ZONE '2026-08-13 11:40:00+00', TIMESTAMP WITH TIME ZONE '2026-08-13 11:40:00+00'),
('desk-setup', 'streamora-studio', '一个让创作更专注的桌面整理流程', '从杂乱到专注的桌面整理记录。', 'https://images.unsplash.com/photo-1526498460520-4c246339dccb?auto=format&fit=crop&w=1200&q=85', 762, 'Streamora Studio', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=160&q=80', 'knowledge', '知识', 7420, 'PUBLIC', 'PUBLISHED', 'asset-desk-setup', TIMESTAMP WITH TIME ZONE '2026-08-13 05:20:00+00', TIMESTAMP WITH TIME ZONE '2026-08-13 05:20:00+00', TIMESTAMP WITH TIME ZONE '2026-08-13 05:20:00+00'),
('indie-game-night', 'pixel-signal', '独立游戏夜：用一小时走进一座会呼吸的小城', '一场独立游戏的夜间体验与拆解。', 'https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1200&q=85', 3942, '像素讯号', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=160&q=80', 'games', '游戏', 31560, 'PUBLIC', 'PUBLISHED', 'asset-indie-game-night', TIMESTAMP WITH TIME ZONE '2026-08-12 15:05:00+00', TIMESTAMP WITH TIME ZONE '2026-08-12 15:05:00+00', TIMESTAMP WITH TIME ZONE '2026-08-12 15:05:00+00'),
('forest-soundscape', 'field-notes', '雨后森林的声音采集：一段给耳朵的散步', '收录雨后森林的自然声响。', 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=1200&q=85', 918, '原野笔记', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=160&q=80', 'knowledge', '知识', 11820, 'PUBLIC', 'PUBLISHED', 'asset-forest-soundscape', TIMESTAMP WITH TIME ZONE '2026-08-12 07:25:00+00', TIMESTAMP WITH TIME ZONE '2026-08-12 07:25:00+00', TIMESTAMP WITH TIME ZONE '2026-08-12 07:25:00+00');

INSERT INTO video.video_tag (video_id, tag) VALUES
('city-pet-journey', '生活'), ('city-pet-journey', 'AI 陪伴'), ('city-pet-journey', '创作日常'),
('morning-breakfast', '生活'), ('morning-breakfast', '早餐'),
('coast-diary', '萌宠'), ('coast-diary', '旅行'),
('desk-setup', '知识'), ('desk-setup', '创作'),
('indie-game-night', '游戏'), ('indie-game-night', '独立游戏'),
('forest-soundscape', '知识'), ('forest-soundscape', '自然声音');
