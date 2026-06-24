package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.Song;
import com.ty1l.spotify_remake.Mapper.Public.SongMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SongServiceiml implements SongService {
    @Autowired
    private SongMapper songMapper;


    @Override
    public List<Song> findAll() {
        return songMapper.findAll();
    }

    @Override
    public Song findById(Integer id) {
        return songMapper.findById(id);
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
    }

    @Override
    public void update(Song song) {
        songMapper.update(song);
    }

    @Override
    public void delete(Integer id) {
        songMapper.deleteById(id);
    }
}
