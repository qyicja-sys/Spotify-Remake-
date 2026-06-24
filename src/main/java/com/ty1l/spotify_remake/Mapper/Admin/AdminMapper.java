package com.ty1l.spotify_remake.Mapper.Admin;

import com.ty1l.spotify_remake.Entity.Admin.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminMapper {

    @Select("SELECT * FROM sys_admin WHERE username = #{username} AND is_deleted = 0 AND status = 1")
    Admin findByUsername(String username);
}
