package rf.ebanina.UI.UI.Element.Buttons.Player;

import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Element.Buttons.Button;

public class PrevButton extends Button {
    public PrevButton() {
        super(ResourceManager.Instance.loadSVG("prevButton"));
    }
}
