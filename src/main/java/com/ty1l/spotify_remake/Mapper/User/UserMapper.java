package com.ty1l.spotify_remake.Mapper.User;

import com.ty1l.spotify_remake.Entity.User.User;
import com.ty1l.spotify_remake.Entity.User.LoginUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
//    public List<User> FindUser();

    @Select("select * from user where email = #{email}")
    public User Login(LoginUser user);// 登录，先查询用户是否存在，再验证密码

    @Insert("insert into user (userName, nickName, gender, email, password, phone, personalMotto) values (#{userName}, #{nickName}, #{gender}, #{email}, #{password}, #{phone}, #{personalMotto})")
    @Options(useGeneratedKeys = true, keyProperty = "id") // 自动回填自增ID到user对象的id属性中
    public Integer Register(User user);// 注册

    //查询邮箱是否重复（用户是否存在）
    @Select("select count(*) from user where email = #{email}")
    public Integer FindUserByEmail(String email);

    //查询用户是否存在
    @Select("select count(*) from user where userName = #{userName}")
    public Integer FindUserByName(String userName);

    //忘记密码，更新密码
    @Update("update user set password = #{password} where email = #{email}")
    public int UpdatePassword(@Param("email") String email, @Param("password") String password);

    //差找userid
    @Select("select id from user where userName = #{userName}")
    public Integer FindUserIdByName(String userName);

    //查询所有标记为歌手的用户
    @Select("select * from user where is_artist = 1")
    public List<User> findArtistUsers();

    //根据昵称更新用户头像
    @Update("update user set profilePic = #{profilePic} where nickName = #{nickName}")
    public int updateProfilePicByNickName(@Param("nickName") String nickName, @Param("profilePic") String profilePic);

    //根据ID查询用户
    @Select("select * from user where id = #{id}")
    public User findById(@Param("id") Long id);

    //根据ID更新昵称
    @Update("update user set nickName = #{nickName} where id = #{id}")
    public int updateNickNameById(@Param("id") Long id, @Param("nickName") String nickName);

    //根据ID更新头像
    @Update("update user set profilePic = #{profilePic} where id = #{id}")
    public int updateProfilePicById(@Param("id") Long id, @Param("profilePic") String profilePic);

    //根据昵称查询用户数量（判断昵称是否已存在）
    @Select("select count(*) from user where nickName = #{nickName}")
    public int countByNickName(@Param("nickName") String nickName);

    //根据ID更新艺术家标记
    @Update("update user set is_artist = #{isArtist} where id = #{id}")
    public int updateIsArtistById(@Param("id") Long id, @Param("isArtist") Integer isArtist);
}
