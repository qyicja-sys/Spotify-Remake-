package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.Artist;

import java.util.List;

public interface ArtistService {
    List<Artist> findAll();

    Artist findById(Integer id);

    Artist findByName(String name);

    List<Artist> findByNameSearch(String name);

    void add(Artist artist);

    void update(Artist artist);

    void delete(Integer id);
}
