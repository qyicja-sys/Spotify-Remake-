package com.ty1l.spotify_remake.Entity.Public;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 歌曲播放流水表 — 异步写入 MySQL 留作流水和持久化备份
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongPlayLog {
    private Long id;
    private Integer songId;
    private Long playCount;
    private LocalDate playDate;
    private LocalDateTime createdAt;
}
