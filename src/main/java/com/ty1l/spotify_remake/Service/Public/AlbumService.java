package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.Album;

import java.util.List;

public interface AlbumService {

    List<Album> findAll();

    Album findById(Long id);

    List<Album> findByArtistId(Long artistId);

    List<Album> findByNameSearch(String name);

    void add(Album album);

    void update(Album album);

    void delete(Long id);
}
