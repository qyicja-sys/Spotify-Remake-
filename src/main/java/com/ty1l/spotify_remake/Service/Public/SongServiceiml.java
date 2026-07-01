package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.Song;
import com.ty1l.spotify_remake.Mapper.Public.SongMapper;
import com.ty1l.spotify_remake.Service.BloomFilterService;
import com.ty1l.spotify_remake.Service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SongServiceiml implements SongService {

    private static final Logger log = LoggerFactory.getLogger(SongServiceiml.class);

    @Autowired
    private SongMapper songMapper;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private BloomFilterService bloomFilterService;

    @Override
    public List<Song> findAll() {
        return songMapper.findAll();
    }

    @Override
    public Song findById(Integer id) {
        // 布隆过滤器：id 绝对不存在则直接返回 null
        if (!bloomFilterService.songExists(id)) {
            log.debug("Bloom filter rejected song id={}", id);
            return null;
        }
        String key = String.format(CacheService.KEY_SONG, id);
        Song cached = cacheService.get(key, Song.class);
        if (cached != null) {
            log.debug("Song cache hit for id={}", id);
            return cached;
        }
        Song song = songMapper.findById(id);
        if (song != null) {
            cacheService.set(key, song);
        }
        return song;
    }

    @Override
    public List<Song> findByTitle(String title) {
        return songMapper.findByTitle(title);
    }

    @Override
    public List<Song> findByArtistId(Integer artistId) {
        return songMapper.findByArtistId(artistId);
    }

    @Override
    public void add(Song song) {
        songMapper.insert(song);
        // 新增歌曲后同步到布隆过滤器，使 ID 立即可查
        if (song.getId() != null) {
            bloomFilterService.addSong(song.getId());
        }
    }

    @Override
    public void update(Song song) {
        songMapper.update(song);
        // 更新后使缓存失效
        cacheService.delete(String.format(CacheService.KEY_SONG, song.getId()));
    }

    @Override
    public void delete(Integer id) {
        songMapper.deleteById(id);
        // 删除后使缓存失效
        cacheService.delete(String.format(CacheService.KEY_SONG, id));
    }
}
