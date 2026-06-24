package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.Album;
import com.ty1l.spotify_remake.Mapper.Public.AlbumMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlbumServiceiml implements AlbumService {

    @Autowired
    private AlbumMapper albumMapper;

    @Override
    public List<Album> findAll() {
        return albumMapper.findAll();
    }

    @Override
    public Album findById(Long id) {
        return albumMapper.findById(id);
    }

    @Override
    public List<Album> findByArtistId(Long artistId) {
        return albumMapper.findByArtistId(artistId);
    }

    @Override
    public List<Album> findByNameSearch(String name) {
        return albumMapper.findByNameSearch(name);
    }

    @Override
    public void add(Album album) {
        albumMapper.insert(album);
    }

    @Override
    public void update(Album album) {
        albumMapper.update(album);
    }

    @Override
    public void delete(Long id) {
        albumMapper.deleteById(id);
    }
}
