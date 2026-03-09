package rf.ebanina.UI.UI.Element.Buttons.Player;

import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Element.Buttons.Button;

public class NextButton extends Button {
    public NextButton() {
        super(ResourceManager.Instance.loadSVG("nextButton"));
    }
}