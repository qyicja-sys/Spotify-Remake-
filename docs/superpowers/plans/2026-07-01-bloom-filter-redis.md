# RedisBloom 布隆过滤器 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 集成 Redisson RBloomFilter 防止缓存击穿，保护 Song/Artist/Album ID 查询和外部搜索 API。

**Architecture:** RedissonClient 连接 RedisBloom 模块，BloomFilterService 封装 RBloomFilter 操作，BloomFilterInitializer 启动时预加载 ID。各 Service 在 Cache-Aside 流程前插入布隆过滤器检查。

**Tech Stack:** Spring Boot 3.2.0, Redisson 3.32.0, RedisBloom, MyBatis, Java 21

## Global Constraints

- Redis key 规范：`{业务}:{模块}:{key}`
- 布隆过滤器误判率统一 0.01
- Redisson 连接池大小 4，不与 Lettuce 竞争
- 数据新增时同步 BF.ADD，删除时不操作（布隆过滤器不支持删除）

---

### Task 1: 添加 Redisson 依赖

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 添加 redisson-spring-boot-starter 依赖**

在 `pom.xml` 的 `<!-- 连接池依赖 -->` 后面插入：

```xml
        <!-- Redisson（RedisBloom 布隆过滤器） -->
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
            <version>3.32.0</version>
        </dependency>
```

- [ ] **Step 2: 验证依赖可解析**

Run: `mvn dependency:resolve -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "feat: add Redisson dependency for RedisBloom integration"
```

---

### Task 2: 创建 RedissonConfig

**Files:**
- Create: `src/main/java/com/ty1l/spotify_remake/config/RedissonConfig.java`

**Interfaces:**
- Produces: `RedissonClient redissonClient()` — 供 BloomFilterService 注入

- [ ] **Step 1: 创建 RedissonConfig**

```java
package com.ty1l.spotify_remake.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置（与 Lettuce 共存，独立连接池）
 *
 * 用于 RedisBloom 布隆过滤器操作，连接池较小以节省资源。
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setConnectionPoolSize(4)
                .setConnectionMinimumIdleSize(1)
                .setConnectTimeout(3000)
                .setRetryAttempts(3);
        return Redisson.create(config);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/ty1l/spotify_remake/config/RedissonConfig.java
git commit -m "feat: add RedissonConfig for RedisBloom connection"
```

---

### Task 3: 创建 BloomFilterService

**Files:**
- Create: `src/main/java/com/ty1l/spotify_remake/Service/BloomFilterService.java`

**Interfaces:**
- Consumes: `RedissonClient redissonClient`（来自 Task 2）
- Produces:
  - `boolean songExists(Integer id)`
  - `boolean artistExists(Integer id)`
  - `boolean albumExists(Long id)`
  - `void addSong(Integer id)`
  - `void addArtist(Integer id)`
  - `void addAlbum(Long id)`
  - `void addSongBatch(int[] ids)`
  - `void addArtistBatch(int[] ids)`
  - `void addAlbumBatch(long[] ids)`
  - `boolean isKnownEmptySearch(String keyword)`
  - `boolean isKnownEmptyArtistSearch(String name)`
  - `boolean isKnownEmptyLyrics(String title)`
  - `void markEmptySearch(String keyword)`
  - `void markEmptyArtistSearch(String name)`
  - `void markEmptyLyrics(String title)`

- [ ] **Step 1: 创建 BloomFilterService**

```java
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
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/ty1l/spotify_remake/Service/BloomFilterService.java
git commit -m "feat: add BloomFilterService wrapping Redisson RBloomFilter"
```

---

### Task 4: Mapper 新增 findAllIds 方法

