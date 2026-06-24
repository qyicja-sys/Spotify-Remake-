package com.ty1l.spotify_remake.Controller.Public;

import com.ty1l.spotify_remake.Entity.Public.Artist;
import com.ty1l.spotify_remake.Entity.Public.SearchSongVO;
import com.ty1l.spotify_remake.Entity.Public.SearchVo;
import com.ty1l.spotify_remake.Mapper.Public.SongMapper;
import com.ty1l.spotify_remake.Service.Public.ArtistService;
import com.ty1l.spotify_remake.utility.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/spotify")
public class SearchController {
    @Autowired
    private SongMapper songMapper;
    @Autowired
    private ArtistService artistService;

    @GetMapping("/search")
    public Result search(@RequestParam("title") String title) {
        List<SearchSongVO> songs = songMapper.searchWithTitle(title);
        List<Artist> artists = artistService.findByNameSearch(title);
        if(songs.isEmpty() && artists.isEmpty()) {
            return Result.errorClient("No results found");
        }
        SearchVo searchVo = new SearchVo(songs, artists);
        return Result.success(searchVo);
    }
}
