package rf.ebanina.ebanina.Player.Controllers.Playlist;

import rf.ebanina.ebanina.Player.Track;

/**
 * Контроллер воспроизведения отдельных треков.
 * <p>
 * Расширяет {@link IController} возможностью прямого открытия конкретного трека.
 * </p>
 * @see PlaylistController
 */
public interface IPlayController
        extends IController
{
    /**
     * Открывает конкретный трек для воспроизведения.
     *
     * @param newValue трек для воспроизведения
     */
    void open(Track newValue);
}
