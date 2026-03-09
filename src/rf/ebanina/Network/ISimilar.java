package rf.ebanina.Network;

import rf.ebanina.ebanina.Player.Track;

import java.util.List;

public interface ISimilar {
    void updateSimilar(Track track);
    List<Track> getSimilar(String f);
}
