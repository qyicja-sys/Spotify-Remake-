package com.ty1l.spotify_remake.Mapper.Public;

import com.ty1l.spotify_remake.Entity.Public.SearchSongVO;
import com.ty1l.spotify_remake.Entity.Public.Song;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SongMapper {

    List<Song> findAll();//查询所有歌曲

    Song findById(Integer id);//根据id查询歌曲

    List<Song> findByTitle(String title);//根据标题查询歌曲

    List<Song> findByArtistId(Integer artistId);//根据艺术家ID查询歌曲

    List<SearchSongVO> searchWithTitle(@Param("title") String title);//搜索歌曲（带艺人名）

    int insert(Song song);//插入歌曲

    int update(Song song);//更新歌曲

    int deleteById(Integer id);//根据id删除歌曲

    Song findByExternalId(@Param("externalSource") String externalSource, @Param("externalId") String externalId);

    List<SearchSongVO> findByArtistIdWithNames(@Param("artistId") Integer artistId);

    List<Song> findByAlbumId(@Param("albumId") Long albumId);

    int updateAlbumIdAndCover(@Param("songId") Integer songId, @Param("albumId") Long albumId, @Param("coverUrl") String coverUrl);
}
