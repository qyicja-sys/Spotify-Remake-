package com.ty1l.spotify_remake.Entity.Public;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 排行榜缓存对象 — 使用逻辑过期（无物理 TTL）
 *
 * 物理 TTL = -1（永不过期），逻辑过期时间存储在 expireTime 字段。
 * 读取时自行判断是否逻辑过期：过期则返回旧数据 + 异步刷新。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardCache {
    /** 排行榜歌曲列表（已填充歌名、艺人名、封面等） */
    private List<LeaderboardSongVO> songs;
    /** 逻辑过期时间戳（毫秒），例如次日 00:00:00 的毫秒值 */
    private long expireTime;
    /** 缓存创建/刷新时间 */
    private long refreshTime;
}
