package com.ty1l.spotify_remake.Controller.Public;

import com.ty1l.spotify_remake.Entity.User.PlaylistDetailVO;
import com.ty1l.spotify_remake.Service.Public.PlaylistService;
import com.ty1l.spotify_remake.utility.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/spotify/playlist")
public class PlaylistController {

    @Autowired
    private PlaylistService playlistService;

    @GetMapping("/{id}")
    public Result getDetail(@PathVariable Integer id) {
        PlaylistDetailVO detail = playlistService.findDetailById(id);
        if (detail == null) {
            return Result.errorClient("Playlist not found");
        }
        return Result.success(detail);
    }
}
