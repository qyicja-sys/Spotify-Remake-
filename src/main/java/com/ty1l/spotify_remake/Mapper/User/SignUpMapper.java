package com.ty1l.spotify_remake.Mapper.User;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SignUpMapper {
    //向playlist插入收藏歌单，内容分别为：userId——对应user表的id字段,title——"已点赞的歌曲",coverUrl——"\static\datas\musicResouces\music_cover\personal\like_songs.png",createAt——默认值,type——0
    @Insert("insert into playlists (userId, name, coverUrl, type) values (#{userId}, '已点赞的歌曲', '/static/datas/musicResouces/music_cover/personal/like_songs.png', 0)")
    public void InsertLikePlaylist(Integer userId);
}
