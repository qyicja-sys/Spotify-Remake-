package com.ty1l.spotify_remake.Controller.Public;

import com.ty1l.spotify_remake.Entity.Public.LeaderboardSongVO;
import com.ty1l.spotify_remake.Service.Public.LeaderboardService;
import com.ty1l.spotify_remake.utility.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 排行榜控制器 — 全球歌曲排行榜
 */
@RestController
@RequestMapping("/spotify/leaderboard")
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    /**
     * 获取全球 Top 50 排行榜
     *
     * @return 排行榜歌曲列表（排名 1-50，含歌名、艺人、封面、播放量）
     */
    @GetMapping("/global")
    public Result getGlobalTop50() {
        List<LeaderboardSongVO> songs = leaderboardService.getGlobalTop50();
        Map<String, Object> data = new HashMap<>();
        data.put("songs", songs);
        data.put("total", songs.size());
        data.put("updatedAt", System.currentTimeMillis());
        return Result.success(data);
    }
}
