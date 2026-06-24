package com.ty1l.spotify_remake.Entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistBriefVO {
    private Integer id;           // 歌单ID
    private Integer userId;       // 用户ID
    private String name;       // 歌单名字
    private String profile;       // 歌单描述
    private String coverUrl;   // 歌单图片链接
    private String coverNetworkUrl; // 歌单图片链接（网络）
    private Integer type;  // 歌单类型（系统：0，用户：1）
    private Integer isPrivate; // 0=公开, 1=私人
    private String authorName; // 歌单创建者昵称
}
