package com.ty1l.spotify_remake.Mapper.Public;

import com.ty1l.spotify_remake.Entity.Public.Album;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlbumMapper {

    List<Album> findAll();

    Album findById(@Param("id") Long id);

    List<Album> findByArtistId(@Param("artistId") Long artistId);

    List<Album> findByNameSearch(@Param("name") String name);

    List<Album> findByArtistIdAndNameSearch(@Param("artistId") Long artistId, @Param("name") String name);

    int insert(Album album);

    int update(Album album);

    int deleteById(@Param("id") Long id);
}