**Files:**
- Modify: `src/main/java/com/ty1l/spotify_remake/Mapper/Public/SongMapper.java` — 添加 `List<Integer> findAllIds();`
- Modify: `src/main/java/com/ty1l/spotify_remake/Mapper/Public/ArtistMapper.java` — 添加 `List<Integer> findAllIds();`
- Modify: `src/main/java/com/ty1l/spotify_remake/Mapper/Public/AlbumMapper.java` — 添加 `List<Long> findAllIds();`
- Modify: `src/main/resources/com/ty1l/spotify_remake/Mapper/SongMapper.xml` — 添加 SQL
- Modify: `src/main/resources/com/ty1l/spotify_remake/Mapper/ArtistMapper.xml` — 添加 SQL
- Modify: `src/main/resources/com/ty1l/spotify_remake/Mapper/AlbumMapper.xml` — 添加 SQL

**Interfaces:**
- Produces: `List<Integer> SongMapper.findAllIds()`, `List<Integer> ArtistMapper.findAllIds()`, `List<Long> AlbumMapper.findAllIds()`

- [ ] **Step 1: 修改 SongMapper.java — 添加 findAllIds**

在 `src/main/java/com/ty1l/spotify_remake/Mapper/Public/SongMapper.java` 末尾（接口结束 `}` 前）添加：

```java
    /**
     * 查询所有歌曲 ID（布隆过滤器启动预加载用）
     */
    List<Integer> findAllIds();
```

- [ ] **Step 2: 修改 SongMapper.xml — 添加 SQL**

在 `src/main/resources/com/ty1l/spotify_remake/Mapper/SongMapper.xml` 的 `</mapper>` 前添加：

```xml
    <select id="findAllIds" resultType="java.lang.Integer">
        SELECT id FROM songs
    </select>
```

- [ ] **Step 3: 修改 ArtistMapper.java — 添加 findAllIds**

在 `src/main/java/com/ty1l/spotify_remake/Mapper/Public/ArtistMapper.java` 末尾添加：

```java
    /**
     * 查询所有艺术家 ID（布隆过滤器启动预加载用）
     */
    List<Integer> findAllIds();
```

- [ ] **Step 4: 修改 ArtistMapper.xml — 添加 SQL**

在 `src/main/resources/com/ty1l/spotify_remake/Mapper/ArtistMapper.xml` 的 `</mapper>` 前添加：

```xml
    <select id="findAllIds" resultType="java.lang.Integer">
        SELECT id FROM artists
    </select>
```

- [ ] **Step 5: 修改 AlbumMapper.java — 添加 findAllIds**

在 `src/main/java/com/ty1l/spotify_remake/Mapper/Public/AlbumMapper.java` 末尾添加：

```java
    /**
     * 查询所有专辑 ID（布隆过滤器启动预加载用）
     */
    List<Long> findAllIds();
```

- [ ] **Step 6: 修改 AlbumMapper.xml — 添加 SQL**

在 `src/main/resources/com/ty1l/spotify_remake/Mapper/AlbumMapper.xml` 的 `</mapper>` 前添加：

```xml
    <select id="findAllIds" resultType="java.lang.Long">
        SELECT id FROM albums
    </select>
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/ty1l/spotify_remake/Mapper/Public/SongMapper.java \
        src/main/java/com/ty1l/spotify_remake/Mapper/Public/ArtistMapper.java \
        src/main/java/com/ty1l/spotify_remake/Mapper/Public/AlbumMapper.java \
        src/main/resources/com/ty1l/spotify_remake/Mapper/SongMapper.xml \
        src/main/resources/com/ty1l/spotify_remake/Mapper/ArtistMapper.xml \
        src/main/resources/com/ty1l/spotify_remake/Mapper/AlbumMapper.xml
git commit -m "feat: add findAllIds to Song/Artist/Album mappers for bloom filter preload"
```

---

### Task 5: 创建 BloomFilterInitializer

**Files:**
- Create: `src/main/java/com/ty1l/spotify_remake/Service/BloomFilterInitializer.java`

