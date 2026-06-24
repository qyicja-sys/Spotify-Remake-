package com.ty1l.spotify_remake.Mapper.User;

import com.ty1l.spotify_remake.Entity.User.LoginInfoVo;
import com.ty1l.spotify_remake.Entity.User.PlaylistBriefVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LoginMapper {
    LoginInfoVo PackageLoginInfo(String userName);

    List<PlaylistBriefVO> PackageMyPlaylists(Integer userId);

    List<PlaylistBriefVO> PackageSystemPlaylists();
}
