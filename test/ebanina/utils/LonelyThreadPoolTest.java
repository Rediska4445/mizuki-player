package ebanina.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rf.ebanina.utils.concurrency.LonelyThreadPool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LonelyThreadPoolTest {

    private LonelyThreadPool pool;

    @BeforeEach
    void setUp() {
        pool = new LonelyThreadPool();
    }

    @AfterEach
    void tearDown() throws Exception {
        pool.close();
    }

    @Test
    void testRunNewTaskExecutes() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        pool.runNewTask(() -> latch.countDown());

        assertTrue(latch.await(1, TimeUnit.SECONDS), "Task did not execute within timeout");
    }

    @Test
    void testRunNewTaskCancelsPrevious() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);

        Runnable longTask = () -> {
            started.countDown();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
            }
        };

        pool.runNewTask(longTask);
        assertTrue(started.await(1, TimeUnit.SECONDS), "Long task did not start");

        // запускаем новую задачу, что должно прервать предыдущую
        CountDownLatch secondLatch = new CountDownLatch(1);
        pool.runNewTask(() -> secondLatch.countDown());

        assertTrue(interrupted.await(1, TimeUnit.SECONDS), "Previous task was not interrupted");
        assertTrue(secondLatch.await(1, TimeUnit.SECONDS), "Second task did not execute");
    }

    @Test
    void testRunNewTaskWithOnCancelRunsOnCancel() throws Exception {
        CountDownLatch onCancelLatch = new CountDownLatch(1);
        CountDownLatch secondRunLatch = new CountDownLatch(1);

        Runnable longTask = () -> {
            try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        };

        Runnable onCancel = onCancelLatch::countDown;
        pool.runNewTask(longTask);
        pool.runNewTask(() -> secondRunLatch.countDown(), onCancel);

        assertTrue(onCancelLatch.await(1, TimeUnit.SECONDS), "onCancel Runnable did not run");
        assertTrue(secondRunLatch.await(1, TimeUnit.SECONDS), "Second task did not execute");
    }

    @Test
    void testShutdownStopsPool() throws Exception {
        pool.shutdown();

        // После shutdown запуск новой задачи бросит RejectedExecutionException
        assertThrows(RejectedExecutionException.class, () -> pool.runNewTask(() -> {}));
    }
}
