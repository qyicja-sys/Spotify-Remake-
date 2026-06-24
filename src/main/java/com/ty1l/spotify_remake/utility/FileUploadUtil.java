package com.ty1l.spotify_remake.utility;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class FileUploadUtil {

    private static final String COVER_DIR = "D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/src/main/resources/static/datas/musicResouces/music_cover/songs/";
    private static final String MUSIC_DIR = "D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/src/main/resources/static/datas/musicResouces/musics/";
    private static final String PLAYLIST_COVER_DIR = "D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/src/main/resources/static/datas/musicResouces/music_cover/daliy/";
    private static final String ARTIST_AVATAR_DIR = "D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/src/main/resources/static/datas/profilePic/artists/";
    private static final String USER_PROFILE_DIR = "D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/src/main/resources/static/datas/profilePic/";
    private static final String PERSONAL_PLAYLIST_DIR = "D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/src/main/resources/static/datas/musicResouces/music_cover/personal/";
    private static final String ALBUM_COVER_DIR = "D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/src/main/resources/static/datas/musicResouces/music_cover/albums/";

    /**
     * 保存歌单封面图片，使用管理员指定的文件名
     */
    public static String savePlaylistCover(MultipartFile file, String customName) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        Path dir = Paths.get(PLAYLIST_COVER_DIR);
        Files.createDirectories(dir);
        String fileName = sanitize(customName) + ext;
        Path target = dir.resolve(fileName);
        if (Files.exists(target)) {
            fileName = sanitize(customName) + "_" + UUID.randomUUID().toString().substring(0, 6) + ext;
            target = dir.resolve(fileName);
        }

        file.transferTo(target.toFile());
        return "/static/datas/musicResouces/music_cover/daliy/" + fileName;
    }

    /**
     * 保存封面图片，文件名格式：歌手名 - 歌曲名.ext
     */
    public static String saveCover(MultipartFile file, String artistName, String songTitle) throws IOException {
        return saveFile(file, COVER_DIR, artistName, songTitle,
                "/static/datas/musicResouces/music_cover/songs/");
    }

    /**
     * 保存音频文件，文件名格式：歌手名 - 歌曲名.ext
     */
    public static String saveMusic(MultipartFile file, String artistName, String songTitle) throws IOException {
        return saveFile(file, MUSIC_DIR, artistName, songTitle,
                "/static/datas/musicResouces/musics/");
    }

    private static String saveFile(MultipartFile file, String dirPath,
                                   String artistName, String songTitle,
                                   String urlPrefix) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        String baseName;
        if (artistName != null && !artistName.isBlank()) {
            baseName = sanitize(artistName) + " - " + sanitize(songTitle);
        } else {
            baseName = sanitize(songTitle);
        }

        // 如果同名文件已存在，加随机后缀避免覆盖
        Path dir = Paths.get(dirPath);
        Files.createDirectories(dir);
        String fileName = baseName + ext;
        Path target = dir.resolve(fileName);
        if (Files.exists(target)) {
            fileName = baseName + "_" + UUID.randomUUID().toString().substring(0, 6) + ext;
            target = dir.resolve(fileName);
        }

        file.transferTo(target.toFile());
        return urlPrefix + fileName;
    }

    /**
     * 保存艺术家头像，同时复制一份到 user profilePic 目录
     * 文件名与艺术家 name 一致
     * @return 数组: [artistAvatarUrl, userProfilePicUrl]
     */
    public static String[] saveArtistAvatar(MultipartFile file, String artistName) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        String fileName = sanitize(artistName) + ext;

        // 保存到 artists 目录
        Path artistDir = Paths.get(ARTIST_AVATAR_DIR);
        Files.createDirectories(artistDir);
        Path artistTarget = artistDir.resolve(fileName);
        file.transferTo(artistTarget.toFile());

        // 复制到 user profilePic 目录
        Path userDir = Paths.get(USER_PROFILE_DIR);
        Files.createDirectories(userDir);
        Path userTarget = userDir.resolve(fileName);
        Files.copy(artistTarget, userTarget, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        String artistUrl = "/static/datas/profilePic/artists/" + fileName;
        String userUrl = "/static/datas/profilePic/" + fileName;
        return new String[]{artistUrl, userUrl};
    }

    /**
     * 保存用户头像，文件名为 [username].jpg
     * @return 相对路径，如 /static/datas/profilePic/username.jpg
     */
    public static String saveUserProfilePic(MultipartFile file, String username) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalName = file.getOriginalFilename();
        String ext = ".jpg";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        String fileName = sanitize(username) + ext;
        Path dir = Paths.get(USER_PROFILE_DIR);
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName);

        // 如果已存在同名文件，先删除再写入（覆盖旧头像）
        if (Files.exists(target)) {
            Files.delete(target);
        }

        file.transferTo(target.toFile());
        return "/static/datas/profilePic/" + fileName;
    }

    /**
     * 保存用户自建歌单封面，文件名为 [userId]_[playlistId].ext
     * @return 相对路径，如 /static/datas/musicResouces/music_cover/personal/1_2.jpg
     */
    public static String savePersonalPlaylistCover(MultipartFile file, Long userId, Integer playlistId) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalName = file.getOriginalFilename();
        String ext = ".jpg";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        String fileName = userId + "_" + playlistId + ext;
        Path dir = Paths.get(PERSONAL_PLAYLIST_DIR);
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName);

        // 覆盖旧封面
        if (Files.exists(target)) {
            Files.delete(target);
        }

        file.transferTo(target.toFile());
        return "/static/datas/musicResouces/music_cover/personal/" + fileName;
    }

    /**
     * 保存专辑封面图片，文件名格式：专辑名.ext
     */
    public static String saveAlbumCover(MultipartFile file, String albumName) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        Path dir = Paths.get(ALBUM_COVER_DIR);
        Files.createDirectories(dir);
        String fileName = sanitize(albumName) + ext;
        Path target = dir.resolve(fileName);
        if (Files.exists(target)) {
            fileName = sanitize(albumName) + "_" + UUID.randomUUID().toString().substring(0, 6) + ext;
            target = dir.resolve(fileName);
        }

        file.transferTo(target.toFile());
        return "/static/datas/musicResouces/music_cover/albums/" + fileName;
    }

    private static String sanitize(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[/\\\\:*?\"<>|]", "_").trim();
    }
}