**Interfaces:**
- Consumes: `BloomFilterService`（Task 3）, `SongMapper.findAllIds()`, `ArtistMapper.findAllIds()`, `AlbumMapper.findAllIds()`（Task 4）
- Produces: 无（ApplicationRunner）

- [ ] **Step 1: 创建 BloomFilterInitializer**

```java
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
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/ty1l/spotify_remake/Service/BloomFilterInitializer.java
git commit -m "feat: add BloomFilterInitializer for startup ID preloading"
```

---

### Task 6: 修改 SongServiceiml — 添加布隆过滤器检查

**Files:**
- Modify: `src/main/java/com/ty1l/spotify_remake/Service/Public/SongServiceiml.java`

**Interfaces:**
- Consumes: `BloomFilterService.songExists()` / `BloomFilterService.addSong()`

- [ ] **Step 1: 注入 BloomFilterService 并修改 findById**

在 `SongServiceiml` 中注入 `BloomFilterService`，修改 `findById` 方法。

插入字段声明（`@Autowired private CacheService cacheService;` 之后）：

```java
    @Autowired
    private BloomFilterService bloomFilterService;
```

修改 `findById` 方法：

```java
    @Override
    public Song findById(Integer id) {
        // 布隆过滤器：id 绝对不存在则直接返回 null
        if (!bloomFilterService.songExists(id)) {
            log.debug("Bloom filter rejected song id={}", id);
            return null;
        }
        String key = String.format(CacheService.KEY_SONG, id);
        Song cached = cacheService.get(key, Song.class);
        if (cached != null) {
            log.debug("Song cache hit for id={}", id);
            return cached;
        }
        Song song = songMapper.findById(id);
        if (song != null) {
            cacheService.set(key, song);
        }
        return song;
    }
```

- [ ] **Step 2: 修改 add 方法 — 新增歌曲同步布隆过滤器**

```java
    @Override
    public void add(Song song) {
        songMapper.insert(song);
        // 新增歌曲后同步到布隆过滤器，使 ID 立即可查
        if (song.getId() != null) {
            bloomFilterService.addSong(song.getId());
        }
    }
```

- [ ] **Step 3: 添加 import**

在文件顶部 import 区域添加：

```java
import com.ty1l.spotify_remake.Service.BloomFilterService;
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/ty1l/spotify_remake/Service/Public/SongServiceiml.java
git commit -m "feat: add bloom filter check to SongServiceiml.findById"
```

---

### Task 7: 修改 ArtistServiceiml — 添加布隆过滤器检查

**Files:**
- Modify: `src/main/java/com/ty1l/spotify_remake/Service/Public/ArtistServiceiml.java`

**Interfaces:**
- Consumes: `BloomFilterService.artistExists()` / `BloomFilterService.addArtist()`

- [ ] **Step 1: 注入 BloomFilterService 并修改 getArtistDetail**

在 `ArtistServiceiml` 的 `@Autowired private CacheService cacheService;` 后添加：

```java
    @Autowired
    private BloomFilterService bloomFilterService;
```

修改 `getArtistDetail` 方法，在缓存读取前插入布隆过滤器检查：

```java
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getArtistDetail(Integer id) {
        // 布隆过滤器：id 绝对不存在则直接返回 null
        if (!bloomFilterService.artistExists(id)) {
            log.debug("Bloom filter rejected artist id={}", id);
            return null;
        }

        String cacheKey = String.format(CacheService.KEY_ARTIST, id);

        // 尝试从缓存读取
        Map<String, Object> cached = cacheService.get(cacheKey, Map.class);
        if (cached != null) {
            log.debug("Artist cache hit for id={}", id);
            return cached;
        }

        log.info("Artist cache miss for id={}, rebuilding...", id);

        Artist artist = artistMapper.findById(id);
        if (artist == null) return null;

        List<SearchSongVO> songs = songMapper.findByArtistIdWithNames(id);
        if (songs == null) songs = Collections.emptyList();

        // 月听众数 = max(Redis实时值, DB兜底值)，保证刚播放就可见
        int monthlyListeners = monthlyListenersService.getMonthlyListeners(id);

        Map<String, Object> detail = Map.of(
                "artist", artist,
                "songs", songs,
                "monthlyListeners", monthlyListeners
        );

        cacheService.set(cacheKey, detail);
        return detail;
    }
```

