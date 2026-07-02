package com.ty1l.spotify_remake.Service.Public.External;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ty1l.spotify_remake.Entity.Public.ExternalTrackVO;
import com.ty1l.spotify_remake.utility.SignedUrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class GDMusicApiClient {

    @Value("${music.external.gdmusic.base-url}")
    private String baseUrl;

    @Value("${music.external.gdmusic.source}")
    private String defaultSource;

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public GDMusicApiClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    public List<ExternalTrackVO> search(String keyword) {
        List<ExternalTrackVO> results = new ArrayList<>();
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api.php")
                .queryParam("types", "search")
                .queryParam("source", defaultSource)
                .queryParam("name", keyword)
                .queryParam("count", 10)
                .queryParam("pages", 1)
                .build()
                .toUriString();
        try {
            String json = restTemplate.getForObject(url, String.class);
            JsonNode tracks = mapper.readTree(json);
            if (!tracks.isArray()) return results;

            // 先解析所有 track 基本信息
            List<String[]> coverTasks = new ArrayList<>();
            for (JsonNode track : tracks) {
                ExternalTrackVO vo = new ExternalTrackVO();
                String id = track.path("id").asText();
                String source = track.path("source").asText(defaultSource);
                vo.setExternalId(source + "_" + id);
                vo.setSource("gdmusic");
                vo.setTitle(track.path("name").asText());
                // artist 是数组，拼接为字符串
                vo.setArtistName(parseArtistName(track.path("artist")));
                vo.setStreamUrl(SignedUrlUtil.generate("/spotify/external/stream-proxy/gdmusic/" + source + "_" + id));
                // 解析歌词ID，用于后续获取歌词
                String lyricId = track.path("lyric_id").asText(null);
                if (lyricId == null || lyricId.isEmpty()) {
                    lyricId = id; // lyric_id 一般与曲目ID相同
                }
                vo.setLyricId(lyricId);
                results.add(vo);

                // 记录需要获取封面的任务
                String picId = track.path("pic_id").asText(null);
                vo.setPicId(picId);
                if (picId != null && !picId.isEmpty()) {
                    coverTasks.add(new String[]{String.valueOf(results.size() - 1), source, picId});
                }
            }

            // 并行获取所有封面 URL
            if (!coverTasks.isEmpty()) {
                List<CompletableFuture<Void>> coverFutures = coverTasks.stream()
                        .map(task -> CompletableFuture.runAsync(() -> {
                            try {
                                int index = Integer.parseInt(task[0]);
                                String coverUrl = getCoverUrl(task[1], task[2]);
                                results.get(index).setCoverUrl(coverUrl);
                            } catch (Exception ignored) {}
                        }, executor))
                        .toList();
                try {
                    CompletableFuture.allOf(coverFutures.toArray(new CompletableFuture[0]))
                            .get(5, TimeUnit.SECONDS);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("[GDMusic] search failed: " + e.getMessage());
        }
        results.removeIf(r -> r.getStreamUrl() == null || r.getStreamUrl().isEmpty()
                || r.getCoverUrl() == null || r.getCoverUrl().isEmpty());
        return results;
    }

    /** 解析 artist 字段（可能是数组或字符串） */
    private String parseArtistName(JsonNode artistNode) {
        if (artistNode == null || artistNode.isMissingNode()) return "未知艺人";
        if (artistNode.isArray()) {
            List<String> names = new ArrayList<>();
            for (JsonNode n : artistNode) {
                String name = n.asText(null);
                if (name != null && !name.isEmpty()) names.add(name);
            }
            return names.isEmpty() ? "未知艺人" : String.join(", ", names);
        }
        String text = artistNode.asText(null);
        return (text != null && !text.isEmpty()) ? text : "未知艺人";
    }

    /** 获取专辑封面 URL */
    private String getCoverUrl(String source, String picId) {
        try {
            String url = String.format("%s/api.php?types=pic&source=%s&id=%s&size=300", baseUrl, source, picId);
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(json);
            return root.path("url").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    /** 获取歌词（同时返回原文和翻译） */
    public Map<String, String> getLyrics(String source, String lyricId) {
        try {
            String url = String.format("%s/api.php?types=lyric&source=%s&id=%s",
                    baseUrl, source, lyricId);
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(json);
            String lyric = root.path("lyric").asText(null);
            String tlyric = root.path("tlyric").asText(null);
            Map<String, String> result = new HashMap<>();
            if (lyric != null && !lyric.isEmpty()) result.put("lyric", lyric);
            if (tlyric != null && !tlyric.isEmpty()) result.put("tlyric", tlyric);
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            System.err.println("[GDMusic] getLyrics failed: " + e.getMessage());
            return null;
        }
    }

    /** 按艺人名搜索外部歌曲，并行搜索多源，按艺人名过滤，返回合并去重后的结果 */
    public List<ExternalTrackVO> searchByArtist(String artistName) {
        List<ExternalTrackVO> results = new ArrayList<>();
        String[] sources = {"netease", "joox"};

        // 并行搜索多个音乐源
        List<CompletableFuture<List<ExternalTrackVO>>> sourceFutures = new ArrayList<>();
        for (String src : sources) {
            sourceFutures.add(CompletableFuture.supplyAsync(() -> {
                List<ExternalTrackVO> srcResults = new ArrayList<>();
                try {
                    String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api.php")
                            .queryParam("types", "search")
                            .queryParam("source", src)
                            .queryParam("name", artistName)
                            .queryParam("count", 30)
                            .queryParam("pages", 1)
                            .build()
                            .toUriString();
                    String json = restTemplate.getForObject(url, String.class);
                    JsonNode tracks = mapper.readTree(json);
                    if (!tracks.isArray()) return srcResults;

                    List<String[]> coverTasks = new ArrayList<>();
                    for (JsonNode track : tracks) {
                        // 按艺人名过滤：只保留 artist 数组中包含搜索关键字的歌曲
                        if (!artistNameMatches(track.path("artist"), artistName)) continue;
                        ExternalTrackVO vo = new ExternalTrackVO();
                        String id = track.path("id").asText();
                        String trackSource = track.path("source").asText(src);
                        vo.setExternalId(trackSource + "_" + id);
                        vo.setSource("gdmusic");
                        vo.setTitle(track.path("name").asText());
                        vo.setArtistName(parseArtistName(track.path("artist")));
                        vo.setStreamUrl(SignedUrlUtil.generate("/spotify/external/stream-proxy/gdmusic/" + trackSource + "_" + id));
                        String lyricId = track.path("lyric_id").asText(null);
                        if (lyricId == null || lyricId.isEmpty()) {
                            lyricId = id;
                        }
                        vo.setLyricId(lyricId);
                        srcResults.add(vo);

                        String picId = track.path("pic_id").asText(null);
                vo.setPicId(picId);
                        if (picId != null && !picId.isEmpty()) {
                            coverTasks.add(new String[]{String.valueOf(srcResults.size() - 1), trackSource, picId});
                        }
                    }

                    // 并行获取封面
                    if (!coverTasks.isEmpty()) {
                        List<CompletableFuture<Void>> coverFutures = coverTasks.stream()
                                .map(task -> CompletableFuture.runAsync(() -> {
                                    try {
                                        int index = Integer.parseInt(task[0]);
                                        String coverUrl = getCoverUrl(task[1], task[2]);
                                        srcResults.get(index).setCoverUrl(coverUrl);
                                    } catch (Exception ignored) {}
                                }, executor))
                                .toList();
                        try {
                            CompletableFuture.allOf(coverFutures.toArray(new CompletableFuture[0]))
                                    .get(5, TimeUnit.SECONDS);
                        } catch (Exception ignored) {}
                    }
                } catch (Exception e) {
                    System.err.println("[GDMusic] searchByArtist failed for source " + src + ": " + e.getMessage());
                }
                return srcResults;
            }, executor));
        }

        // 等待所有源完成，合并去重
        try {
            CompletableFuture.allOf(sourceFutures.toArray(new CompletableFuture[0]))
                    .get(8, TimeUnit.SECONDS);
        } catch (Exception ignored) {}

        for (CompletableFuture<List<ExternalTrackVO>> future : sourceFutures) {
            try {
                List<ExternalTrackVO> srcResults = future.get();
                for (ExternalTrackVO vo : srcResults) {
                    boolean duplicate = results.stream().anyMatch(r ->
                            normalizeTitle(r.getTitle()).equals(normalizeTitle(vo.getTitle())) && primaryArtist(r.getArtistName()).equals(primaryArtist(vo.getArtistName())));
                    if (!duplicate) {
                        results.add(vo);
                    }
                }
            } catch (Exception ignored) {}
        }
        results.removeIf(r -> r.getStreamUrl() == null || r.getStreamUrl().isEmpty()
                || r.getCoverUrl() == null || r.getCoverUrl().isEmpty());
        return results;
    }

    /** 检查艺人名是否匹配 artist JSON 节点中的任一项（双向匹配：精确匹配 或 搜索名包含艺人名） */
    private boolean artistNameMatches(JsonNode artistNode, String searchName) {
        if (artistNode == null || artistNode.isMissingNode()) return false;
        String searchLower = searchName.trim().toLowerCase();
        if (artistNode.isArray()) {
            for (JsonNode n : artistNode) {
                String name = n.asText(null);
                if (name != null) {
                    String nameLower = name.trim().toLowerCase();
                    // 精确匹配 或 搜索名包含艺人名（支持组合艺人如 "A, B" 匹配单独的 "A"）
                    if (nameLower.equals(searchLower) || searchLower.contains(nameLower)) return true;
                }
            }
            return false;
        }
        String text = artistNode.asText(null);
        if (text != null) {
            String textLower = text.trim().toLowerCase();
            return textLower.equals(searchLower) || searchLower.contains(textLower);
        }
        return false;
    }

    /** 去掉 Explicit/Clean/Remaster 等版本后缀，用于去重比较 */
    private static String normalizeTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase().replaceAll("\\s*(?:\\(|\\[)?explicit(?:\\)|\\])?|\\s*(?:\\(|\\[)?clean(?:\\)|\\])?|\\s*(?:\\(|\\[)?remaster(?:ed)?(?:\\s*\\d{4})?(?:\\)|\\])?|\\s*-\\s*explicit|\\s*-\\s*clean", "").trim();
    }

    /** 取主要作者（第一个），用于去重比较 */
    private static String primaryArtist(String artistName) {
        if (artistName == null || artistName.isEmpty()) return "";
        String first = artistName.split(",")[0].trim().toLowerCase();
        return first;
    }

    /** 获取真实音频流 URL，sourceId 格式为 "source_trackId" */
    public String getRealStreamUrl(String sourceId) {
        int sep = sourceId.indexOf('_');
        if (sep <= 0) return null;
        String source = sourceId.substring(0, sep);
        String trackId = sourceId.substring(sep + 1);
        String url = String.format(
                "%s/api.php?types=url&source=%s&id=%s&br=320",
                baseUrl, source, trackId
        );
        try {
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(json);
            return root.path("url").asText(null);
        } catch (Exception e) {
            return null;
        }
    }
}