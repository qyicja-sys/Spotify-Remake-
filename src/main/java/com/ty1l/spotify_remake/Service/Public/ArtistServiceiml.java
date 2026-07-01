package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.Artist;
import com.ty1l.spotify_remake.Entity.Public.SearchSongVO;
import com.ty1l.spotify_remake.Mapper.Public.ArtistMapper;
import com.ty1l.spotify_remake.Mapper.Public.SongMapper;
import com.ty1l.spotify_remake.Service.BloomFilterService;
import com.ty1l.spotify_remake.Service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ArtistServiceiml implements ArtistService {

    private static final Logger log = LoggerFactory.getLogger(ArtistServiceiml.class);

    @Autowired
    private ArtistMapper artistMapper;

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private BloomFilterService bloomFilterService;

    @Autowired
    private MonthlyListenersService monthlyListenersService;

    @Override
    public List<Artist> findAll() {
        return artistMapper.findAll();
    }

    @Override
    public Artist findById(Integer id) {
        return artistMapper.findById(id);
    }

    @Override
    public Artist findByName(String name) {
        return artistMapper.findByName(name);
    }

    @Override
    public List<Artist> findByNameSearch(String name) {
        return artistMapper.findByNameSearch(name);
    }

    @Override
    public void add(Artist artist) {
        artistMapper.insert(artist);
        if (artist.getId() != null) {
            bloomFilterService.addArtist(artist.getId());
        }
    }

    @Override
    public void update(Artist artist) {
        artistMapper.update(artist);
    }

    @Override
    public void delete(Integer id) {
        artistMapper.deleteById(id);
    }

    @Override
    public List<Artist> resolveOrCreateArtists(String[] names) {
        List<Artist> result = new ArrayList<>();
        for (String raw : names) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) continue;
            Artist artist = artistMapper.findByName(trimmed);
            if (artist == null) {
                artist = new Artist();
                artist.setName(trimmed);
                artistMapper.insert(artist);
            }
            result.add(artist);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getArtistDetail(Integer id) {
        // 布隆过滤器：id 绝对不存在则直接返回 null
        if (!bloomFilterService.artistExists(id)) {
            log.debug("Bloom filter rejected artist id={}", id);
            return null;
        }

        String cacheKey = String.format(CacheService.KEY_ARTIST, id);

        // 尝试从缓存读取
        Map<String, Object> cached = cacheService.get(cacheKey, Map.class);
        if (cached != null) {
            log.debug("Artist cache hit for id={}", id);
            return cached;
        }

        log.info("Artist cache miss for id={}, rebuilding...", id);

        Artist artist = artistMapper.findById(id);
        if (artist == null) return null;

        List<SearchSongVO> songs = songMapper.findByArtistIdWithNames(id);
        if (songs == null) songs = Collections.emptyList();

        // 月听众数 = max(Redis实时值, DB兜底值)，保证刚播放就可见
        int monthlyListeners = monthlyListenersService.getMonthlyListeners(id);

        Map<String, Object> detail = Map.of(
                "artist", artist,
                "songs", songs,
                "monthlyListeners", monthlyListeners
        );

        cacheService.set(cacheKey, detail);
        return detail;
    }

}
