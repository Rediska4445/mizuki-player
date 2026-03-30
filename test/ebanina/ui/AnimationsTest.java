package ebanina.ui;

import javafx.animation.Animation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rf.ebanina.UI.UI.Animations;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

public class AnimationsTest {
    private Animations animations;

    @Mock
    private Animation mockAnimation;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        animations = new Animations();

        // Reflection для установки полей (поскольку private)
        setField("isAppFocused", true);
        setField("isAppMinimized", false);

        // Mock анимации
        when(mockAnimation.getStatus()).thenReturn(Animation.Status.STOPPED);
    }

    @Test
    void play_whenAppFocused_shouldPlayAnimation() throws Exception {
        // GIVEN - изначально остановлена
        when(mockAnimation.getStatus()).thenReturn(Animation.Status.STOPPED);

        // WHEN
        Animation result = animations.play(mockAnimation, "fadeIn");

        // THEN
        verify(mockAnimation).play();  // Должен вызвать play()
        assertSame(mockAnimation, result);

        // Последовательная проверка статусов
        verify(mockAnimation, times(1)).getStatus();  // Вызван 1 раз
        assertEquals(Animation.Status.STOPPED, mockAnimation.getStatus()); // Был STOPPED
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = Animations.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(animations, value);
    }
}