package com.ty1l.spotify_remake.Mapper.Public;

import com.ty1l.spotify_remake.Entity.Public.SongPlayLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 歌曲播放流水 Mapper
 */
@Mapper
public interface SongPlayLogMapper {

    /** 插入或更新播放流水（UPSERT：song_id + play_date 唯一） */
    int upsertPlayLog(SongPlayLog log);

    /** 批量插入播放流水 */
    int batchInsert(@Param("list") List<SongPlayLog> logs);

    /** 批量 UPSERT 播放流水（零点从 Redis ZSET 同步） */
    int batchUpsert(@Param("list") List<SongPlayLog> logs);

    /** 查询指定日期的播放流水汇总（GROUP BY song_id，按播放量降序） */
    List<Map<String, Object>> aggregateByDate(@Param("playDate") LocalDate playDate,
                                               @Param("limit") int limit);

    /** 查询指定日期的所有播放流水汇总 */
    List<Map<String, Object>> aggregateByDateAll(@Param("playDate") LocalDate playDate);
}
