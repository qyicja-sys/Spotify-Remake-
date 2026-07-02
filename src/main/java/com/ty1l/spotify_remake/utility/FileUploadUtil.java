package com.ty1l.spotify_remake.utility;

import com.aliyun.oss.OSS;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 文件上传工具类 — 所有上传走阿里云 OSS。
 * 原本地文件存储实现已禁用。
 */
public class FileUploadUtil {

    private static OSS ossClient;
    private static String bucketName;
    private static String endpoint;

    /**
     * 由 Spring 容器在 Bean 初始化后注入（通过 OssConfig 调用）
     */
    public static void init(OSS oss, String bucket, String ep) {
        ossClient = oss;
        bucketName = bucket;
        endpoint = ep;
    }

    private static String buildOssUrl(String objectKey) {
        // https://<bucket>.<endpoint-host>/<objectKey>
        String host = endpoint.replace("https://", "").replace("http://", "");
        return "https://" + bucketName + "." + host + "/" + objectKey;
    }

    // ──────────────────────────────────────────────
    // OSS 目录前缀
    // ──────────────────────────────────────────────
    private static final String PREFIX_COVER_SONGS    = "datas/musicResouces/music_cover/songs/";
    private static final String PREFIX_MUSIC          = "datas/musicResouces/musics/";
    private static final String PREFIX_COVER_PLAYLIST = "datas/musicResouces/music_cover/daliy/";
    private static final String PREFIX_COVER_ALBUM    = "datas/musicResouces/music_cover/albums/";
    private static final String PREFIX_COVER_PERSONAL = "datas/musicResouces/music_cover/personal/";
    private static final String PREFIX_AVATAR_ARTIST  = "datas/profilePic/artists/";
    private static final String PREFIX_AVATAR_USER    = "datas/profilePic/";

    // ═══════════════════════════════════════════════
    // 公开方法（签名不变，控制器无需修改）
    // ═══════════════════════════════════════════════

