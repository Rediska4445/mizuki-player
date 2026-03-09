package rf.ebanina.UI.UI.Element.Buttons;

import rf.ebanina.File.Resources.ResourceManager;

public class Commons extends Button {
    public Commons() {
        super(ResourceManager.Instance.loadSVG("commons"));

        setCornerRadius(5);
    }
}
