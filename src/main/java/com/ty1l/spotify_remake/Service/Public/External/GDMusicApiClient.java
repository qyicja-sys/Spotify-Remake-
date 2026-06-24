package com.ty1l.spotify_remake.Service.Public.External;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ty1l.spotify_remake.Entity.Public.ExternalTrackVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class GDMusicApiClient {

    @Value("${music.external.gdmusic.base-url}")
    private String baseUrl;

    @Value("${music.external.gdmusic.source}")
    private String defaultSource;

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public GDMusicApiClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
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
                vo.setExternalId(id);
                vo.setSource("gdmusic");
                vo.setTitle(track.path("name").asText());
                // artist 是数组，拼接为字符串
                vo.setArtistName(parseArtistName(track.path("artist")));
                vo.setStreamUrl("/spotify/external/stream-proxy/gdmusic/" + source + "_" + id);
                results.add(vo);

                // 记录需要获取封面的任务
                String picId = track.path("pic_id").asText(null);
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
                        }))
                        .toList();
                try {
                    CompletableFuture.allOf(coverFutures.toArray(new CompletableFuture[0]))
                            .get(5, TimeUnit.SECONDS);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("[GDMusic] search failed: " + e.getMessage());
        }
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
