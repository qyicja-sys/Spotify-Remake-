package com.ty1l.spotify_remake.Controller.Public;

import com.ty1l.spotify_remake.Entity.Public.Artist;
import com.ty1l.spotify_remake.Entity.User.User;
import com.ty1l.spotify_remake.Mapper.User.UserMapper;
import com.ty1l.spotify_remake.Service.Public.ArtistService;
import com.ty1l.spotify_remake.utility.FileUploadUtil;
import com.ty1l.spotify_remake.utility.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@Slf4j
@RestController
@RequestMapping("/admin/spotify/artists")
public class ArtistController {

    @Autowired
    private ArtistService artistService;

    @Autowired
    private UserMapper userMapper;

    @GetMapping
    public Result list() {
        syncArtistUsers();
        List<Artist> artists = artistService.findAll();
        return Result.success(artists);
    }

    /**
     * 检查 user 表中 is_artist=1 的用户，如果其 nickName 不在 artists 表中则自动添加
     */
    private void syncArtistUsers() {
        List<User> artistUsers = userMapper.findArtistUsers();
        for (User user : artistUsers) {
            if (user.getNickName() == null || user.getNickName().isBlank()) continue;
            Artist existing = artistService.findByName(user.getNickName().trim());
            if (existing == null) {
                Artist newArtist = new Artist();
                newArtist.setName(user.getNickName().trim());
                newArtist.setAvatarUrl(copyProfilePicToArtists(user.getProfilePic()));
                artistService.add(newArtist);
                log.info("Auto-synced artist from user: {} (userId={})", user.getNickName(), user.getId());
            }
        }
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Artist artist = artistService.findById(id);
        return Result.success(artist);
    }

    @PostMapping
    public Result add(@RequestBody Artist artist) {
        log.info("Add artist: {}", artist);
        artistService.add(artist);
        return Result.success("Artist added successfully");
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody Artist artist) {
        artist.setId(id);
        log.info("Update artist: {}", artist);
        artistService.update(artist);
        return Result.success("Artist updated successfully");
    }

    @PostMapping("/upload-avatar")
    public Result uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            @RequestParam("artistName") String artistName) {
        try {
            String[] urls = FileUploadUtil.saveArtistAvatar(file, artistName.trim());
            if (urls == null) return Result.error("上传文件为空");

            // 同步到 user 表
            userMapper.updateProfilePicByNickName(artistName.trim(), urls[1]);
            log.info("Uploaded artist avatar: artistUrl={}, userUrl={}", urls[0], urls[1]);

            Map<String, String> result = new HashMap<>();
            result.put("artistAvatarUrl", urls[0]);
            result.put("userProfilePicUrl", urls[1]);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Artist avatar upload failed", e);
            return Result.error("头像上传失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        log.info("Delete artist id: {}", id);
        artistService.delete(id);
        return Result.success("Artist deleted successfully");
    }

    /**
     * 复制用户头像文件到 artists 目录，返回 artists 的 URL
     */
    private String copyProfilePicToArtists(String profilePic) {
        if (profilePic == null || profilePic.isBlank()) return null;
        if (profilePic.contains("/profilePic/artists/")) return profilePic;

        try {
            String relativePath = profilePic.startsWith("/") ? profilePic.substring(1) : profilePic;
            Path source = Paths.get("src/main/resources", relativePath);
            if (!Files.exists(source)) {
                source = Paths.get("D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/src/main/resources", relativePath);
            }
            if (!Files.exists(source)) {
                return profilePic;
            }

            String fileName = source.getFileName().toString();
            Path targetDir = Paths.get("D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/src/main/resources/static/datas/profilePic/artists/");
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(fileName);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            return "/static/datas/profilePic/artists/" + fileName;
        } catch (Exception e) {
            log.error("Failed to copy profile pic to artists dir: {}", profilePic, e);
            return profilePic;
        }
    }
}
