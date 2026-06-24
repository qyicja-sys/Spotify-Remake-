package com.ty1l.spotify_remake.Service.User;

import com.ty1l.spotify_remake.Entity.User.Playlist;
import com.ty1l.spotify_remake.Entity.User.PlaylistBriefVO;
import com.ty1l.spotify_remake.Mapper.User.mainWebMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MainWebServiceiml implements MainWebService {

    @Autowired
    private mainWebMapper mainWebMapper;

    @Override
    public PlaylistBriefVO createPlaylist(Long userId) {
        // 1. 统计该用户已有歌单数量，生成默认名称
        int count = mainWebMapper.countUserPlaylists(userId);
        String name = "新建歌单 #" + (count + 1);

        // 2. 插入数据库
        Playlist playlist = new Playlist();
        playlist.setUserId(String.valueOf(userId));
        playlist.setName(name);
        playlist.setType("0");
        mainWebMapper.insertPlaylist(playlist);

        // 3. 封装返回值
        PlaylistBriefVO vo = new PlaylistBriefVO();
        vo.setId(playlist.getId());
        vo.setUserId(Math.toIntExact(userId));
        vo.setName(name);
        vo.setCoverUrl(null);
        vo.setType(0);
        return vo;
    }
}
