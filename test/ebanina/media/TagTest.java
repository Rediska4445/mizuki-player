package ebanina.media;

import org.junit.jupiter.api.Test;
import rf.ebanina.ebanina.Player.Track;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;


public class TagTest {

    @Test
    void shouldSetAndGetName() {
        Track.Tag tag = new Track.Tag();
        tag.setName("java");
        assertEquals("java", tag.getName(), "Имя тега должно быть 'java'");
    }

    @Test
    void shouldInitializeWithNameViaConstructor() {
        Track.Tag tag = new Track.Tag("spring");
        assertEquals("spring", tag.getName(), "Конструктор должен установить имя");
    }

    @Test
    void shouldSerializeAndDeserializeCorrectly() throws IOException, ClassNotFoundException {
        Track.Tag original = new Track.Tag("test");

        byte[] serialized;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
            serialized = baos.toByteArray();
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            Track.Tag deserialized = (Track.Tag) ois.readObject();

            assertNotNull(deserialized, "Десериализованный объект не должен быть null");
            assertEquals(original.getName(), deserialized.getName(),
                    "Имя после десериализации должно совпадать");
            assertEquals(original, deserialized, "Объекты должны быть равны по equals");
        }
    }

    @Test
    void shouldCompareTagsCorrectly() {
        Track.Tag tag1 = new Track.Tag("tag");
        Track.Tag tag2 = new Track.Tag("tag");
        Track.Tag tag3 = new Track.Tag("other");

        assertEquals(tag1, tag2, "Теги с одинаковым именем должны быть равны");
        assertNotEquals(tag1, tag3, "Теги с разными именами не должны быть равны");
        assertNotEquals(tag1, null, "Тег не должен быть равен null");

        assertEquals(tag1.hashCode(), tag2.hashCode(),
                "Хэш-коды одинаковых тегов должны совпадать");
    }

    @Test
    void shouldHandleNullName() {
        Track.Tag tag = new Track.Tag();
        tag.setName(null);
        assertNull(tag.getName(), "Имя тега может быть null");
    }
}
