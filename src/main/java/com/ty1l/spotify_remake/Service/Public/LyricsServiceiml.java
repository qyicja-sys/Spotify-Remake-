package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.Song;
import com.ty1l.spotify_remake.Mapper.Public.SongMapper;
import com.ty1l.spotify_remake.Service.BloomFilterService;
import com.ty1l.spotify_remake.Service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LyricsServiceiml implements LyricsService {

    private static final Logger log = LoggerFactory.getLogger(LyricsServiceiml.class);

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private ExternalMusicService externalMusicService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private BloomFilterService bloomFilterService;

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

    @Override
    public Map<String, String> getExternalLyrics(String source, String lyricId) {
        // 布隆过滤器：外部歌词用 (source:lyricId) 合成的 key 标记空结果
        String emptyMarker = source + ":" + lyricId;
        if (bloomFilterService.isKnownEmptyLyrics(emptyMarker)) {
            log.debug("Bloom filter rejected empty external lyrics {}:{}", source, lyricId);
            return null;
        }

        String cacheKey = String.format(CacheService.KEY_LYRICS_EXTERNAL, source, lyricId);

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

    /** 读取 .lrc 歌词文件，优先从 classpath 读取，失败则尝试文件系统 */
    private String readLrcFile(String path) {
        // 尝试从 classpath 读取
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (resource.exists()) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                return sb.toString().trim();
            }
        } catch (Exception ignored) {}

        // 尝试从文件系统读取
        try {
            Path filePath = Paths.get(path);
            if (Files.exists(filePath)) {
                return Files.readString(filePath, StandardCharsets.UTF_8).trim();
            }
        } catch (Exception ignored) {}

        // 尝试从 static 目录读取（项目根目录下的相对路径）
        try {
            Path filePath = Paths.get("src/main/resources", path);
            if (Files.exists(filePath)) {
                return Files.readString(filePath, StandardCharsets.UTF_8).trim();
            }
        } catch (Exception ignored) {}

        return null;
    }
}
