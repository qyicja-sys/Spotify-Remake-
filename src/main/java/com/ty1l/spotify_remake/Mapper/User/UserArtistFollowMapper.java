package com.ty1l.spotify_remake.Mapper.User;

import com.ty1l.spotify_remake.Entity.Public.Artist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserArtistFollowMapper {

    int insert(@Param("userId") Long userId, @Param("artistId") Integer artistId);

    int delete(@Param("userId") Long userId, @Param("artistId") Integer artistId);

    int count(@Param("userId") Long userId, @Param("artistId") Integer artistId);

    List<Artist> findFollowedArtists(@Param("userId") Long userId);

    int countFollowersByArtistId(@Param("artistId") Integer artistId);
}
