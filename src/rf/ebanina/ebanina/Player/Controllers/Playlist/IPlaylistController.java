package rf.ebanina.ebanina.Player.Controllers.Playlist;

import java.io.IOException;

/**
 * Интерфейс процессора плейлистов.
 * <p>
 * Расширяет базовый {@link IController} функциональностью работы с файлами плейлистов.
 * </p>
 *
 * @implNote Реализация загружает плейлист
 * @see PlaylistController
 */
public interface IPlaylistController
        extends IController
{
    /**
     * Загружает плейлист.
     *
     * @param path абсолютный путь к файлу плейлиста
     * @throws IllegalArgumentException если путь недействителен или формат не поддерживается
     * @throws IOException при ошибках чтения файла
     */
    void setPlaylist(String path);
}
