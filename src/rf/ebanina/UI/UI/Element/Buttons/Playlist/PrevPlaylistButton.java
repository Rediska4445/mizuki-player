package rf.ebanina.UI.UI.Element.Buttons.Playlist;

import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Element.Buttons.Button;

public class PrevPlaylistButton extends Button {
    public PrevPlaylistButton() {
        super(ResourceManager.Instance.loadSVG("prevPlaylistButton"));
    }
}