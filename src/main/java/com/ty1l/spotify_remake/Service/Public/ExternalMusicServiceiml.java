package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.ExternalTrackVO;
import com.ty1l.spotify_remake.Service.Public.External.GDMusicApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExternalMusicServiceiml implements ExternalMusicService {

    @Autowired
    private GDMusicApiClient gdMusicApiClient;

    @Override
    public List<ExternalTrackVO> search(String keyword) {
        return gdMusicApiClient.search(keyword);
    }

    @Override
    public String getRealStreamUrl(String source, String trackId) {
        if ("gdmusic".equals(source)) {
            return gdMusicApiClient.getRealStreamUrl(trackId);
        }
        return null;
    }
}
