package com.ty1l.spotify_remake.Service;

import com.ty1l.spotify_remake.Mapper.Public.AlbumMapper;
import com.ty1l.spotify_remake.Mapper.Public.ArtistMapper;
import com.ty1l.spotify_remake.Mapper.Public.SongMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 布隆过滤器启动预加载器
 *
 * 应用启动时从 DB 全量加载 Song/Artist/Album ID 到 RedisBloom 布隆过滤器。
 */
@Component
public class BloomFilterInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BloomFilterInitializer.class);

    private final BloomFilterService bloomFilterService;
    private final SongMapper songMapper;
    private final ArtistMapper artistMapper;
    private final AlbumMapper albumMapper;

    public BloomFilterInitializer(BloomFilterService bloomFilterService,
                                  SongMapper songMapper,
                                  ArtistMapper artistMapper,
                                  AlbumMapper albumMapper) {
        this.bloomFilterService = bloomFilterService;
        this.songMapper = songMapper;
        this.artistMapper = artistMapper;
        this.albumMapper = albumMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        loadSongIds();
        loadArtistIds();
        loadAlbumIds();
    }

    private void loadSongIds() {
        long start = System.currentTimeMillis();
        List<Integer> ids = songMapper.findAllIds();
        if (ids == null || ids.isEmpty()) {
            log.warn("No song IDs found in DB, skipping bloom filter load");
            return;
        }
        int[] arr = ids.stream().mapToInt(Integer::intValue).toArray();
        bloomFilterService.addSongBatch(arr);
        log.info("Bloom filter loaded {} song IDs in {}ms", ids.size(), System.currentTimeMillis() - start);
    }

    private void loadArtistIds() {
        long start = System.currentTimeMillis();
        List<Integer> ids = artistMapper.findAllIds();
        if (ids == null || ids.isEmpty()) {
            log.warn("No artist IDs found in DB, skipping bloom filter load");
            return;
        }
        int[] arr = ids.stream().mapToInt(Integer::intValue).toArray();
        bloomFilterService.addArtistBatch(arr);
        log.info("Bloom filter loaded {} artist IDs in {}ms", ids.size(), System.currentTimeMillis() - start);
    }

    private void loadAlbumIds() {
        long start = System.currentTimeMillis();
        List<Long> ids = albumMapper.findAllIds();
        if (ids == null || ids.isEmpty()) {
            log.warn("No album IDs found in DB, skipping bloom filter load");
            return;
        }
        long[] arr = ids.stream().mapToLong(Long::longValue).toArray();
        bloomFilterService.addAlbumBatch(arr);
        log.info("Bloom filter loaded {} album IDs in {}ms", ids.size(), System.currentTimeMillis() - start);
    }
}
