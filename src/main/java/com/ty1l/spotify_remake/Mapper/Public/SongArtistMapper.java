package com.ty1l.spotify_remake.Mapper.Public;

import com.ty1l.spotify_remake.Entity.Public.SongArtist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SongArtistMapper {

    List<SongArtist> findBySongId(@Param("songId") Integer songId);

    int insert(SongArtist songArtist);

    int deleteBySongId(@Param("songId") Integer songId);
}
