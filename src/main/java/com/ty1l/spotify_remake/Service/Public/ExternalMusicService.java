package com.ty1l.spotify_remake.Service.Public;

import com.ty1l.spotify_remake.Entity.Public.ExternalTrackVO;

import java.util.List;

public interface ExternalMusicService {

    List<ExternalTrackVO> search(String keyword);

    String getRealStreamUrl(String source, String trackId);
}
