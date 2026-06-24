package com.ty1l.spotify_remake.Service.User;

import com.ty1l.spotify_remake.Entity.User.ProfileVO;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {
    // 获取用户个人主页信息（含最近播放记录）
    ProfileVO getProfile(Long userId);

    // 更新用户昵称
    void updateNickName(Long userId, String nickName);

    // 更新用户头像
    String updateProfilePic(Long userId, MultipartFile file) throws Exception;

    // 注册成为艺术家
    void registerArtist(Long userId, String nickName);
}