- [ ] **Step 2: 修改 add 方法 — 新增艺术家同步布隆过滤器**

```java
    @Override
    public void add(Artist artist) {
        artistMapper.insert(artist);
        if (artist.getId() != null) {
            bloomFilterService.addArtist(artist.getId());
        }
    }
```

- [ ] **Step 3: 添加 import**

```java
import com.ty1l.spotify_remake.Service.BloomFilterService;
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/ty1l/spotify_remake/Service/Public/ArtistServiceiml.java
git commit -m "feat: add bloom filter check to ArtistServiceiml.getArtistDetail"
```

---

### Task 8: 修改 AlbumServiceiml — 添加布隆过滤器检查

**Files:**
- Modify: `src/main/java/com/ty1l/spotify_remake/Service/Public/AlbumServiceiml.java`

**Interfaces:**
- Consumes: `BloomFilterService.albumExists()` / `BloomFilterService.addAlbum()`

- [ ] **Step 1: 注入 BloomFilterService 并修改 findById**

在 `@Autowired private CacheService cacheService;` 后添加：

```java
    @Autowired
    private BloomFilterService bloomFilterService;
```

修改 `findById` 方法：

```java
    @Override
    public Album findById(Long id) {
        // 布隆过滤器：id 绝对不存在则直接返回 null
        if (!bloomFilterService.albumExists(id)) {
            log.debug("Bloom filter rejected album id={}", id);
            return null;
        }
        String key = String.format(CacheService.KEY_ALBUM, id);
        Album cached = cacheService.get(key, Album.class);
        if (cached != null) {
            log.debug("Album cache hit for id={}", id);
            return cached;
        }
        Album album = albumMapper.findById(id);
        if (album != null) {
            cacheService.set(key, album);
        }
        return album;
    }
```

- [ ] **Step 2: findByArtistId 保持不变**

`findByArtistId` 的参数是 `artistId`，语义上不属于专辑 ID 布隆过滤器的保护范围（艺术家可能确实有 0 张专辑，不应被拦截）。保持原逻辑不变。

- [ ] **Step 3: 修改 add 方法 — 新增专辑同步布隆过滤器**

```java
    @Override
    public void add(Album album) {
        albumMapper.insert(album);
        if (album.getId() != null) {
            bloomFilterService.addAlbum(album.getId());
        }
    }
```

- [ ] **Step 4: 添加 import**

```java
import com.ty1l.spotify_remake.Service.BloomFilterService;
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ty1l/spotify_remake/Service/Public/AlbumServiceiml.java
git commit -m "feat: add bloom filter check to AlbumServiceiml.findById"
```

---

### Task 9: 修改 ExternalMusicServiceiml — 空结果标记

**Files:**
- Modify: `src/main/java/com/ty1l/spotify_remake/Service/Public/ExternalMusicServiceiml.java`

**Interfaces:**
- Consumes: `BloomFilterService.isKnownEmptySearch()`, `markEmptySearch()`, `isKnownEmptyArtistSearch()`, `markEmptyArtistSearch()`

- [ ] **Step 1: 注入 BloomFilterService**

在 `@Autowired private CacheService cacheService;` 后添加：

```java
    @Autowired
    private BloomFilterService bloomFilterService;
```

- [ ] **Step 2: 修改 search 方法**

