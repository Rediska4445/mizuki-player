package rf.ebanina.ebanina.Player.Controllers.Playlist;

import rf.ebanina.ebanina.Player.Track;

public interface IPlayProcessor
        extends IProcessor
{
    void open(Track newValue);
}
