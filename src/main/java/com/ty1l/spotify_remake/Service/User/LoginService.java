package com.ty1l.spotify_remake.Service.User;

import com.ty1l.spotify_remake.Entity.User.HomeDashboardVO;
import com.ty1l.spotify_remake.Entity.User.LoginUser;
import com.ty1l.spotify_remake.Entity.User.PlaylistBriefVO;

import java.util.List;

public interface LoginService {

    // 登录
    public HomeDashboardVO Login(LoginUser user);

    //忘记密码
    public void forgetPassword(String email, String newPassword,String token);

    //补全歌单图片url
    public List<PlaylistBriefVO> completePlaylistCoverUrl(List<PlaylistBriefVO> playlists);
}