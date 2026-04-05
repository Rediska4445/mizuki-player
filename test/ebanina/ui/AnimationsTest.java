package ebanina.ui;

import javafx.animation.Animation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rf.ebanina.UI.UI.Animations;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class AnimationsTest {

    private Animations animations;
    @Mock
    private Animation mockAnimation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        animations = new Animations().setAppFocused(true).setAppMinimized(false);

        // Mock последовательность вызовов
        when(mockAnimation.getStatus())
                .thenReturn(Animation.Status.STOPPED)
                .thenReturn(Animation.Status.RUNNING);
    }

    @Test
    void play_whenFocusedAndStopped_shouldPlay() {
        // WHEN
        Animation result = animations.play(mockAnimation, "fadeIn");

        // THEN
        verify(mockAnimation).getStatus();  // Проверка статуса
        verify(mockAnimation).play();        // Запуск
        assertSame(mockAnimation, result);
    }

    @Test
    void play_whenMinimized_shouldStop() {
        // GIVEN
        animations.setAppMinimized(true);

        // WHEN
        animations.play(mockAnimation, "any");

        // THEN
        verify(mockAnimation).stop();
        verify(mockAnimation, never()).play();
    }

    @Test
    void play_whenUnfocusedAndAllowed_shouldPlay() {
        // GIVEN
        animations.setAppFocused(false);
        when(mockAnimation.getStatus()).thenReturn(Animation.Status.STOPPED);

        // WHEN
        animations.play(mockAnimation, "outTransition");  // Разрешено!

        // THEN
        verify(mockAnimation).play();
    }

    @Test
    void play_whenUnfocusedAndNotAllowed_shouldStop() {
        // GIVEN
        animations.setAppFocused(false);

        // WHEN
        animations.play(mockAnimation, "random");  // НЕ разрешено

        // THEN
        verify(mockAnimation).stop();
        verify(mockAnimation, never()).play();
    }

    @Test
    void play_whenAlreadyRunning_shouldNotReplay() {
        // GIVEN
        when(mockAnimation.getStatus()).thenReturn(Animation.Status.RUNNING);

        // WHEN
        animations.play(mockAnimation, "test");

        // THEN
        verify(mockAnimation, never()).play();  // Уже запущена!
    }

    @Test
    void play_nullAnimation_shouldThrowNPE() {
        // WHEN & THEN
        assertThrows(NullPointerException.class,
                () -> animations.play(null, "test"));
    }
}