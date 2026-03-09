package rf.ebanina.UI.UI.Element.Buttons.Playlist;

import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.UI.Element.Buttons.Button;

public class HideLeft extends Button {
    public HideLeft() {
        super(ResourceManager.Instance.loadSVG("hideLeftButton"));

        setCornerRadius(5);
        setSize(25, 25);
    }
}
