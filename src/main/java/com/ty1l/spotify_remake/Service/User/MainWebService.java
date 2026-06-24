package com.ty1l.spotify_remake.Service.User;

import com.ty1l.spotify_remake.Entity.User.PlaylistBriefVO;

public interface MainWebService {
    // 新建歌单：根据用户ID创建歌单，返回新建歌单信息
    PlaylistBriefVO createPlaylist(Long userId);
}
