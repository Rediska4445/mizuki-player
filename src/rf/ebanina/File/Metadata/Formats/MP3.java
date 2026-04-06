package rf.ebanina.File.Metadata.Formats;

import com.mpatric.mp3agic.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import rf.ebanina.File.Configuration.ConfigurationManager;
import rf.ebanina.File.Resources.ResourceManager;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Paint.ColorProcessor;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.Media;
import rf.ebanina.ebanina.Player.MediaPlayer;
import rf.ebanina.ebanina.Player.Track;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static rf.ebanina.File.Metadata.MetadataOfFile.iMetadataOfFiles;

public class MP3
        implements IFormatAudioMetadata
{
    @Override
    public int getDuration(String path) {
        double dura;

        try (MediaPlayer player = new MediaPlayer(new Media(path))) {
            dura = player.getOverDuration().toSeconds();
        }

        return (int) dura;
    }

    @Override
    public String getTitle(String path) {
        String[] delimiters = {" — ", " - ", "–", "—"};

        try {
            Mp3File mp3file = new Mp3File(path);
            String title = null;

            if (mp3file.hasId3v2Tag()) {
                title = mp3file.getId3v2Tag().getTitle();
            }

            if ((title == null || title.isEmpty()) && mp3file.hasId3v1Tag()) {
                title = mp3file.getId3v1Tag().getTitle();
            }

            if (title != null && !title.isEmpty()) {
                return title;
            } else {
                String filename = new File(path).getName();
                filename = filename.substring(0, filename.toLowerCase().lastIndexOf("."));

                for (String delimiter : delimiters) {
                    if (filename.contains(delimiter)) {
                        String[] parts = filename.split(delimiter, 2);

                        if (parts.length == 2) {
                            return parts[1].trim();
                        }
                    }
                }

                return filename;
            }
        } catch (InvalidDataException | UnsupportedTagException | IOException | RuntimeException e) {
            String filename = new File(path).getName();
            filename = filename.substring(0, filename.toLowerCase().lastIndexOf("."));

            for (String delimiter : delimiters) {
                if (filename.contains(delimiter)) {
                    String[] parts = filename.split(delimiter, 2);

                    if (parts.length == 2) {
                        return parts[1].trim();
                    }
                }
            }

            return filename;
        }
    }

    @Override
    public String getArtist(String path) {
        try {
            Mp3File mp3file = new Mp3File(path);
            String artist = null;

            if (mp3file.hasId3v2Tag()) {
                artist = mp3file.getId3v2Tag().getArtist();
            }

            if ((artist == null || artist.isEmpty()) && mp3file.hasId3v1Tag()) {
                artist = mp3file.getId3v1Tag().getArtist();
            }

            if (artist != null && !artist.isEmpty()) {
                return artist;
            } else {
                String filename = new File(path).getName();

                if (filename.toLowerCase().endsWith(".mp3")) {
                    filename = filename.substring(0, filename.length() - 4);
                }

                String[] delimiters = {" — ", " - ", "–", "—"};
                for (String delimiter : delimiters) {
                    if (filename.contains(delimiter)) {
                        String[] parts = filename.split(delimiter, 2);
                        if (parts.length >= 1) {
                            return parts[0].trim();
                        }
                    }
                }

                return "Unknown Artist";
            }
        } catch (InvalidDataException | UnsupportedTagException | IOException | RuntimeException e) {
            return "Unknown Artist";
        }
    }

    @Override
    public int getAudioFileDuration(String path) {
        try {
            return (int) new Mp3File(path).getLengthInSeconds();
        } catch (InvalidDataException | UnsupportedTagException | IOException | RuntimeException e) {
            return 0;
        }
    }

    @Override
    public Image getArt(Track path, int size, int size1, boolean preserve_ration, boolean smooth) {
        Image result = ResourceManager.Instance.loadImage("album_art_logo", size, size, ColorProcessor.isPreserveRatio, ColorProcessor.isSmooth);

        if (!path.isNetty()) {
            if (path.getPath() != null) {
                try {
                    java.nio.file.Path path_track = Paths.get(path.getPath());

                    if(path.getPath().endsWith("mp3")) {
                        Mp3File mp3file = new Mp3File(path_track.toFile());
                        ID3v2 id3v2Tag = mp3file.getId3v2Tag();

                        byte[] imageData = id3v2Tag.getAlbumImage();

                        if (imageData != null) {
                            result = new javafx.scene.image.Image(
                                    new ByteArrayInputStream(imageData),
                                    size, size, preserve_ration, smooth
                            );
                        } else if (ConfigurationManager.instance.getBooleanItem("album_art_parse", "false")) {
                            result = Root.rootImpl.artProcessor.parseImage(path.viewName(), size, size1, preserve_ration, smooth);

                            if (!PlayProcessor.playProcessor.isNetwork() && ConfigurationManager.instance.getBooleanItem("album_art_parsed_set_in_tags", "false")) {
                                iMetadataOfFiles.setArt(path.getPath(), SwingFXUtils.fromFXImage(result, null));
                            }
                        }
                    }
                } catch (InvalidDataException | UnsupportedTagException | IOException | RuntimeException e) {
                    result = new javafx.scene.image.Image(
                            ResourceManager.Instance.resourcesPaths.get("album_art_logo"), size, size, preserve_ration, smooth
                    );
                }
            }
        } else {
            return Root.rootImpl.artProcessor.parseImage(path.viewName(), size, size1, preserve_ration, smooth);
        }

        return result;
    }

    @Override
    public void setTitle(String path, String title) {
        try {
            Mp3File mp3file = new Mp3File(path);
            boolean hasId3v2 = mp3file.hasId3v2Tag();
            ID3v2 id3v2 = hasId3v2 ? mp3file.getId3v2Tag() : new com.mpatric.mp3agic.ID3v24Tag();
            id3v2.setTitle(title);

            if (!hasId3v2) {
                mp3file.setId3v2Tag(id3v2);
            }

            File tempFile = File.createTempFile("temp_mp3_", "." + getFormat());

            mp3file.save(tempFile.getAbsolutePath());

            File originalFile = new File(path);
            Files.move(
                    tempFile.toPath(),
                    originalFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setArtist(String path, String artist) {
        try {
            Mp3File mp3file = new Mp3File(path);
            boolean hasId3v2 = mp3file.hasId3v2Tag();
            ID3v2 id3v2 = hasId3v2 ? mp3file.getId3v2Tag() : new com.mpatric.mp3agic.ID3v24Tag();
            id3v2.setArtist(artist);

            if (!hasId3v2) {
                mp3file.setId3v2Tag(id3v2);
            }

            File tempFile = File.createTempFile("temp_mp3_", "." + getFormat());
            mp3file.save(tempFile.getAbsolutePath());

            File originalFile = new File(path);
            Files.move(
                    tempFile.toPath(),
                    originalFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] bufferedImageToByteArray(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        baos.flush();
        byte[] imageBytes = baos.toByteArray();
        baos.close();
        return imageBytes;
    }

    @Override
    public void setArt(String path, BufferedImage image) {
        try {
            byte[] imageData = bufferedImageToByteArray(image, "png");

            Mp3File mp3file = new Mp3File(path);

            ID3v2 id3v2Tag;
            if (mp3file.hasId3v2Tag()) {
                id3v2Tag = mp3file.getId3v2Tag();
            } else {
                id3v2Tag = new ID3v24Tag();
                mp3file.setId3v2Tag(id3v2Tag);
            }

            String mimeType = "image/png";

            id3v2Tag.setAlbumImage(imageData, mimeType);

            String tempFilePath = path + ".temp";
            mp3file.save(tempFilePath);

            Files.move(
                    Path.of(tempFilePath),
                    Path.of(path),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (UnsupportedTagException | InvalidDataException | IOException e) {
            e.printStackTrace();
        } catch (NotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getMetadataValue(String path, String key) {
        try {
            Mp3File mp3file = new Mp3File(path);
            if (!mp3file.hasId3v2Tag()) {
                return null;
            }

            ID3v2 id3v2 = mp3file.getId3v2Tag();

            return switch (key.toLowerCase()) {
                case "title" -> id3v2.getTitle();
                case "artist" -> id3v2.getArtist();
                case "album" -> id3v2.getAlbum();
                case "genre" -> id3v2.getGenreDescription();
                case "year", "date" -> id3v2.getYear();
                case "track" -> id3v2.getTrack();
                case "comment" -> id3v2.getComment();
                case "composer" -> id3v2.getComposer();
                case "lyrics" -> id3v2.getLyrics();
                case "length", "duration" -> String.valueOf(id3v2.getLength());
                default -> null;
            };
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void setMetadataValue(String path, String key, String value) {
        try {
            Mp3File mp3file = new Mp3File(path);
            ID3v2 id3v2 = mp3file.hasId3v2Tag() ? mp3file.getId3v2Tag() : new ID3v24Tag();

            switch (key.toLowerCase()) {
                case "title" -> id3v2.setTitle(value);
                case "artist" -> id3v2.setArtist(value);
                case "album" -> id3v2.setAlbum(value);
                case "genre" -> id3v2.setGenreDescription(value);
                case "year", "date" -> id3v2.setYear(value);
                case "track" -> id3v2.setTrack(value);
                case "comment" -> id3v2.setComment(value);
                case "composer" -> id3v2.setComposer(value);
                case "lyrics" -> id3v2.setLyrics(value);
                default -> {
                    return;
                }
            }

            if (!mp3file.hasId3v2Tag()) {
                mp3file.setId3v2Tag(id3v2);
            }

            File tempFile = File.createTempFile("temp_mp3_", "." + getFormat());
            mp3file.save(tempFile.getAbsolutePath());

            Files.move(
                    tempFile.toPath(),
                    Paths.get(path),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Map.Entry<String, String>> getAllMetadata(String path) {
        List<Map.Entry<String, String>> metadata = new ArrayList<>();

        try {
            Mp3File mp3file = new Mp3File(path);

            if (mp3file.hasId3v2Tag()) {
                ID3v2 id3v2 = mp3file.getId3v2Tag();

                addIfNotNull(metadata, "title", id3v2.getTitle());
                addIfNotNull(metadata, "artist", id3v2.getArtist());
                addIfNotNull(metadata, "album", id3v2.getAlbum());
                addIfNotNull(metadata, "year", id3v2.getYear());
                addIfNotNull(metadata, "genre", id3v2.getGenreDescription());
                addIfNotNull(metadata, "track", id3v2.getTrack());
                addIfNotNull(metadata, "comment", id3v2.getComment());
                addIfNotNull(metadata, "composer", id3v2.getComposer());
                addIfNotNull(metadata, "publisher", id3v2.getPublisher());
                addIfNotNull(metadata, "original_artist", id3v2.getOriginalArtist());
                addIfNotNull(metadata, "album_artist", id3v2.getAlbumArtist());
                addIfNotNull(metadata, "copyright", id3v2.getCopyright());
                addIfNotNull(metadata, "url", id3v2.getUrl());
                addIfNotNull(metadata, "encoder", id3v2.getEncoder());
                addIfNotNull(metadata, "lyrics", id3v2.getLyrics());

                if (id3v2.getLength() > 0) {
                    metadata.add(new AbstractMap.SimpleImmutableEntry<>("length", String.valueOf(id3v2.getLength())));
                }

                if (id3v2.getAlbumImageMimeType() != null) {
                    metadata.add(new AbstractMap.SimpleImmutableEntry<>("album_art_mime", id3v2.getAlbumImageMimeType()));
                }

                byte[] imageData = id3v2.getAlbumImage();
                if (imageData != null) {
                    metadata.add(new AbstractMap.SimpleImmutableEntry<>("album_art_size", String.valueOf(imageData.length)));
                }
            }

            if (!mp3file.hasId3v2Tag() && mp3file.hasId3v1Tag()) {
                ID3v1 id3v1 = mp3file.getId3v1Tag();

                addIfNotNull(metadata, "title", id3v1.getTitle());
                addIfNotNull(metadata, "artist", id3v1.getArtist());
                addIfNotNull(metadata, "album", id3v1.getAlbum());
                addIfNotNull(metadata, "genre", id3v1.getGenreDescription());
                addIfNotNull(metadata, "comment", id3v1.getComment());
                addIfNotNull(metadata, "year", id3v1.getYear());
            }

            long fileSize = Files.size(Paths.get(path));
            metadata.add(new AbstractMap.SimpleImmutableEntry<>("file_size_bytes", String.valueOf(fileSize)));

            if (mp3file.getBitrate() > 0) {
                metadata.add(new AbstractMap.SimpleImmutableEntry<>("bitrate_kbps", String.valueOf(mp3file.getBitrate())));
            }

            if (mp3file.getSampleRate() > 0) {
                metadata.add(new AbstractMap.SimpleImmutableEntry<>("sample_rate_hz", String.valueOf(mp3file.getSampleRate())));
            }

            metadata.add(new AbstractMap.SimpleImmutableEntry<>("channels", mp3file.isVbr() ? "VBR" : "CBR"));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return metadata;
    }

    private void addIfNotNull(List<Map.Entry<String, String>> list, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            list.add(new AbstractMap.SimpleImmutableEntry<>(key, value.trim()));
        }
    }

    @Override
    public String getFormat() {
        return MediaPlayer.AvailableFormat.MP3.getTitle();
    }
}