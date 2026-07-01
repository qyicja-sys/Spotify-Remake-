package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.Album;
import com.ty1l.spotify_remake.Mapper.Public.AlbumMapper;
import com.ty1l.spotify_remake.Service.BloomFilterService;
import com.ty1l.spotify_remake.Service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlbumServiceiml implements AlbumService {

    private static final Logger log = LoggerFactory.getLogger(AlbumServiceiml.class);

    @Autowired
    private AlbumMapper albumMapper;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private BloomFilterService bloomFilterService;

    @Override
    public List<Album> findAll() {
        return albumMapper.findAll();
    }

    @Override
    public Album findById(Long id) {
        // 布隆过滤器：id 绝对不存在则直接返回 null
        if (!bloomFilterService.albumExists(id)) {
            log.debug("Bloom filter rejected album id={}", id);
            return null;
        }
        String key = String.format(CacheService.KEY_ALBUM, id);
        Album cached = cacheService.get(key, Album.class);
        if (cached != null) {
            log.debug("Album cache hit for id={}", id);
            return cached;
        }
        Album album = albumMapper.findById(id);
        if (album != null) {
            cacheService.set(key, album);
        }
        return album;
    }

    @Override
    public List<Album> findByArtistId(Long artistId) {
        String key = String.format(CacheService.KEY_ALBUM_BY_ARTIST, artistId);
        List<Album> cached = cacheService.getList(key, Album.class);
        if (cached != null) {
            log.debug("Album-by-artist cache hit for artistId={}", artistId);
            return cached;
        }
        List<Album> albums = albumMapper.findByArtistId(artistId);
        if (albums != null) {
            cacheService.set(key, albums);
        }
        return albums;
    }

    @Override
    public List<Album> findByNameSearch(String name) {
        return albumMapper.findByNameSearch(name);
    }

    @Override
    public void add(Album album) {
        albumMapper.insert(album);
        if (album.getId() != null) {
            bloomFilterService.addAlbum(album.getId());
        }
    }

    @Override
    public void update(Album album) {
        albumMapper.update(album);
        // 更新后使缓存失效
        evictAlbumCache(album.getId());
    }

    @Override
    public void delete(Long id) {
        // 删除前先查出来以便清理关联缓存
        Album album = albumMapper.findById(id);
        albumMapper.deleteById(id);
        if (album != null) {
            evictAlbumCache(id);
            if (album.getArtistId() != null) {
                cacheService.delete(String.format(CacheService.KEY_ALBUM_BY_ARTIST, album.getArtistId()));
            }
        }
    }

    /** 使单个专辑缓存失效 */
    private void evictAlbumCache(Long albumId) {
        cacheService.delete(String.format(CacheService.KEY_ALBUM, albumId));
    }
}
