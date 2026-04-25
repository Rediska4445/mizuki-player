package rf.ebanina.UI.UI.Element.Dialogs;

import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import rf.ebanina.File.Resources.ResourceManager;

/**
 * Диалог лицензионного соглашения с принудительным просмотром контента.
 * <p>
 * Компонент показывает текст лицензии в прокручиваемой области. Кнопка "Принять"
 * активируется только после полного прокрутки текста до конца (&ge; 98% позиции).
 * Если контент короче высоты диалога, кнопка сразу активна.
 * </p>
 *
 * <h3>Ключевые особенности</h3>
 * <ul>
 *   <li>Автоопределение необходимости прокрутки через {@link ScrollPane#getVmax()}.</li>
 *   <li>Прокрутка до 98% активирует {@link #acceptButton} ({@link Button#setDisable(boolean)}}).</li>
 *   <li>Кастомный стиль ScrollPane без фона через stylesheet {@code "scrollpane-scroll-bar"}.</li>
 *   <li>Текст в {@link TextFlow} с межстрочным интервалом 5px и цветом LIGHTGRAY.</li>
 * </ul>
 *
 * <h3>Сценарии использования</h3>
 * <ul>
 *   <li>EULA/GPL лицензии в установщиках/приложениях.</li>
 *   <li>Обязательные Terms of Service с доказательством прочтения.</li>
 *   <li>Юридические соглашения с UX-подтверждением ознакомления.</li>
 * </ul>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * LicenseDialog license = new LicenseDialog(
 *     primaryStage,
 *     "Лицензионное соглашение",
 *     "Полный текст лицензии...",
 *     "Я согласен"
 * );
 * license.show();
 * }</pre>
 *
 * Диалог автоматически закрывается при нажатии активной кнопки через {@link #hide()}.
 *
 * @author Ebanina Std
 * @since 0.1.4.4
 * @see Dialog
 * @see ScrollPane
 * @see TextFlow
 */
public class LicenseDialog
        extends AnimationDialog
{
    /**
     * Прокручиваемая область с текстом лицензии (фиксированная высота 400px).
     */
    protected ScrollPane scrollPane;
    /**
     * Кнопка принятия лицензии, изначально отключена до прокрутки текста.
     */
    protected rf.ebanina.UI.UI.Element.Buttons.Button acceptButton;
    /**
     * Создаёт диалог лицензии с полным UI и логикой активации кнопки.
     * <p>
     * Сборка компонентов:
     * <ul>
     *   <li>Заголовок: жирный белый текст 16px (верхний регистр).</li>
     *   <li>Контент: {@link TextFlow} с серым текстом 14px, интервал 5px.</li>
     *   <li>ScrollPane: прозрачный фон, кастомный стиль скроллбара.</li>
     *   <li>Кнопка: скругление 20px, размер 140×44px, курсор HAND, изначально отключена.</li>
     * </ul>
     * </p>
     *
     * @param ownerStage родительское окно
     * @param title заголовок диалога (автоматически переводится в верхний регистр)
     * @param licenseText полный текст лицензии для отображения
     * @param btnText текст на кнопке принятия
     */
    public LicenseDialog(Stage ownerStage, Pane root, String title, String licenseText, String btnText) {
        super(ownerStage, root);

        setCloseOnBackgroundClick(false);

        Label titleLabel = new Label(title.toUpperCase());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16px;");

        Text content = new Text(licenseText);
        content.setFill(Color.LIGHTGRAY);
        content.setStyle("-fx-font-size: 14px;");
        TextFlow flow = new TextFlow(content);
        flow.setLineSpacing(5);

        scrollPane = new ScrollPane(flow);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.getStylesheets().add(ResourceManager.Instance.loadStylesheet("scrollpane-scroll-bar"));

        Label btnLabel = new Label(btnText);
        btnLabel.setTextFill(Color.WHITE);

        acceptButton = new rf.ebanina.UI.UI.Element.Buttons.Button(btnLabel) {};
        acceptButton.setCornerRadius(20);
        acceptButton.setSize(140, 44);
        acceptButton.setDisable(true);
        acceptButton.setCursor(Cursor.HAND);

        acceptButton.setOnAction(e -> hide());

        scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 0.98) {
                acceptButton.setDisable(false);
            }
        });

        Platform.runLater(() -> {
            if (scrollPane.getVmax() == 0 || flow.getBoundsInLocal().getHeight() < scrollPane.getHeight()) {
                acceptButton.setDisable(false);
            }
        });

        dialogBox.getChildren().addAll(titleLabel, scrollPane, acceptButton);
    }

    /**
     * Возвращает прокручиваемую область лицензионного текста.
     *
     * @return {@link ScrollPane} для дополнительной настройки или доступа к состоянию
     */
    public ScrollPane getScrollPane() {
        return scrollPane;
    }

    /**
     * Возвращает кнопку принятия лицензии.
     *
     * @return {@link Button} для программного управления (disable/enable, стилизация)
     */
    public rf.ebanina.UI.UI.Element.Buttons.Button getAcceptButton() {
        return acceptButton;
    }
}
