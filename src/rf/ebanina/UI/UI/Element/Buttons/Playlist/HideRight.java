package rf.ebanina.UI.UI.Element.Buttons.Playlist;

import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Element.Buttons.Button;

public class HideRight extends Button {
    public HideRight() {
        super(ResourceManager.Instance.loadSVG("hideRightButton"));

        setCornerRadius(5);
        setSize(25, 25);
    }
}
