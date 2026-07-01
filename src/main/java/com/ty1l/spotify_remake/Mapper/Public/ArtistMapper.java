package com.ty1l.spotify_remake.Mapper.Public;

import com.ty1l.spotify_remake.Entity.Public.Artist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArtistMapper {

    List<Artist> findAll();

    Artist findById(Integer id);

    Artist findByName(@Param("name") String name);

    List<Artist> findByNameSearch(@Param("name") String name);

    int insert(Artist artist);

    int update(Artist artist);

    int deleteById(Integer id);

    /**
     * 查询所有艺术家 ID（布隆过滤器启动预加载用）
     */
    List<Integer> findAllIds();
}