    /**
     * 上传歌单封面
     */
    public static String savePlaylistCover(MultipartFile file, String customName) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String ext = getExtension(file.getOriginalFilename());
        String objectKey = PREFIX_COVER_PLAYLIST + sanitize(customName) + ext;
        objectKey = avoidOverwrite(objectKey);
        upload(file, objectKey);
        return buildOssUrl(objectKey);
    }

    /**
     * 上传歌曲封面
     */
    public static String saveCover(MultipartFile file, String artistName, String songTitle) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String objectKey = buildSongKey(PREFIX_COVER_SONGS, file.getOriginalFilename(), artistName, songTitle);
        upload(file, objectKey);
        return buildOssUrl(objectKey);
    }

    /**
     * 上传音乐文件
     */
    public static String saveMusic(MultipartFile file, String artistName, String songTitle) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String objectKey = buildSongKey(PREFIX_MUSIC, file.getOriginalFilename(), artistName, songTitle);
        upload(file, objectKey);
        return buildOssUrl(objectKey);
    }

    /**
     * 上传艺术家头像，同时存一份到用户头像目录。
     * @return [artistAvatarUrl, userProfilePicUrl]
     */
    public static String[] saveArtistAvatar(MultipartFile file, String artistName) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String ext = getExtension(file.getOriginalFilename());
        String fileName = sanitize(artistName) + ext;

        // artists 目录
        String artistKey = PREFIX_AVATAR_ARTIST + fileName;
        upload(file, artistKey);

        // users 目录（复制同一份）
        String userKey = PREFIX_AVATAR_USER + fileName;
        try (InputStream in = file.getInputStream()) {
            ossClient.putObject(bucketName, userKey, in);
        }

        return new String[]{ buildOssUrl(artistKey), buildOssUrl(userKey) };
    }

    /**
     * 上传用户头像，文件名为 [username].jpg
     */
    public static String saveUserProfilePic(MultipartFile file, String username) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String ext = getExtension(file.getOriginalFilename());
        if (ext.isEmpty()) ext = ".jpg";
        String objectKey = PREFIX_AVATAR_USER + sanitize(username) + ext;
        upload(file, objectKey); // 覆盖旧头像
        return buildOssUrl(objectKey);
    }

    /**
     * 上传用户自建歌单封面
     */
    public static String savePersonalPlaylistCover(MultipartFile file, Long userId, Integer playlistId) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String ext = getExtension(file.getOriginalFilename());
        if (ext.isEmpty()) ext = ".jpg";
        String objectKey = PREFIX_COVER_PERSONAL + userId + "_" + playlistId + ext;
        upload(file, objectKey); // 覆盖旧封面
        return buildOssUrl(objectKey);
    }

    /**
     * 上传专辑封面
     */
    public static String saveAlbumCover(MultipartFile file, String albumName) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String ext = getExtension(file.getOriginalFilename());
        String objectKey = PREFIX_COVER_ALBUM + sanitize(albumName) + ext;
        objectKey = avoidOverwrite(objectKey);
        upload(file, objectKey);
        return buildOssUrl(objectKey);
    }

    // ═══════════════════════════════════════════════
    // 内部工具方法
    // ═══════════════════════════════════════════════

    private static void upload(MultipartFile file, String objectKey) throws IOException {
        try (InputStream in = file.getInputStream()) {
            ossClient.putObject(bucketName, objectKey, in);
        }
    }

    private static String buildSongKey(String prefix, String originalName, String artistName, String songTitle) {
        String ext = getExtension(originalName);
        String baseName;
        if (artistName != null && !artistName.isBlank()) {
            baseName = sanitize(artistName) + " - " + sanitize(songTitle);
        } else {
            baseName = sanitize(songTitle);
        }
        return prefix + baseName + ext;
    }

    /**
     * 如果 OSS 上已存在同名对象，加随机后缀避免覆盖
     */
    private static String avoidOverwrite(String objectKey) {
        if (ossClient.doesObjectExist(bucketName, objectKey)) {
            int dot = objectKey.lastIndexOf('.');
            String base = dot > 0 ? objectKey.substring(0, dot) : objectKey;
            String ext  = dot > 0 ? objectKey.substring(dot) : "";
            objectKey = base + "_" + UUID.randomUUID().toString().substring(0, 6) + ext;
        }
        return objectKey;
    }

    private static String getExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }

    /**
     * 将用户头像复制到艺术家头像目录（OSS 上复制对象）。
     * 如果 URL 是 OSS 地址，在 OSS 内复制；如果是旧本地路径，尝试从本地文件上传到 OSS。
     * @return 艺术家头像 OSS URL
     */
    public static String copyAvatarToArtistDir(String existingUrl) {
        if (existingUrl == null || existingUrl.isBlank()) return null;
        if (existingUrl.contains("/avatars/artists/")) return existingUrl;

        String sourceKey = extractOssKey(existingUrl);
        if (sourceKey != null) {
            // OSS URL → OSS 内复制
            String fileName = sourceKey.substring(sourceKey.lastIndexOf('/') + 1);
            String destKey = PREFIX_AVATAR_ARTIST + fileName;
            try {
                ossClient.copyObject(bucketName, sourceKey, bucketName, destKey);
                return buildOssUrl(destKey);
            } catch (Exception e) {
                // 复制失败，回退原 URL
                return existingUrl;
            }
        }
        // 旧本地路径 → 返回原路径（本地文件 + WebConfig 仍能访问）
        return existingUrl;
    }

    /**
     * 判断 URL 是否是当前 OSS bucket 的地址，是则提取 objectKey
     */
    private static String extractOssKey(String url) {
        if (url == null) return null;
        String host = endpoint.replace("https://", "").replace("http://", "");
        String prefix = "https://" + bucketName + "." + host + "/";
        if (url.startsWith(prefix)) {
            return url.substring(prefix.length());
        }
        // 也兼容不带协议的短格式
        prefix = bucketName + "." + host + "/";
        if (url.contains(prefix)) {
            return url.substring(url.indexOf(prefix) + prefix.length());
        }
        return null;
    }

    private static String sanitize(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[/\\\\:*?\"<>|]", "_").trim();
    }

    // ═══════════════════════════════════════════════════════════════
    // 原本地文件存储实现 — 已禁用
    // 如需回退，取消以下注释并恢复旧方法签名
    // ═══════════════════════════════════════════════════════════════
    //
    // private static final String COVER_DIR = "D:/javaedit/.../music_cover/songs/";
    // private static final String MUSIC_DIR = "D:/javaedit/.../musics/";
    // ... (旧实现已移除，见 git 历史 004c52e)
}
