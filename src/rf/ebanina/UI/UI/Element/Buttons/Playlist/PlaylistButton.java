package rf.ebanina.UI.UI.Element.Buttons.Playlist;

import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Element.Buttons.Button;

public class PlaylistButton extends Button {
    public PlaylistButton() {
        super(ResourceManager.Instance.loadSVG("playlistButton"));
    }
}