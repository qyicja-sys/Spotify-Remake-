package com.ty1l.spotify_remake.Service.Public;

import java.util.Map;

public interface LyricsService {

    /**
     * 根据歌名查询本地歌词
     * @param title 歌曲名称
     * @return Map: { "lyric": "LRC歌词内容" } 或 null
     */
    Map<String, String> getLocalLyrics(String title);

    /**
     * 获取外部歌曲歌词
     * @param source 音乐源 (gdmusic)
     * @param lyricId 歌词ID
     * @return Map: { "lyric": "LRC歌词内容" } 或 null
     */
    Map<String, String> getExternalLyrics(String source, String lyricId);
}
