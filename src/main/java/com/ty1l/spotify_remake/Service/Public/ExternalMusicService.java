package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.ExternalTrackVO;

import java.util.List;
import java.util.Map;

public interface ExternalMusicService {

    List<ExternalTrackVO> search(String keyword);

    List<ExternalTrackVO> searchByArtist(String artistName);

    String getRealStreamUrl(String source, String trackId);

    /** 获取外部歌词，返回 Map 包含 lyric（原文）和 tlyric（翻译），至少包含一个 */
    Map<String, String> getLyrics(String source, String lyricId);

    /**
     * 将外部搜索结果中的歌词同步到本地 songs 表：
     * 1. 若本地已有该歌曲（按 externalSource+externalId 或 title+主艺术家匹配）且 lyrics 为空，从外部 API 获取歌词并更新
     * 2. 仅处理 lyrics 字段为空的歌曲，避免覆盖已有歌词
     *
     * @param results 外部搜索结果
     */
    void syncLyricsToLocal(List<ExternalTrackVO> results);
}