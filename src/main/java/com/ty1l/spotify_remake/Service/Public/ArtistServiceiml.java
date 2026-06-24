package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.Artist;
import com.ty1l.spotify_remake.Mapper.Public.ArtistMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArtistServiceiml implements ArtistService {

    @Autowired
    private ArtistMapper artistMapper;

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
    }

    @Override
    public void update(Artist artist) {
        artistMapper.update(artist);
    }

    @Override
    public void delete(Integer id) {
        artistMapper.deleteById(id);
    }
}
