package com.ty1l.spotify_remake.utility;
//如果你有很多个 Controller 接口（比如：获取自建歌单、修改个人资料、收藏歌曲）都需要知道当前登录的是谁，
//每次都在 Controller 里写一遍 request.getHeader("token") 和解析代码就会非常痛苦，代码也显得很臃肿。
//既然你的 TokenInterceptor 已经拦截了所有请求并且解析了 Token，你可以利用 ThreadLocal 把解析出来的用户 ID 存起来。
//
//ThreadLocal 可以保证在同一个请求（同一个线程）的生命周期内，随时随地都能直接拿到数据


public class BaseContext {
    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();
    private static final ThreadLocal<String> versionLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void setCurrentVersion(String version) {
        versionLocal.set(version);
    }

    public static String getCurrentVersion() {
        return versionLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
        versionLocal.remove();
    }
}