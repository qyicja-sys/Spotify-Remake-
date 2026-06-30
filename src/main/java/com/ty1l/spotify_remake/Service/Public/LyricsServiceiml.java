package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.Song;
import com.ty1l.spotify_remake.Mapper.Public.SongMapper;
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

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private ExternalMusicService externalMusicService;

    @Override
    public Map<String, String> getLocalLyrics(String title) {
        List<Song> songs = songMapper.findByTitle(title);
        if (songs.isEmpty()) return null;

        // 取第一个匹配的歌曲
        Song song = songs.get(0);
        String lyricsPath = song.getLyrics();
        if (lyricsPath == null || lyricsPath.isEmpty()) return null;

        String content = null;

        // 如果 lyrics 字段存储的是 .lrc 文件路径，从磁盘读取
        if (lyricsPath.endsWith(".lrc")) {
            content = readLrcFile(lyricsPath);
        } else {
            // 否则当作直接存储的 LRC 歌词内容
            content = lyricsPath;
        }

        if (content == null || content.isEmpty()) return null;

        Map<String, String> result = new HashMap<>();
        result.put("lyric", content);
        return result;
    }

    @Override
    public Map<String, String> getExternalLyrics(String source, String lyricId) {
        String lrc = externalMusicService.getLyrics(source, lyricId);
        if (lrc == null || lrc.isEmpty()) return null;

        Map<String, String> result = new HashMap<>();
        result.put("lyric", lrc);
        return result;
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
