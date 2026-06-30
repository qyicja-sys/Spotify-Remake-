package com.ty1l.spotify_remake.config;

import com.aliyun.oss.OSS;
import com.ty1l.spotify_remake.utility.FileUploadUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 将 OSS 客户端注入到静态工具类 FileUploadUtil 中。
 */
@Component
public class OssInitializer {

    private final OSS ossClient;
    private final String bucketName;
    private final String endpoint;

    public OssInitializer(OSS ossClient,
                          @Qualifier("ossBucketName") String bucketName,
                          @Qualifier("ossEndpoint") String endpoint) {
        this.ossClient = ossClient;
        this.bucketName = bucketName;
        this.endpoint = endpoint;
    }

    @PostConstruct
    public void init() {
        FileUploadUtil.init(ossClient, bucketName, endpoint);
    }
}
