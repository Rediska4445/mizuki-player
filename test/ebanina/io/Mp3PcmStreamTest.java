package ebanina.io;

import javazoom.jl.decoder.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rf.ebanina.ebanina.Player.Mp3PcmStream;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Mp3PcmStreamTest {

    Bitstream bitstream;
    Decoder decoder;
    Mp3PcmStream pcmStream;

    @BeforeEach
    void setUp() {
        bitstream = mock(Bitstream.class);
        decoder = mock(Decoder.class);
        pcmStream = new Mp3PcmStream(bitstream);

        // Заменим внутренний decoder на мок для контроля
        // (через отражение или добавим сеттер в класс, если возможно)
        // Здесь для примера допустим доступно через рефлексию (упрощение)
        try {
            var decoderField = Mp3PcmStream.class.getDeclaredField("decoder");
            decoderField.setAccessible(true);
            decoderField.set(pcmStream, decoder);
        } catch (Exception e) {
            fail("Failed to set decoder mock");
        }
    }

    @Test
    void testReadReturnsDecodedBytes() throws IOException, BitstreamException, DecoderException {
        Header header = mock(Header.class);
        SampleBuffer sampleBuffer = mock(SampleBuffer.class);

        // Моделируем чтение фрейма
        when(bitstream.readFrame())
                .thenReturn(header)
                .thenReturn(null); // завершение потока после первого фрейма

        short[] shortBuffer = new short[]{0x0102, 0x0304};
        when(sampleBuffer.getBuffer()).thenReturn(shortBuffer);
        when(sampleBuffer.getBufferLength()).thenReturn(shortBuffer.length);

        when(decoder.decodeFrame(header, bitstream)).thenReturn(sampleBuffer);

        // Прочитать данные
        int b1 = pcmStream.read();
        int b2 = pcmStream.read();
        int b3 = pcmStream.read();
        int b4 = pcmStream.read();

        // Ожидаем правильное преобразование shorts в little-endian байты
        assertEquals(0x02, b1);
        assertEquals(0x01, b2);
        assertEquals(0x04, b3);
        assertEquals(0x03, b4);

        // После окончания чтения возвращаем -1
        assertEquals(-1, pcmStream.read());
    }

    @Test
    void testReadByteArrayPartiallyFilled() throws Exception {
        Header header = mock(Header.class);
        SampleBuffer sampleBuffer = mock(SampleBuffer.class);

        when(bitstream.readFrame()).thenReturn(header).thenReturn(null);

        short[] shortBuffer = new short[]{0x0102, 0x0304};
        when(sampleBuffer.getBuffer()).thenReturn(shortBuffer);
        when(sampleBuffer.getBufferLength()).thenReturn(shortBuffer.length);

        when(decoder.decodeFrame(header, bitstream)).thenReturn(sampleBuffer);

        byte[] buffer = new byte[4];
        int readBytes = pcmStream.read(buffer, 0, 4);

        assertEquals(4, readBytes);
        assertArrayEquals(new byte[]{0x02, 0x01, 0x04, 0x03}, buffer);

        int end = pcmStream.read(buffer, 0, 4);
        assertEquals(-1, end);
    }

    @Test
    void testReadThrowsIOExceptionOnDecoderException() throws Exception {
        when(bitstream.readFrame()).thenReturn(mock(Header.class));
        when(decoder.decodeFrame(any(), eq(bitstream))).thenThrow(new DecoderException("", new IOException()));

        assertThrows(IOException.class, () -> pcmStream.read());
    }
}

