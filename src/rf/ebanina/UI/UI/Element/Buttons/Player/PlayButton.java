package rf.ebanina.UI.UI.Element.Buttons.Player;

import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Element.Buttons.Button;

public class PlayButton extends Button {
    public PlayButton() {
        super(ResourceManager.Instance.loadSVG("playButton"));
    }
}