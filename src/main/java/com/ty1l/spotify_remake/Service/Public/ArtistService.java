package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.Artist;

import java.util.List;
import java.util.Map;

public interface ArtistService {
    List<Artist> findAll();

    Artist findById(Integer id);

    Artist findByName(String name);

    List<Artist> findByNameSearch(String name);

    void add(Artist artist);

    void update(Artist artist);

    void delete(Integer id);

    /**
     * Resolve or create artists by name.
     * For each name, findByName first, then insert if not found.
     * Returns list of Artist objects corresponding to the input names.
     */
    List<Artist> resolveOrCreateArtists(String[] names);

    /**
     * 获取艺术家详情（含歌曲列表、月听众数），带 Redis 缓存
     */
    Map<String, Object> getArtistDetail(Integer id);

}
