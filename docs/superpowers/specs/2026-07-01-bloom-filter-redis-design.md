# RedisBloom 布隆过滤器防缓存击穿 — 设计文档

**日期**: 2026-07-01
**分支**: `feature/captcha-redis-migration`
**状态**: 待评审

## 1. 背景

当前缓存模式为 Cache-Aside：请求 → 查 Redis → 未命中 → 查 DB，缓存结果。恶意请求用不存在的 ID 反复访问时，每次穿透缓存打到 DB（缓存击穿）。此外外部音乐 API 搜索也存在类似问题——乱码关键词反复穿透到外部 API。

### 受影响的服务

| 服务 | 方法 | 缓存 Key | 穿透目标 |
|------|------|----------|----------|
| SongServiceiml | findById | `spotify:song:{id}` | MySQL |
| ArtistServiceiml | getArtistDetail | `spotify:artist:{id}` | MySQL |
| AlbumServiceiml | findById | `spotify:album:{id}` | MySQL |
| AlbumServiceiml | findByArtistId | `spotify:album:artist:{id}` | MySQL |
| ExternalMusicServiceiml | search | `spotify:external:search:{kw}` | 外部 API |
| ExternalMusicServiceiml | searchByArtist | `spotify:external:artist:{name}` | 外部 API |
| LyricsServiceiml | getLocalLyrics | `spotify:lyrics:local:{title}` | MySQL |
| LyricsServiceiml | getExternalLyrics | `spotify:lyrics:external:{src}:{id}` | 外部 API |

## 2. 方案选择

**Redisson RBloomFilter** — 调用 RedisBloom 模块原生命令（`BF.RESERVE`、`BF.ADD`、`BF.EXISTS`），API 干净，与现有 Lettuce 连接池独立，不相互影响。

## 3. 架构

```
请求 → 布隆过滤器检查
  ├─ 数据存在性过滤器（song/artist/album）
  │   ├─ 不存在 → 直接返回 null（拦截穿透）
  │   └─ 可能存在 → Cache-Aside 流程（Redis → DB）
  └─ 空结果标记过滤器（search/lyrics）
      ├─ 已知无结果 → 直接返回空列表
      └─ 未知 → 执行查询 → 结果为空时标记到过滤器
```

## 4. 布隆过滤器 Key 设计

遵循 `{业务}:{模块}:{key}` 规范。

### 数据存在性过滤器（预加载）

| 过滤器 | Redis Key | 存储内容 | 预计插入量 | 初始化 |
|--------|-----------|----------|-----------|--------|
| 歌曲 ID | `spotify:bloom:song` | song.id | 10,000 | 启动时全量加载 |
| 艺术家 ID | `spotify:bloom:artist` | artist.id | 1,000 | 启动时全量加载 |
| 专辑 ID | `spotify:bloom:album` | album.id | 5,000 | 启动时全量加载 |

### 空结果标记过滤器（运行时积累）

| 过滤器 | Redis Key | 存储内容 | 预计插入量 | 初始化 |
|--------|-----------|----------|-----------|--------|
| 外部搜索空词 | `spotify:bloom:search:empty` | 无结果的搜索关键词 | 100,000 | 首次使用时 |
| 艺人搜索空词 | `spotify:bloom:artist-search:empty` | 无结果的艺人搜索词 | 50,000 | 首次使用时 |
| 本地歌词空标题 | `spotify:bloom:lyrics:empty` | 无歌词的标题 | 50,000 | 首次使用时 |

所有过滤器误判率统一 **0.01**。

## 5. 代码结构

```
新增文件：
├── config/RedissonConfig.java              # RedissonClient Bean
├── Service/BloomFilterService.java         # 封装 RBloomFilter 操作
├── Service/BloomFilterInitializer.java     # ApplicationRunner 启动预加载

修改文件：
├── pom.xml                                 # 添加 redisson-spring-boot-starter
├── Service/Public/SongServiceiml.java      # findById 加布隆检查
├── Service/Public/ArtistServiceiml.java    # getArtistDetail 加布隆检查
├── Service/Public/AlbumServiceiml.java     # findById/findByArtistId 加布隆检查
├── Service/Public/ExternalMusicServiceiml.java  # search/searchByArtist 空结果标记
├── Service/Public/LyricsServiceiml.java    # getLocalLyrics/getExternalLyrics 空结果标记
├── Mapper/Public/SongMapper.java           # 新增 findAllIds
├── Mapper/Public/ArtistMapper.java         # 新增 findAllIds
├── Mapper/Public/AlbumMapper.java          # 新增 findAllIds
└── resources/com/.../Mapper/SongMapper.xml 等 # 新增 findAllIds SQL
```

## 6. 核心组件详情

### 6.1 RedissonConfig

