package com.ty1l.spotify_remake.utility;

import lombok.Data;

@Data
public class Result {
    private Integer code;   // 状态码：200成功，其他失败
    private String message; // 提示信息
    private Object data;    // 返回的数据

    // 构造器私有，使用静态方法创建
    private Result() {}

    // 成功结果，不包含数据
    public static Result success() {
        Result r = new Result();
        r.code = 200;
        r.message = "success";
        return r;
    }

    // 成功结果，包含数据
    public static Result success(Object data) {
        Result r = success();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    // 失败结果，包含提示信息，服务器错误
    public static Result error(String message) {
        Result r = new Result();
        r.code = 500;
        r.message = message;
        return r;
    }

    // 失败结果，包含提示信息，客户端错误
    public static Result errorClient(String message) {
        Result r = new Result();
        r.code = 400;
        r.message = message;
        return r;
    }

    // getter / setter ...
}
