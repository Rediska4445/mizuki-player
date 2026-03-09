package utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rf.ebanina.utils.collections.TypicalMapWrapper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TypicalMapWrapperTest {

    @Test
    void testPutAndGet() {
        TypicalMapWrapper<String> map = new TypicalMapWrapper<>();

        map.put("intKey", 123, Integer.class);
        map.put("strKey", "abc", String.class);

        assertEquals(123, map.get("intKey", Integer.class));
        assertEquals("abc", map.get("strKey", String.class));

        // Проверяем, что при отсутствующем ключе возвращается null
        assertNull(map.get("missingKey", String.class));
    }

    @Test
    void testGetTypeMismatchThrows() {
        TypicalMapWrapper<String> map = new TypicalMapWrapper<>();
        map.put("key", "value", String.class);

        // Запрос с неверным типом должен выбросить ClassCastException
        assertThrows(ClassCastException.class, () -> map.get("key", Integer.class));
    }

    @Test
    void testEqualsAndHashCode() {
        TypicalMapWrapper<String> map1 = new TypicalMapWrapper<>();
        TypicalMapWrapper<String> map2 = new TypicalMapWrapper<>();

        map1.put("key1", 1, Integer.class);
        map1.put("key2", "two", String.class);

        map2.put("key2", "two", String.class);
        map2.put("key1", 1, Integer.class);

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());

        map2.put("key1", 2, Integer.class);
        assertNotEquals(map1, map2);
    }

    @Test
    void testToStringContainsKeysAndTypes() {
        TypicalMapWrapper<String> map = new TypicalMapWrapper<>();
        map.put("key", 100, Integer.class);

        String str = map.toString();
        assertTrue(str.contains("key"));
        assertTrue(str.contains("Integer"));
        assertTrue(str.contains("100"));
    }

    private TypicalMapWrapper<String> map;

    @BeforeEach
    void setUp() {
        map = new TypicalMapWrapper<>();
    }

    @Test
    void putAuto_storesInteger_withCorrectType() {
        map.putAuto("age", 25);
        Integer result = map.getAuto("age", Integer.class);
        assertEquals(25, result);
    }

    @Test
    void putAuto_storesString_withCorrectType() {
        map.putAuto("name", "KUTE");
        String result = map.getAuto("name", String.class);
        assertEquals("KUTE", result);
    }

    @Test
    void putAuto_storesComplexObject() {
        ArrayList<String> list = new ArrayList<>();
        list.add("test");
        map.putAuto("list", list);

        List<?> result = map.getAuto("list", List.class);
        assertEquals(1, result.size());
        assertEquals("test", result.get(0));
    }

    @Test
    void getAuto_returnsNull_forMissingKey() {
        String result = map.getAuto("missing", String.class);
        assertNull(result);
    }

    @Test
    void getAuto_returnsExactMatch() {
        map.putAuto("name", "KUTE");
        String result = map.getAuto("name", String.class);
        assertEquals("KUTE", result);
    }

    @Test
    void getAuto_castsToSupertype() {
        ArrayList<String> concreteList = new ArrayList<>();
        map.putAuto("list", concreteList);

        List<?> result = map.getAuto("list", List.class);
        assertNotNull(result);
        assertInstanceOf(ArrayList.class, result);
    }

    @Test
    void getAuto_withoutTarget_usesSavedType() {
        // Given
        map.putAuto("name", "KUTE");

        // When
        String result = map.getAuto("name");

        // Then
        assertEquals("KUTE", result);
    }

    @Test
    void getAuto_withoutTarget_returnsNull_forMissingKey() {
        assertNull(map.getAuto("missing"));
    }

    @Test
    void getAuto_withoutTarget_worksWithIntegers() {
        map.putAuto("age", 25);
        Integer result = map.getAuto("age");
        assertEquals(25, result);
    }

    @Test
    void getAuto_withoutTarget_worksWith_putAuto() {
        // Given
        map.putAuto("name", "KUTE"); // сохраняет String.class

        // When & Then
        String result = map.getAuto("name");
        assertEquals("KUTE", result);
    }

    @Test
    void getAuto_throwsClassCastException_forIncompatibleTypes() {
        map.putAuto("number", 42);

        ClassCastException exception = assertThrows(ClassCastException.class,
                () -> map.getAuto("number", String.class));

        assertTrue(exception.getMessage().contains("Integer"));
        assertTrue(exception.getMessage().contains("String"));
        assertTrue(exception.getMessage().contains("number"));
    }

    @Test
    void putAuto_getAuto_fullCycle() {
        // Arrange & Act
        map.putAuto("name", "KUTE");
        map.putAuto("age", 30);
        map.putAuto("active", true);

        // Assert
        assertEquals("KUTE", map.getAuto("name", String.class));
        assertEquals(30, map.getAuto("age", Integer.class));
        assertTrue(map.getAuto("active", Boolean.class));
    }

    @Test
    void getAuto_handlesMultipleTypesInOneMap() {
        map.putAuto("string", "hello");
        map.putAuto("int", 123);
        map.putAuto("bool", false);

        assertEquals("hello", map.getAuto("string", String.class));
        assertEquals(123, map.getAuto("int", Integer.class));
        assertFalse(map.getAuto("bool", Boolean.class));
    }
}
