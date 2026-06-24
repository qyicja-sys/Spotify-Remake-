package com.ty1l.spotify_remake.Exception;

import com.ty1l.spotify_remake.utility.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // 这个注解让此类成为全局“救火员”
public class GlobalExceptionHandler {

    // 捕获你在 Service 里抛出的特定异常（假设你叫它 LoginFailedException）
    // 如果你还没定义异常类，也可以先捕获 RuntimeException.class
    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        e.printStackTrace(); // 在控制台打印错误堆栈，方便调试

        // 将报错信息包装进你的 Result.errorClient 中返回
        return Result.errorClient(e.getMessage());
    }
}
