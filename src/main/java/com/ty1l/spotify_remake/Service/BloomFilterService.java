package com.ty1l.spotify_remake.Service;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 布隆过滤器服务
 *
 * 封装 Redisson RBloomFilter，使用 RedisBloom 模块原生命令。
 * 分为两类过滤器：
 * 1. 数据存在性过滤器（song/artist/album ID）— 预加载，启动时从 DB 全量填充
 * 2. 空结果标记过滤器（search/lyrics 关键词）— 运行时积累，搜索返回空时标记
 */
@Service
public class BloomFilterService {

    private static final Logger log = LoggerFactory.getLogger(BloomFilterService.class);

    private static final double FALSE_PROBABILITY = 0.01;

    // 数据存在性过滤器 Key
    private static final String BLOOM_SONG = "spotify:bloom:song";
    private static final String BLOOM_ARTIST = "spotify:bloom:artist";
    private static final String BLOOM_ALBUM = "spotify:bloom:album";

    // 空结果标记过滤器 Key
    private static final String BLOOM_SEARCH_EMPTY = "spotify:bloom:search:empty";
    private static final String BLOOM_ARTIST_SEARCH_EMPTY = "spotify:bloom:artist-search:empty";
    private static final String BLOOM_LYRICS_EMPTY = "spotify:bloom:lyrics:empty";

    private final RedissonClient redisson;

    public BloomFilterService(RedissonClient redissonClient) {
        this.redisson = redissonClient;
    }

    // ── 数据存在性 ────────────────────────────────────────────────

    public boolean songExists(Integer id) {
        return getOrCreate(BLOOM_SONG, 10_000).contains(String.valueOf(id));
    }

    public boolean artistExists(Integer id) {
        return getOrCreate(BLOOM_ARTIST, 1_000).contains(String.valueOf(id));
    }

    public boolean albumExists(Long id) {
        return getOrCreate(BLOOM_ALBUM, 5_000).contains(String.valueOf(id));
    }

    public void addSong(Integer id) {
        getOrCreate(BLOOM_SONG, 10_000).add(String.valueOf(id));
    }

    public void addArtist(Integer id) {
        getOrCreate(BLOOM_ARTIST, 1_000).add(String.valueOf(id));
    }

    public void addAlbum(Long id) {
        getOrCreate(BLOOM_ALBUM, 5_000).add(String.valueOf(id));
    }

    /** 批量添加歌曲 ID（启动预加载用，减少网络往返） */
    public void addSongBatch(int[] ids) {
        if (ids == null || ids.length == 0) return;
        RBloomFilter<String> filter = getOrCreate(BLOOM_SONG, 10_000);
        for (int id : ids) {
            filter.add(String.valueOf(id));
        }
    }

    /** 批量添加艺术家 ID */
    public void addArtistBatch(int[] ids) {
        if (ids == null || ids.length == 0) return;
        RBloomFilter<String> filter = getOrCreate(BLOOM_ARTIST, 1_000);
        for (int id : ids) {
            filter.add(String.valueOf(id));
        }
    }

    /** 批量添加专辑 ID */
    public void addAlbumBatch(long[] ids) {
        if (ids == null || ids.length == 0) return;
        RBloomFilter<String> filter = getOrCreate(BLOOM_ALBUM, 5_000);
        for (long id : ids) {
            filter.add(String.valueOf(id));
        }
    }

    // ── 空结果标记 ────────────────────────────────────────────────

    public boolean isKnownEmptySearch(String keyword) {
        return getOrCreate(BLOOM_SEARCH_EMPTY, 100_000).contains(keyword);
    }

    public boolean isKnownEmptyArtistSearch(String name) {
        return getOrCreate(BLOOM_ARTIST_SEARCH_EMPTY, 50_000).contains(name);
    }

    public boolean isKnownEmptyLyrics(String title) {
        return getOrCreate(BLOOM_LYRICS_EMPTY, 50_000).contains(title);
    }

    public void markEmptySearch(String keyword) {
        getOrCreate(BLOOM_SEARCH_EMPTY, 100_000).add(keyword);
    }

    public void markEmptyArtistSearch(String name) {
        getOrCreate(BLOOM_ARTIST_SEARCH_EMPTY, 50_000).add(name);
    }

    public void markEmptyLyrics(String title) {
        getOrCreate(BLOOM_LYRICS_EMPTY, 50_000).add(title);
    }

    // ── 内部方法 ──────────────────────────────────────────────────

    /**
     * 懒初始化：首次访问时 tryInit，key 已存在则复用（跳过初始化）。
     * tryInit 在 key 已存在时返回 false，不会覆盖已有过滤器。
     */
    private RBloomFilter<String> getOrCreate(String key, long expectedInsertions) {
        RBloomFilter<String> filter = redisson.getBloomFilter(key);
        if (!filter.isExists()) {
            filter.tryInit(expectedInsertions, FALSE_PROBABILITY);
            log.info("Bloom filter created: key={}, expectedInsertions={}", key, expectedInsertions);
        }
        return filter;
    }

    /** 判断布隆过滤器是否已初始化（供 Initializer 使用） */
    public boolean isBloomFilterExists(String key) {
        return redisson.getBloomFilter(key).isExists();
    }
}