- 从 `application.yaml` 读取 `spring.data.redis.host` 和 `port`
- 连接池大小 4，最小空闲 1（不与 Lettuce 主连接池竞争）
- 超时 3000ms，重试 3 次

### 6.2 BloomFilterService

```java
@Service
public class BloomFilterService {
    // 数据存在性
    boolean songExists(Integer id);
    boolean artistExists(Integer id);
    boolean albumExists(Long id);
    void addSong(Integer id);
    void addArtist(Integer id);
    void addAlbum(Long id);

    // 空结果标记
    boolean isKnownEmptySearch(String keyword);
    boolean isKnownEmptyArtistSearch(String name);
    boolean isKnownEmptyLyrics(String title);
    void markEmptySearch(String keyword);
    void markEmptyArtistSearch(String name);
    void markEmptyLyrics(String title);
}
```

- 内部用 `RedissonClient.getBloomFilter(key)` 获取 `RBloomFilter`
- 懒初始化：首次调用时 `tryInit(expectedInsertions, 0.01)`，key 已存在则跳过
- `add*` 批量方法也提供 `addSongBatch(int[])` 用于启动预加载

### 6.3 BloomFilterInitializer

`@Component` 实现 `ApplicationRunner`，`run()` 中：
1. 调用 `songMapper.findAllIds()` 批量 add 到 `spotify:bloom:song`
2. 调用 `artistMapper.findAllIds()` 批量 add 到 `spotify:bloom:artist`
3. 调用 `albumMapper.findAllIds()` 批量 add 到 `spotify:bloom:album`
4. 记录耗时和插入数量日志

### 6.4 Mapper 新增查询

```xml
<!-- SongMapper.xml -->
<select id="findAllIds" resultType="java.lang.Integer">
    SELECT id FROM songs
</select>

<!-- ArtistMapper.xml -->
<select id="findAllIds" resultType="java.lang.Integer">
    SELECT id FROM artists
</select>

<!-- AlbumMapper.xml -->
<select id="findAllIds" resultType="java.lang.Long">
    SELECT id FROM albums
</select>
```

## 7. Service 改动逻辑

### 7.1 数据存在性检查（以 SongServiceiml.findById 为例）

```java
public Song findById(Integer id) {
    // 1. 布隆过滤器：id 绝对不存在 → 直接返回 null
    if (!bloomFilterService.songExists(id)) {
        return null;
    }
    // 2. 正常 Cache-Aside 流程
    String key = String.format(CacheService.KEY_SONG, id);
    Song cached = cacheService.get(key, Song.class);
    if (cached != null) return cached;
    Song song = songMapper.findById(id);
    if (song != null) cacheService.set(key, song);
    return song;
}
```

同样的模式应用于 ArtistServiceiml.getArtistDetail、AlbumServiceiml.findById、AlbumServiceiml.findByArtistId。

### 7.2 空结果标记（以 ExternalMusicServiceiml.search 为例）

```java
public List<ExternalTrackVO> search(String keyword) {
    String normalized = keyword.trim().replaceAll("\\s+", " ");
    // 布隆过滤器：已知无结果 → 直接返回空
    if (bloomFilterService.isKnownEmptySearch(normalized)) {
        return Collections.emptyList();
    }
    // 正常缓存 → API 流程
    ...
    if (results == null || results.isEmpty()) {
        bloomFilterService.markEmptySearch(normalized);
        return Collections.emptyList();
    }
    return results;
}
```

## 8. 数据维护

- **新增数据**：各 Service 的 `add()` 方法中调用 `bloomFilterService.addXxx()`
- **删除数据**：不做操作（布隆过滤器不支持删除，假阳性的代价远小于穿透）
- **启动预加载**：每次启动时全量重建数据存在性过滤器（数据量小，毫秒级完成）

## 9. 边界与风险

- **假阳性**：0.01 误判率下，被拦截的真实请求概率为 0%——数据存在性过滤器只在 ID 不在过滤器中时才拦截（"definitely not" 语义），误判只出现在"可能存在的 ID 中混入了不存在的 ID"，不影响正确性，仅影响拦截率
- **空结果标记的假阳性**：一个首次搜索无结果但后来有了结果的关键词会被误判为空，但搜索场景下这个概率极低且影响小（用户只需刷新页面，下次请求可能重建缓存）
- **Redisson 与 Lettuce 共存**：使用独立连接池，互不影响
- **内存占用**：6 个布隆过滤器合计约 1.5 MB

## 10. 验证方式

- 启动日志确认布隆过滤器预加载完成（song/artist/album 插入数量）
- 用不存在 ID 请求 `/api/song/{id}`，确认日志中显示 "Bloom filter rejected"
- 用不存在关键词搜索，第一次穿透到 API 后标记空结果，第二次请求直接返回空列表