```java
    @Override
    public List<ExternalTrackVO> search(String keyword) {
        String normalized = keyword.trim().replaceAll("\\s+", " ");
        // 布隆过滤器：已知无结果的关键词直接返回空列表
        if (bloomFilterService.isKnownEmptySearch(normalized)) {
            log.debug("Bloom filter rejected empty search keyword={}", normalized);
            return Collections.emptyList();
        }

        String cacheKey = String.format(CacheService.KEY_EXTERNAL_SEARCH, normalized);

        List<ExternalTrackVO> cached = cacheService.getList(cacheKey, ExternalTrackVO.class);
        if (cached != null) {
            log.debug("External search cache hit for keyword={}", normalized);
            return cached;
        }

        List<ExternalTrackVO> results = gdMusicApiClient.search(keyword);

        if (results == null || results.isEmpty()) {
            bloomFilterService.markEmptySearch(normalized);
            return Collections.emptyList();
        }

        persistArtists(results);
        syncLyricsToLocal(results);
        cacheService.set(cacheKey, results, CacheService.EXTERNAL_SEARCH_TTL);
        return results;
    }
```

- [ ] **Step 3: 修改 searchByArtist 方法**

```java
    @Override
    public List<ExternalTrackVO> searchByArtist(String artistName) {
        String normalized = artistName.trim().replaceAll("\\s+", " ");
        // 布隆过滤器：已知无结果的艺人名直接返回空列表
        if (bloomFilterService.isKnownEmptyArtistSearch(normalized)) {
            log.debug("Bloom filter rejected empty artist search name={}", normalized);
            return Collections.emptyList();
        }

        String cacheKey = String.format(CacheService.KEY_EXTERNAL_ARTIST, normalized);

        List<ExternalTrackVO> cached = cacheService.getList(cacheKey, ExternalTrackVO.class);
        if (cached != null) {
            log.debug("External artist search cache hit for artistName={}", normalized);
            return cached;
        }

        List<ExternalTrackVO> results = gdMusicApiClient.searchByArtist(artistName);

        if (results == null || results.isEmpty()) {
            bloomFilterService.markEmptyArtistSearch(normalized);
            return Collections.emptyList();
        }

        persistArtists(results);
        syncLyricsToLocal(results);
        cacheService.set(cacheKey, results, CacheService.EXTERNAL_SEARCH_TTL);
        return results;
    }
```

- [ ] **Step 4: 添加 imports**

```java
import java.util.Collections;
import com.ty1l.spotify_remake.Service.BloomFilterService;
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ty1l/spotify_remake/Service/Public/ExternalMusicServiceiml.java
git commit -m "feat: add bloom filter empty-result marking to ExternalMusicService"
```

---

### Task 10: 修改 LyricsServiceiml — 空结果标记

**Files:**
- Modify: `src/main/java/com/ty1l/spotify_remake/Service/Public/LyricsServiceiml.java`

**Interfaces:**
- Consumes: `BloomFilterService.isKnownEmptyLyrics()`, `markEmptyLyrics()`

- [ ] **Step 1: 注入 BloomFilterService**

在 `@Autowired private CacheService cacheService;` 后添加：

```java
    @Autowired
    private BloomFilterService bloomFilterService;
```

- [ ] **Step 2: 修改 getLocalLyrics 方法**

