package rf.ebanina.ebanina.Player.Controllers.Playlist;

import javafx.application.Platform;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;
import rf.ebanina.ebanina.Player.Playlist;
import rf.ebanina.ebanina.Player.Track;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Field;
import rf.ebanina.File.FileManager;
import rf.ebanina.File.Resources.Resources;
import rf.ebanina.UI.Root;
import rf.ebanina.utils.loggining.logging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <h1>PlaylistController</h1>
 * Контроллер управления плейлистами музыкального плеера.
 * <p>
 * Класс отвечает за переключение между плейлистами (папками с музыкой), навигацию по трекам,
 * сохранение/восстановление позиций воспроизведения и синхронизацию UI с состоянием плеера.
 * </p>
 *
 * <h3>Основная концепция</h3>
 * <p>
 * Плейлист = <b>папка с файлами</b>. Переключение плейлистов = смена папки.
 * Состояние (текущий трек, текущий плейлист) сохраняется в кэш-файлах в директории
 * {@link Resources.Properties#DEFAULT_PLAYLIST_CACHE_PATHS}.
 * </p>
 *
 * <h3>Ключевые возможности</h3>
 * <ul>
 *   <li><b>Переключение плейлистов</b>: загрузка треков из новой папки, восстановление позиции.</li>
 *   <li><b>Навигация</b>: next/prev трек с циклическим переходом по концам плейлиста.</li>
 *   <li><b>Автовыбор трека</b>: при смене плейлиста может найти и выбрать текущий трек
 *       (если включена опция {@code playlist_track_auto_select_from_current}).</li>
 *   <li><b>Синхронизация UI</b>: обновление {@link Root#tracksListView} в FX-потоке.</li>
 *   <li><b>Потокобезопасность</b>: {@link #setPlaylist(String)} синхронизирован через
 *       {@link ExecutorService} с single thread.</li>
 * </ul>
 *
 * <h3>Жизненный цикл</h3>
 * <ul>
 *   <li>Статический экземпляр {@link #playlistController} создаётся при первом обращении.</li>
 *   <li>При смене плейлиста сохраняется позиция в старом кэше и восстанавливается из нового.</li>
 *   <li>UI обновляется асинхронно через {@link Platform#runLater(Runnable)}.</li>
 * </ul>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * // Переключение на следующий трек
 * PlaylistController.playlistController.next();
 *
 * // Смена плейлиста (папки)
 * PlaylistController.playlistController.setPlaylist("/music/rock");
 *
 * // Обновление UI при смене плейлиста
 * PlaylistController._refreshPlaylist();
 * }</pre>
 *
 * <h3>Зависимости</h3>
 * <ul>
 *   <li>{@link PlayProcessor}: хранит текущее состояние (tracks, currentMusicDir, итераторы).</li>
 *   <li>{@link FileManager}: загрузка треков из папки, чтение/запись кэша.</li>
 *   <li>{@link Root#tracksListView}: UI компонент для отображения треков.</li>
 * </ul>
 *
 * <h3>Ограничения</h3>
 * <ul>
 *   <li>Статический синглтон — не потокобезопасен для конкурентного доступа.</li>
 *   <li>Методы {@link #next()} и {@link #down()} не проверяют пустой плейлист.</li>
 *   <li>Автовыбор трека сравнивает только title+artist (case-insensitive).</li>
 *   <li>Исключения IOException оборачиваются в RuntimeException без логирования.</li>
 * </ul>
 *
 * @author Ebanina Std
 * @since 0.1.0.x
 * @see PlayProcessor
 * @see FileManager
 * @see IPlaylistProcessor
 * @see Root#tracksListView
 */
@logging(tag = "playlist controller", isActive = false)
public class PlaylistController
        implements IPlaylistProcessor
{
    /**
     * Единственный глобальный экземпляр контроллера плейлистов.
     * <p>
     * Гарантирует, что все части приложения работают с одной и той же инстанцией.
     * Инициализируется лениво при первом обращении к полю.
     * </p>
     * <p>
     * <b>Использование:</b>
     * {@code PlaylistController.playlistController.next();}
     * </p>
     */
    public static PlaylistController playlistController = new PlaylistController();

    /**
     * Callback, выполняемый при смене плейлиста.
     * <p>
     * <b>Последовательность действий:</b>
     * <ol>
     *   <li>Сохраняет текущий индекс в кэш старого плейлиста (по имени папки).</li>
     *   <li>Находит индекс нового плейлиста в общем списке всех плейлистов.</li>
     *   <li>Корректирует индекс границами (через {@link #checkIndexOutOfBoundPlaylist()}).</li>
     *   <li>Обновляет текст текущего плейлиста в UI.</li>
     * </ol>
     * </p>
     * <p>
     * Можно переопределить через {@link #setOnPlaylistChanged(Runnable)}.
     * </p>
     */
    public Runnable onPlaylistChanged = () -> {
        FileManager.instance.save(FileManager.instance.name(
                        Resources.Properties.DEFAULT_PLAYLIST_CACHE_PATHS.getKey() + File.separator
                                + new File(PlayProcessor.playProcessor.getCurrentMusicDir()).getName()),
                PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.getCurrentPlaylistIter()).getPath(), "last index",
                String.valueOf(PlayProcessor.playProcessor.getCurrentPlaylistIter()));

        PlayProcessor.playProcessor.setCurrentPlaylistIter(PlayProcessor.playProcessor.getCurrentPlaylist().indexOf(new Playlist(PlayProcessor.playProcessor.getCurrentMusicDir())));

        checkIndexOutOfBoundPlaylist();

        Root.tracksListView.getCurrentPlaylistText().setText(PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.getCurrentPlaylistIter()).getPath());
    };

    /**
     * Перезагружает текущий плейлист в UI и модель данных.
     * <p>
     * <b>Что делает:</b>
     * <ol>
     *   <li>Очищает ListView в UI и внутренний список треков {@link PlayProcessor#getTracks()}.</li>
     *   <li>Перезагружает треки из текущей папки {@link PlayProcessor#getCurrentMusicDir()}.</li>
     *   <li>Заполняет UI новыми треками и восстанавливает выделение текущего трека.</li>
     * </ol>
     * </p>
     * <p>
     * Вызывается при изменении содержимого папки плейлиста или для синхронизации UI.
     * </p>
     * @throws RuntimeException при ошибках чтения папки с музыкой
     * @see FileManager#getMusic(Path)
     */
    public static void _refreshPlaylist() {
        Root.tracksListView.getTrackListView().getItems().clear();

        try {
            PlayProcessor.playProcessor.getTracks().clear();
            PlayProcessor.playProcessor.getTracks().addAll(FileManager.instance.getMusic(Paths.get(PlayProcessor.playProcessor.getCurrentMusicDir())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Platform.runLater(() -> {
            for (Track track : PlayProcessor.playProcessor.getTracks())
                Root.tracksListView.getTrackListView().getItems().add(new Track(track.toString()));

            Root.trackSelectionModel.select(PlayProcessor.playProcessor.getTrackIter());
        });
    }

    /**
     * Корректирует индекс текущего плейлиста при выходе за границы.
     * <p>
     * <b>Логика:</b>
     * <ul>
     *   <li>Если индекс ≥ размер плейлиста → сбрасывает на 0 (начало).</li>
     *   <li>Если индекс &lt; 0 → устанавливает последний элемент.</li>
     * </ul>
     * </p>
     * <p>
     * Используется для циклической навигации по плейлисту (next/prev).
     * </p>
     */
    public static void checkIndexOutOfBoundPlaylist() {
        if(PlayProcessor.playProcessor.getCurrentPlaylistIter() >= PlayProcessor.playProcessor.getCurrentPlaylist().size()) {
            PlayProcessor.playProcessor.setCurrentPlaylistIter(0);
        } else if(PlayProcessor.playProcessor.getCurrentPlaylistIter() < 0) {
            PlayProcessor.playProcessor.setCurrentPlaylistIter(PlayProcessor.playProcessor.getCurrentPlaylist().size() - 1);
        }
    }

    /**
     * Устанавливает пользовательский callback для события смены плейлиста.
     * <p>
     * Заменяет стандартную логику {@link #onPlaylistChanged} на произвольную.
     * </p>
     * <p>
     * <b>Fluent API:</b> возвращает {@code this} для цепочки вызовов.
     * </p>
     *
     * @param onPlaylistChanged новый callback
     * @return {@code this} для fluent interface
     */
    public PlaylistController setOnPlaylistChanged(Runnable onPlaylistChanged) {
        this.onPlaylistChanged = onPlaylistChanged;
        return this;
    }

    /**
     * <h3>Next → Следующий плейлист</h3>
     * Переключается на следующий плейлист в списке {@link PlayProcessor#getCurrentPlaylist()}.
     * <p>
     * <b>Логика:</b>
     * <ol>
     *   <li>Увеличивает индекс {@code currentPlaylistIter++}.</li>
     *   <li>Если индекс вышел за границы (≥ size или &lt; 0) → сбрасывает на 0.</li>
     *   <li>Загружает новый плейлист через {@link #setPlaylist(String)}.</li>
     * </ol>
     * </p>
     */
    @Override
    public void next() {
        PlayProcessor.playProcessor.setCurrentPlaylistIter(PlayProcessor.playProcessor.getCurrentPlaylistIter() + 1);

        if (PlayProcessor.playProcessor.getCurrentPlaylistIter() >= PlayProcessor.playProcessor.getCurrentPlaylist().size()
                || PlayProcessor.playProcessor.getCurrentPlaylistIter() < 0) {
            PlayProcessor.playProcessor.setCurrentPlaylistIter(0);
        }

        setPlaylist(PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.getCurrentPlaylistIter()).getPath());
    }

    /**
     * <h3>Previous → Предыдущий плейлист</h3>
     * Переключается на предыдущий плейлист в списке.
     * <p>
     * <b>Логика:</b>
     * <ol>
     *   <li>Логирует текущий индекс (отладка).</li>
     *   *   <li>Уменьшает индекс {@code currentPlaylistIter--}.</li>
     *   <li>Если индекс вышел за границы (&lt; 0 или ≥ size) → устанавливает последний.</li>
     *   <li>Загружает новый плейлист через {@link #setPlaylist(String)}.</li>
     * </ol>
     * </p>
     * <p>
     * <b>Замечание:</b> условие {@code currentPlaylistIter - 1 > size - 1} избыточно
     * (после {@code --} индекс уже не может быть больше size).
     * </p>
     */
    @Override
    public void down() {
        PlayProcessor.playProcessor.setCurrentPlaylistIter(PlayProcessor.playProcessor.getCurrentPlaylistIter() - 1);

        if(PlayProcessor.playProcessor.getCurrentPlaylistIter() < 0 || PlayProcessor.playProcessor.getCurrentPlaylistIter() - 1 > PlayProcessor.playProcessor.getCurrentPlaylist().size() - 1) {
            PlayProcessor.playProcessor.setCurrentPlaylistIter(PlayProcessor.playProcessor.getCurrentPlaylist().size() - 1);
        }

        setPlaylist(PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.getCurrentPlaylistIter()).getPath());
    }

    /**
     * Однопоточный исполнитель для потокобезопасной смены плейлистов.
     * <p>
     * Гарантирует, что {@link #setPlaylist(String)} выполняется последовательно,
     * даже при параллельных вызовах из UI или таймеров.
     * </p>
     */
    private static final ExecutorService exec = Executors.newSingleThreadExecutor();

    /**
     * <h1>Основной метод смены плейлиста</h1>
     * Полностью переключает плеер на новую папку с музыкой.
     * <p>
     * <b>Полная последовательность (синхронизирована по {@link #exec}):</b>
     * </p>
     *
     * <h3>1. Подготовка</h3>
     * <ul>
     *   <li>Создаёт объекты {@code newPlaylist} и {@code oldPlaylist}.</li>
     *   <li>Загружает треки из новой папки через {@link FileManager#getMusic(Path)}.</li>
     * </ul>
     *
     * <h3>2. Обновление модели</h3>
     * <ul>
     *   <li>Устанавливает индекс нового плейлиста в общем списке.</li>
     *   <li>Очищает/заполняет {@link PlayProcessor#getTracks()} новыми треками.</li>
     * </ul>
     *
     * <h3>3. Сохранение состояния старого плейлиста</h3>
     * <ul>
     *   <li>Сохраняет {@code trackIter} в кэш старой папки (по её имени).</li>
     * </ul>
     *
     * <h3>4. Восстановление позиции в новом плейлисте</h3>
     * <ul>
     *   <li><b>Автовыбор:</b> если включена опция {@code playlist_track_auto_select_from_current},
     *       ищет текущий трек (по title+artist) в новом плейлисте.</li>
     *   <li><b>Кэш:</b> иначе восстанавливает {@code trackIter} из кэша новой папки.</li>
     * </ul>
     *
     * <h3>5. Финализация</h3>
     * <ul>
     *   <li>Устанавливает {@link PlayProcessor#setCurrentMusicDir(String)}.</li>
     *   <li>Обновляет UI асинхронно через {@link Platform#runLater(Runnable)}.</li>
     * </ul>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * // Переключение на папку "Rock"
     * playlistController.setPlaylist("/music/Rock");
     * }</pre>
     *
     * @param path путь к новой папке-плейлисту
     * @throws RuntimeException при ошибках чтения папки
     */
    @Override
    public void setPlaylist(String path) {
        synchronized (exec) {
            ArrayList<Track> newList;

            Playlist newPlaylist = new Playlist(path);
            Playlist oldPlaylist = new Playlist(PlayProcessor.playProcessor.getCurrentMusicDir());

            try {
                newList = FileManager.instance.getMusic(Path.of(newPlaylist.getPath()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            PlayProcessor.playProcessor.setCurrentPlaylistIter(PlayProcessor.playProcessor.getCurrentPlaylist().indexOf(newPlaylist));

            PlayProcessor.playProcessor.getTracks().clear();
            PlayProcessor.playProcessor.getTracks().addAll(newList);

            String pathOldDir = Path.of(Resources.Properties.DEFAULT_PLAYLIST_CACHE_PATHS.getKey()
                            + File.separator
                            + FileManager.instance.name(oldPlaylist.getFileName()))
                    .toAbsolutePath()
                    .toString();

            String pathNewDir = Path.of(Resources.Properties.DEFAULT_PLAYLIST_CACHE_PATHS.getKey()
                            + File.separator
                            + FileManager.instance.name(newPlaylist.getFileName()))
                    .toAbsolutePath()
                    .toString();

            FileManager.instance.save(
                    pathOldDir,
                    oldPlaylist.getPath(), // currentMusicDir - current playlist
                    Field.fields.get(Field.DataTypes.PLAYLIST_LAST_INDEX.code).getLocalName(),
                    String.valueOf(PlayProcessor.playProcessor.getTrackIter())
            );

            if (ConfigurationManager.instance.getBooleanItem("playlist_track_auto_select_from_current", "false")) {
                //TODO: Сменить на Stream API

                Track curr = new Track(MediaProcessor.mediaProcessor.mediaPlayer.getMedia().getSource());

                boolean isFound = false;

                for (int i = 0; i < newList.size(); i++) {
                    Track track = newList.get(i);

                    if(track.getTitle() == null || curr.getTitle() == null)
                        continue;

                    if (track.getTitle().equalsIgnoreCase(curr.getTitle()) && track.getArtist().equalsIgnoreCase(curr.getArtist())) {
                        PlayProcessor.playProcessor.setTrackIter(i);

                        isFound = true;

                        break;
                    }
                }

                if (!isFound) {
                    PlayProcessor.playProcessor.setTrackIter(Integer.parseInt(FileManager.instance.read(
                            pathNewDir,
                            newPlaylist.getPath(),
                            Field.fields.get(Field.DataTypes.PLAYLIST_LAST_INDEX.code).getLocalName(),
                            "0")
                    ));
                }
            } else {
                PlayProcessor.playProcessor.setTrackIter(Integer.parseInt(FileManager.instance.read(
                        pathNewDir,
                        newPlaylist.getPath(),
                        Field.fields.get(Field.DataTypes.PLAYLIST_LAST_INDEX.code).getLocalName(),
                        "0")
                ));
            }

            PlayProcessor.playProcessor.setCurrentMusicDir(path);

            Platform.runLater(() -> {
                Root.tracksListView.getTrackListView().getItems().clear();

                for(Track track : newList) {
                    Root.tracksListView.getTrackListView().getItems().add(track);
                }

                Root.tracksListView.getTrackListView().getSelectionModel().select(PlayProcessor.playProcessor.getTrackIter());
                Root.tracksListView.getCurrentPlaylistText().setText(PlayProcessor.playProcessor.getCurrentPlaylist().get(PlayProcessor.playProcessor.getCurrentPlaylistIter()).getPath());
            });
        }
    }
}
