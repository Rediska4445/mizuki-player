package rf.ebanina.UI.UI.Element.Buttons.Player;

import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Element.Buttons.Button;

public class PauseButton extends Button {
    public PauseButton() {
        super(ResourceManager.Instance.loadSVG("pauseButton"));
    }
}