```java
    @Override
    public Map<String, String> getLocalLyrics(String title) {
        // title 作为缓存 key（标准化空格）
        String normalized = title.trim().replaceAll("\\s+", " ");
        // 布隆过滤器：已知无歌词的标题直接返回 null
        if (bloomFilterService.isKnownEmptyLyrics(normalized)) {
            log.debug("Bloom filter rejected empty lyrics title={}", normalized);
            return null;
        }

        String cacheKey = String.format(CacheService.KEY_LYRICS_LOCAL, normalized);

        @SuppressWarnings("unchecked")
        Map<String, String> cached = cacheService.get(cacheKey, Map.class);
        if (cached != null) {
            log.debug("Local lyrics cache hit for title={}", normalized);
            return cached.isEmpty() ? null : cached;
        }

        List<Song> songs = songMapper.findByTitle(title);
        if (songs.isEmpty()) {
            bloomFilterService.markEmptyLyrics(normalized);
            cacheService.set(cacheKey, Map.of(), 600);
            return null;
        }

        // 取第一个匹配的歌曲
        Song song = songs.get(0);
        String lyricsPath = song.getLyrics();
        if (lyricsPath == null || lyricsPath.isEmpty()) {
            bloomFilterService.markEmptyLyrics(normalized);
            cacheService.set(cacheKey, Map.of(), 600);
            return null;
        }

        String content = null;

        // 如果 lyrics 字段存储的是 .lrc 文件路径，从磁盘读取
        if (lyricsPath.endsWith(".lrc")) {
            content = readLrcFile(lyricsPath);
        } else {
            // 否则当作直接存储的 LRC 歌词内容
            content = lyricsPath;
        }

        if (content == null || content.isEmpty()) {
            bloomFilterService.markEmptyLyrics(normalized);
            cacheService.set(cacheKey, Map.of(), 600);
            return null;
        }

        Map<String, String> result = new HashMap<>();
        result.put("lyric", content);
        cacheService.set(cacheKey, result, CacheService.LYRICS_TTL);
        return result;
    }
```

- [ ] **Step 3: 修改 getExternalLyrics 方法**

```java
    @Override
    public Map<String, String> getExternalLyrics(String source, String lyricId) {
        String cacheKey = String.format(CacheService.KEY_LYRICS_EXTERNAL, source, lyricId);
        // 布隆过滤器：外部歌词用 (source:lyricId) 合成的 key 标记空结果
        String emptyMarker = source + ":" + lyricId;
        if (bloomFilterService.isKnownEmptyLyrics(emptyMarker)) {
            log.debug("Bloom filter rejected empty external lyrics {}:{}", source, lyricId);
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> cached = cacheService.get(cacheKey, Map.class);
        if (cached != null) {
            log.debug("External lyrics cache hit for source={} lyricId={}", source, lyricId);
            return cached.isEmpty() ? null : cached;
        }

        Map<String, String> lyricsMap = externalMusicService.getLyrics(source, lyricId);
        if (lyricsMap == null || lyricsMap.isEmpty()) {
            bloomFilterService.markEmptyLyrics(emptyMarker);
            cacheService.set(cacheKey, Map.of(), 600);
            return null;
        }
        cacheService.set(cacheKey, lyricsMap, CacheService.LYRICS_TTL);
        return lyricsMap;
    }
```

- [ ] **Step 4: 添加 import**

```java
import com.ty1l.spotify_remake.Service.BloomFilterService;
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ty1l/spotify_remake/Service/Public/LyricsServiceiml.java
git commit -m "feat: add bloom filter empty-result marking to LyricsService"
```

---

### Task 11: 最终验证

- [ ] **Step 1: 编译验证**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 检查所有新文件编码为 UTF-8 无 BOM**

Run: `file -bi src/main/java/com/ty1l/spotify_remake/config/RedissonConfig.java` 等

- [ ] **Step 3: Commit any remaining changes**

```bash
git status
```

如果有遗漏的改动，补充提交。

---

## 实施顺序依赖

```
Task 1 (pom.xml) ──┐
                    ├──> Task 2 (RedissonConfig) ──> Task 3 (BloomFilterService) ──┬──> Task 5 (Initializer)
                    │                                                               │
                    └──> Task 4 (Mapper findAllIds) ────────────────────────────────┘
                                                                                     │
                    Task 6 (SongServiceiml) ─────────────────────────────────────────┤
                    Task 7 (ArtistServiceiml) ───────────────────────────────────────┤  all depend on
                    Task 8 (AlbumServiceiml) ────────────────────────────────────────┤  Task 3 + 4
                    Task 9 (ExternalMusicServiceiml) ────────────────────────────────┤
                    Task 10 (LyricsServiceiml) ──────────────────────────────────────┘
```
