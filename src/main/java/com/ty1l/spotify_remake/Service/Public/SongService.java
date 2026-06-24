package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.Song;

import java.util.List;
import java.util.Map;

public interface SongService {

    List<Song> findAll();

    Song findById(Integer id);

    List<Song> findByTitle(String title);

    List<Song> findByArtistId(Integer artistId);

    void add(Song song);

    void update(Song song);

    void delete(Integer id);
}
