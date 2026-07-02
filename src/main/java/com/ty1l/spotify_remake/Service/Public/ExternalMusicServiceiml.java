package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.ExternalTrackVO;
import com.ty1l.spotify_remake.Entity.Public.Song;
import com.ty1l.spotify_remake.Mapper.Public.SongMapper;
import com.ty1l.spotify_remake.Service.BloomFilterService;
import com.ty1l.spotify_remake.Service.CacheService;
import com.ty1l.spotify_remake.Service.Public.External.GDMusicApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ExternalMusicServiceiml implements ExternalMusicService {

    private static final Logger log = LoggerFactory.getLogger(ExternalMusicServiceiml.class);

    @Autowired
    private GDMusicApiClient gdMusicApiClient;

    @Autowired
    private ArtistService artistService;

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private BloomFilterService bloomFilterService;

    @Value("${music.external.gdmusic.source}")
    private String defaultSource;

    @Override
    public List<ExternalTrackVO> search(String keyword) {
        String normalized = keyword.trim().replaceAll("\\s+", " ");
        if (bloomFilterService.isKnownEmptySearch(normalized)) {
            log.debug("Bloom filter rejected empty search keyword={}", normalized);
            return Collections.emptyList();
        }

        String cacheKey = String.format(CacheService.KEY_EXTERNAL_SEARCH, normalized);

        return cacheService.getOrLoadList(cacheKey, ExternalTrackVO.class, () -> {
            log.info("External search API query for keyword={}", normalized);
            List<ExternalTrackVO> results = gdMusicApiClient.search(keyword);
            if (results == null || results.isEmpty()) {
                bloomFilterService.markEmptySearch(normalized);
                return Collections.emptyList();
            }
            persistArtists(results);
            syncLyricsToLocal(results);
            return results;
        }, CacheService.EXTERNAL_SEARCH_TTL);
    }

    @Override
    public List<ExternalTrackVO> searchByArtist(String artistName) {
        String normalized = artistName.trim().replaceAll("\\s+", " ");
        if (bloomFilterService.isKnownEmptyArtistSearch(normalized)) {
            log.debug("Bloom filter rejected empty artist search name={}", normalized);
            return Collections.emptyList();
        }

        String cacheKey = String.format(CacheService.KEY_EXTERNAL_ARTIST, normalized);

        return cacheService.getOrLoadList(cacheKey, ExternalTrackVO.class, () -> {
            log.info("External artist search API query for artistName={}", normalized);
            List<ExternalTrackVO> results = gdMusicApiClient.searchByArtist(artistName);
            if (results == null || results.isEmpty()) {
                bloomFilterService.markEmptyArtistSearch(normalized);
                return Collections.emptyList();
            }
            persistArtists(results);
            syncLyricsToLocal(results);
            return results;
        }, CacheService.EXTERNAL_SEARCH_TTL);
    }

    /**
     * 从外部搜索结果中提取所有艺术家名称，调用 resolveOrCreateArtists 保存到本地数据库，
     * 方便后续本地搜索命中。
     */
    private void persistArtists(List<ExternalTrackVO> results) {
        for (ExternalTrackVO vo : results) {
            String artistName = vo.getArtistName();
            if (artistName == null || artistName.isBlank() || "未知艺人".equals(artistName)) {
                continue;
            }
            try {
                artistService.resolveOrCreateArtists(artistName.split(","));
            } catch (Exception ignored) {
                // 静默失败，不影响搜索结果返回
            }
        }
    }

    @Override
    public String getRealStreamUrl(String source, String trackId) {
        if (!"gdmusic".equals(source)) {
            return null;
        }
        String sourceId = trackId.contains("_") ? trackId : defaultSource + "_" + trackId;
        String cacheKey = String.format(CacheService.KEY_EXTERNAL_STREAM, source, trackId);

        return cacheService.getOrLoad(cacheKey, String.class,
                () -> {
                    log.info("Stream URL API query for source={} trackId={}", source, trackId);
                    return gdMusicApiClient.getRealStreamUrl(sourceId);
                },
                CacheService.EXTERNAL_STREAM_TTL);
    }

    @Override
    public Map<String, String> getLyrics(String source, String lyricId) {
        if ("gdmusic".equals(source)) {
            return gdMusicApiClient.getLyrics("netease", lyricId);
        }
        return null;
    }

    /**
     * 将外部搜索结果中的歌词同步到本地 songs 表：
     * 1. 按 externalSource + externalId 精确匹配本地歌曲
     * 2. 若未找到，按 title + 主艺术家名精确匹配（通过 song_artist + artists 表）
     * 3. 仅当本地歌曲存在且 lyrics 为空时，从外部 API 获取歌词并写入
     *
     * @param results 外部搜索结果
     */
    @Override
    public void syncLyricsToLocal(List<ExternalTrackVO> results) {
        if (results == null || results.isEmpty()) return;

        for (ExternalTrackVO vo : results) {
            try {
                // 提取纯 trackId（去掉 source_ 前缀）
                String externalId = vo.getExternalId();
                String pureTrackId = externalId.contains("_")
                        ? externalId.substring(externalId.indexOf('_') + 1) : externalId;

                // 1. 优先按 externalSource + externalId 精确匹配
                Song localSong = songMapper.findByExternalId(vo.getSource(), externalId);

                // 2. 若未找到，按 title + 主艺术家名匹配
                if (localSong == null && vo.getTitle() != null && !vo.getTitle().isEmpty()) {
                    String primaryArtist = extractPrimaryArtist(vo.getArtistName());
                    if (primaryArtist != null) {
                        localSong = songMapper.findByTitleAndArtistName(vo.getTitle(), primaryArtist);
                    }
                }

                // 3. 仅当本地歌曲存在且 lyrics 为空时，获取并更新
                if (localSong != null && (localSong.getLyrics() == null || localSong.getLyrics().isEmpty())) {
                    Map<String, String> lyricsMap = gdMusicApiClient.getLyrics("netease", pureTrackId);
                    if (lyricsMap != null && lyricsMap.containsKey("lyric")) {
                        localSong.setLyrics(lyricsMap.get("lyric"));
                        songMapper.update(localSong);
                    }
                }
            } catch (Exception ignored) {
                // 单条失败不影响其他歌曲的同步
            }
        }
    }

    /** 提取主艺术家名（逗号分隔取第一个） */
    private String extractPrimaryArtist(String artistName) {
        if (artistName == null || artistName.isEmpty() || "未知艺人".equals(artistName)) {
            return null;
        }
        String primary = artistName.contains(",")
                ? artistName.substring(0, artistName.indexOf(',')).trim()
                : artistName.trim();
        return primary.isEmpty() ? null : primary;
    }
}