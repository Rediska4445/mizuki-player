package ipc;

import rf.ebanina.ebanina.Player.AudioPlugins.IPCHost;
import rf.ebanina.ebanina.Player.AudioPlugins.PluginWrapper;
import rf.ebanina.ebanina.Player.Controllers.MediaProcessor;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Scanner;

import static rf.ebanina.ebanina.Player.AudioPlugins.PluginWrapper.sendRequest;

public class InteractiveClient {
    public static void main(String[] args) throws Exception {
        IPCHost vstHost = new IPCHost(
                "localhost",
                12345,
                2,
                4096,
                "plugin",
                false,
                "logs");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Завершение работы");
            try {
                sendRequest(vstHost, "TERMINATE_PLUGIN", null, null);
            } catch (Exception e) {
                // Игнорируем ошибки при завершении
            }
            vstHost.stop();
        }));

        vstHost.start();

        System.out.println("Хост запущен. Ожидание подключения...");
        Thread.sleep(1000);

        System.out.println("Подключение к хосту...");

        if (!vstHost.getSocket().isOpen()) {
            System.err.println("Ошибка: Не удалось подключиться к хосту!");
            return;
        }
        System.out.println("✓ Подключено.");

        String pluginPath = "C:\\Program Files\\Common Files\\VST3\\iZotope\\Ozone 9 Equalizer.vst3";
        System.out.println("Инициализация плагина...");

        String initResponse = sendRequest(
                vstHost,
                "INIT_PLUGIN;vst3;" + pluginPath + ";44100;512;true;false",
                null,
                null);

        System.out.println("Плагин инициализирован: " + initResponse);

        // Небольшая задержка перед открытием редактора
        Thread.sleep(500);

        try {
            String response = sendRequest(vstHost, "OPEN_EDITOR", null, null);
            System.out.println("Редактор: " + response);
            // Даем GUI время на инициализацию
            Thread.sleep(1000);
        } catch (Exception e) {
            System.err.println("Не удалось открыть редактор: " + e.getMessage());
        }

        System.out.println("\nХост готов. Отправка аудио запросов...");

        int channels = 2;
        int samples = 512;
        int framesRead = samples; // а не expectedBytes!
        int expectedBytes = channels * samples * 4;

        // Буфер для приема аудио от хоста
        ByteBuffer audioBuffer = ByteBuffer.allocate(expectedBytes);
        audioBuffer.order(ByteOrder.LITTLE_ENDIAN);

        float[][] input = new float[channels][samples];
        float[][] output = new float[channels][samples];

        int requestCount = 0;
        double frequency = 440.0; // Начальная частота (Гц)

        while (true) {
            Thread.sleep(1000); // Пауза 1 секунда

            // 1. Генерация тестового сигнала (синусоида)
            for (int ch = 0; ch < channels; ch++) {
                for (int s = 0; s < samples; s++) {
                    double time = (requestCount * samples + s) / 44100.0;
                    input[ch][s] = (float) (0.5 * Math.sin(2 * Math.PI * frequency * time));
                }
            }

            // 2. Сериализация входа в byte[]
            ByteBuffer dataBuf = ByteBuffer.allocate(expectedBytes);
            dataBuf.order(ByteOrder.LITTLE_ENDIAN);
            for (int ch = 0; ch < channels; ch++) {
                for (int s = 0; s < samples; s++) {
                    dataBuf.putFloat(input[ch][s]);
                }
            }
            byte[] audioData = dataBuf.array();

            try {
                String processCommand = "PROCESS_AUDIO;" + channels + ";" + samples + ";" + framesRead;

                // Передаем audioBuffer последним аргументом!
                String processResponse = sendRequest(vstHost, processCommand, audioData, audioBuffer);

                // 4. Обработка ответа
                if (!processResponse.startsWith("OK:")) {
                    throw new Exception("Неверный формат ответа: " + processResponse);
                }

                // Распаковка аудио из буфера (который уже заполнен внутри sendRequest)
                audioBuffer.rewind(); // Сброс позиции в начало
                for (int ch = 0; ch < channels; ch++) {
                    for (int s = 0; s < samples; s++) {
                        output[ch][s] = audioBuffer.getFloat();
                    }
                }

                // 5. Вывод статистики
                requestCount++;
                double avgInput = 0, avgOutput = 0;
                for (int ch = 0; ch < channels; ch++) {
                    for (int s = 0; s < samples; s++) {
                        avgInput += Math.abs(input[ch][s]);
                        avgOutput += Math.abs(output[ch][s]);
                    }
                }
                avgInput /= (channels * samples);
                avgOutput /= (channels * samples);

                System.out.println("\n=== Запрос #" + requestCount + " ===");
                System.out.println("Частота: " + frequency + " Гц");
                System.out.println("Вход (сред. ампл.): " + String.format("%.4f", avgInput));
                System.out.println("Выход (сред. ампл.): " + String.format("%.4f", avgOutput));

                // Меняем частоту каждые 5 запросов
                if (requestCount % 5 == 0) {
                    frequency = (frequency == 440.0) ? 1000.0 : 440.0;
                    System.out.println(">>> Изменение частоты на " + frequency + " Гц");
                }

            } catch (Exception e) {
                System.err.println("Ошибка обработки: " + e.getMessage());
                e.printStackTrace();

                if (!vstHost.getSocket().isOpen()) {
                    System.err.println("Сокет закрыт. Выход.");
                    break;
                }
            }
        }
    }
}
