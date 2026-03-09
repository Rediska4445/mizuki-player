package rf.ebanina.File.Metadata.Formats;

import javafx.scene.image.Image;
import rf.ebanina.ebanina.Player.MediaPlayer;
import rf.ebanina.ebanina.Player.Track;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WAV implements IFormatAudioMetadata {

    @Override
    public String getFormat() {
        return MediaPlayer.AvailableFormat.WAV.getTitle();
    }

    @Override
    public int getDuration(String path) {
        return 0;
    }

    @Override
    public String getTitle(String path) {
        try {
            return extractMetadataValue(path, "INAM");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getArtist(String path) {
        try {
            return extractMetadataValue(path, "IART");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String extractMetadataValue(String filepath, String key) throws IOException {
        try (DataInputStream stream = new DataInputStream(new FileInputStream(filepath))) {
            byte[] header = new byte[12];
            stream.readFully(header);
            if (!(new String(header, 0, 4, StandardCharsets.US_ASCII).equals("RIFF") &&
                    new String(header, 8, 4, StandardCharsets.US_ASCII).equals("WAVE"))) {

                return null;
            }

            while (stream.available() > 0) {
                byte[] chunkIdBytes = new byte[4];
                stream.readFully(chunkIdBytes);
                String chunkId = new String(chunkIdBytes, StandardCharsets.US_ASCII);
                int chunkSize = Integer.reverseBytes(stream.readInt());

                if (chunkId.equals("LIST")) {
                    byte[] listTypeBytes = new byte[4];
                    stream.readFully(listTypeBytes);
                    String listType = new String(listTypeBytes, StandardCharsets.US_ASCII);

                    if (listType.equals("INFO")) {
                        int bytesRead = 4;

                        while (bytesRead < chunkSize) {
                            byte[] infoIdBytes = new byte[4];
                            stream.readFully(infoIdBytes);
                            String infoId = new String(infoIdBytes, StandardCharsets.US_ASCII);
                            int infoSize = Integer.reverseBytes(stream.readInt());
                            byte[] infoData = new byte[infoSize];
                            stream.readFully(infoData);
                            bytesRead += 8 + infoSize;
                            if ((infoSize % 2) != 0) {
                                stream.readByte();
                                bytesRead++;
                            }

                            if (infoId.equals(key)) {
                                int len = 0;
                                while (len < infoData.length && infoData[len] != 0) {
                                    len++;
                                }

                                return new String(infoData, 0, len, StandardCharsets.UTF_8);
                            }
                        }

                        break;
                    } else {
                        stream.skipBytes(chunkSize - 4);
                    }
                } else {
                    stream.skipBytes(chunkSize);
                }
            }
        }

        return null;
    }

    @Override
    public int getAudioFileDuration(String filepath) {
        try (DataInputStream stream = new DataInputStream(new FileInputStream(filepath))) {
            // RIFF header
            byte[] header = new byte[12];
            stream.readFully(header);
            if (!(new String(header, 0, 4, StandardCharsets.US_ASCII).equals("RIFF") &&
                    new String(header, 8, 4, StandardCharsets.US_ASCII).equals("WAVE"))) {
                return 0;
            }

            int sampleRate = 0;
            int byteRate = 0;
            int dataSize = 0;

            // Читаем чанки
            while (stream.available() > 0) {
                byte[] chunkIdBytes = new byte[4];
                if (stream.read(chunkIdBytes) < 4) break;
                String chunkId = new String(chunkIdBytes, StandardCharsets.US_ASCII);

                int chunkSizeLE = stream.readInt();
                int chunkSize = Integer.reverseBytes(chunkSizeLE);

                if ("fmt ".equals(chunkId)) {
                    // Формат (WAVEFORMATEX)
                    byte[] fmtData = new byte[chunkSize];
                    stream.readFully(fmtData);

                    // Little-endian чтение
                    // numChannels = short at offset 2 (не обязательно использовать)
                    sampleRate = (fmtData[4] & 0xFF) |
                            ((fmtData[5] & 0xFF) << 8) |
                            ((fmtData[6] & 0xFF) << 16) |
                            ((fmtData[7] & 0xFF) << 24);

                    byteRate = (fmtData[8] & 0xFF) |
                            ((fmtData[9] & 0xFF) << 8) |
                            ((fmtData[10] & 0xFF) << 16) |
                            ((fmtData[11] & 0xFF) << 24);
                } else if ("data".equals(chunkId)) {
                    dataSize = chunkSize;
                    // data сам можно не читать для длительности
                    stream.skipBytes(chunkSize);
                } else {
                    // пропускаем ненужные чанки
                    stream.skipBytes(chunkSize);
                }

                // Если всё уже нашли — можно выйти
                if (byteRate > 0 && dataSize > 0) {
                    break;
                }
            }

            if (byteRate > 0 && dataSize > 0) {
                double seconds = (double) dataSize / (double) byteRate;
                return (int) Math.round(seconds); // секундами
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public Image getArt(Track path, int size, int size1, boolean preserve_ration, boolean smooth) {
        if (path.isNetty() || path.getPath() == null) {
            return null; // сетевые треки не поддерживают
        }

        try {
            byte[] imageData = extractImageData(path.getPath());
            if (imageData != null && imageData.length > 0) {
                return new javafx.scene.image.Image(
                        new ByteArrayInputStream(imageData),
                        size, size1, preserve_ration, smooth
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null; // нет обложки в файле
    }

    private byte[] extractImageData(String filepath) throws IOException {
        try (DataInputStream stream = new DataInputStream(new FileInputStream(filepath))) {
            // Проверяем RIFF/WAVE
            byte[] header = new byte[12];
            stream.readFully(header);
            if (!(new String(header, 0, 4, StandardCharsets.US_ASCII).equals("RIFF") &&
                    new String(header, 8, 4, StandardCharsets.US_ASCII).equals("WAVE"))) {
                return null;
            }

            while (stream.available() > 0) {
                byte[] chunkIdBytes = new byte[4];
                if (stream.read(chunkIdBytes) < 4) break;
                String chunkId = new String(chunkIdBytes, StandardCharsets.US_ASCII);
                int chunkSize = Integer.reverseBytes(stream.readInt());

                if ("LIST".equals(chunkId)) {
                    // Читаем тип списка
                    byte[] listTypeBytes = new byte[4];
                    stream.readFully(listTypeBytes);
                    String listType = new String(listTypeBytes, StandardCharsets.US_ASCII);

                    if ("INFO".equals(listType)) {
                        // Ищем IDIT (изображение)
                        return extractImageFromInfoChunk(stream, chunkSize - 4);
                    } else if ("exif".equals(listType) || "ID3 ".equals(listType)) {
                        // Альтернативные форматы с изображением
                        byte[] data = new byte[chunkSize - 4];
                        stream.readFully(data);
                        byte[] image = extractImageFromBytes(data);
                        if (image != null) return image;
                    }
                } else if ("IDIT".equals(chunkId) || "PNG ".equals(chunkId) || "JPEG".equals(chunkId)) {
                    // Прямой чанк с изображением
                    byte[] imageData = new byte[chunkSize];
                    stream.readFully(imageData);
                    if (isValidImage(imageData)) {
                        return imageData;
                    }
                } else {
                    stream.skipBytes(chunkSize);
                }
            }
        }
        return null;
    }

    private byte[] extractImageFromInfoChunk(DataInputStream stream, int infoSize) throws IOException {
        int bytesRead = 0;
        while (bytesRead < infoSize) {
            byte[] infoIdBytes = new byte[4];
            stream.readFully(infoIdBytes);
            String infoId = new String(infoIdBytes, StandardCharsets.US_ASCII);
            int infoSizeInt = Integer.reverseBytes(stream.readInt());

            if ("IDIT".equals(infoId)) {
                byte[] imageData = new byte[infoSizeInt];
                stream.readFully(imageData);
                if (isValidImage(imageData)) {
                    return imageData;
                }
            }

            bytesRead += 8 + infoSizeInt;
            if ((infoSizeInt % 2) != 0) {
                stream.readByte();
                bytesRead++;
            }
        }
        return null;
    }

    private byte[] extractImageFromBytes(byte[] data) {
        // Простая проверка PNG/JPEG сигнатур
        if (data.length > 8) {
            if (data[0] == (byte)0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G') {
                return data;
            }
            if (data[0] == (byte)0xFF && data[1] == (byte)0xD8) {
                return data;
            }
        }
        return null;
    }

    private boolean isValidImage(byte[] data) {
        if (data == null || data.length < 8) return false;
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        // JPEG: FF D8 FF
        return (data[0] == (byte)0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) ||
                (data[0] == (byte)0xFF && data[1] == (byte)0xD8);
    }

    @Override
    public void setTitle(String path, String title) {

    }

    @Override
    public void setArtist(String path, String artist) {

    }

    @Override
    public void setArt(String path, BufferedImage image) {

    }

    @Override
    public String getMetadataValue(String path, String key) {
        return null;
    }

    @Override
    public void setMetadataValue(String path, String key, String value) {

    }

    @Override
    public List<Map.Entry<String, String>> getAllMetadata(String path) {
        return new ArrayList<>();
    }
}
