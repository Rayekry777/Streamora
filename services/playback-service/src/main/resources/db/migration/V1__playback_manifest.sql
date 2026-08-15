CREATE TABLE playback.playback_manifest (
    video_id VARCHAR(64) PRIMARY KEY,
    manifest_url VARCHAR(2048) NOT NULL,
    poster_url VARCHAR(2048) NOT NULL,
    publication_state VARCHAR(24) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (publication_state IN ('PUBLIC', 'WITHDRAWN'))
);

CREATE TABLE playback.subtitle_track (
    video_id VARCHAR(64) NOT NULL,
    language VARCHAR(16) NOT NULL,
    label VARCHAR(64) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    PRIMARY KEY (video_id, language),
    CONSTRAINT fk_subtitle_manifest FOREIGN KEY (video_id) REFERENCES playback.playback_manifest (video_id)
);

INSERT INTO playback.playback_manifest (video_id, manifest_url, poster_url, publication_state, updated_at) VALUES
('city-pet-journey', 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8', 'https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?auto=format&fit=crop&w=1200&q=85', 'PUBLIC', TIMESTAMP WITH TIME ZONE '2026-08-14 09:30:00+00'),
('morning-breakfast', 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8', 'https://images.unsplash.com/photo-1498837167922-ddd27525d352?auto=format&fit=crop&w=1200&q=85', 'PUBLIC', TIMESTAMP WITH TIME ZONE '2026-08-14 02:15:00+00'),
('coast-diary', 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8', 'https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=1200&q=85', 'PUBLIC', TIMESTAMP WITH TIME ZONE '2026-08-13 11:40:00+00'),
('desk-setup', 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8', 'https://images.unsplash.com/photo-1526498460520-4c246339dccb?auto=format&fit=crop&w=1200&q=85', 'PUBLIC', TIMESTAMP WITH TIME ZONE '2026-08-13 05:20:00+00'),
('indie-game-night', 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8', 'https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1200&q=85', 'PUBLIC', TIMESTAMP WITH TIME ZONE '2026-08-12 15:05:00+00'),
('forest-soundscape', 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8', 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=1200&q=85', 'PUBLIC', TIMESTAMP WITH TIME ZONE '2026-08-12 07:25:00+00');
