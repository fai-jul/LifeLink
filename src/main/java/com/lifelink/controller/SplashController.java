package com.lifelink.controller;

import com.lifelink.SceneManager;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Professional, minimal splash animation:
 * blood-drop scales/pulses in -> logo fades/scales in -> tagline fades in
 * -> "press any key" fades in -> any key/click advances to Login.
 *
 * Kept subtle per the spec: "should look professional, not like a gaming intro."
 */
public class SplashController {

    @FXML private StackPane root;
    @FXML private StackPane dropContainer;
    @FXML private Label titleLabel;
    @FXML private Label taglineLabel;
    @FXML private Label hintLabel;

    private boolean advanced = false;

    @FXML
    public void initialize() {
        dropContainer.setScaleX(0.6);
        dropContainer.setScaleY(0.6);
        dropContainer.setOpacity(0);

        // 1. Drop scales/fades in
        FadeTransition dropFade = new FadeTransition(Duration.millis(600), dropContainer);
        dropFade.setFromValue(0);
        dropFade.setToValue(1);

        ScaleTransition dropScale = new ScaleTransition(Duration.millis(600), dropContainer);
        dropScale.setFromX(0.6);
        dropScale.setFromY(0.6);
        dropScale.setToX(1.0);
        dropScale.setToY(1.0);

        ParallelTransition dropIn = new ParallelTransition(dropFade, dropScale);

        // 2. The mark settles once; the screen stays calm and readable.
        ScaleTransition settle = new ScaleTransition(Duration.millis(700), dropContainer);
        settle.setFromX(1.0);
        settle.setFromY(1.0);
        settle.setToX(0.96);
        settle.setToY(0.96);

        // 3. Title fades/scales in
        FadeTransition titleFade = new FadeTransition(Duration.millis(500), titleLabel);
        titleFade.setToValue(1);
        ScaleTransition titleScale = new ScaleTransition(Duration.millis(500), titleLabel);
        titleScale.setFromX(0.9);
        titleScale.setFromY(0.9);
        titleScale.setToX(1);
        titleScale.setToY(1);

        // 4. Tagline fades in
        FadeTransition taglineFade = new FadeTransition(Duration.millis(500), taglineLabel);
        taglineFade.setToValue(1);

        // 5. Hint fades in
        FadeTransition hintFade = new FadeTransition(Duration.millis(500), hintLabel);
        hintFade.setToValue(1);

        SequentialTransition sequence = new SequentialTransition(
                dropIn,
                settle,
                new PauseTransition(Duration.millis(150)),
                new ParallelTransition(titleFade, titleScale),
                new PauseTransition(Duration.millis(150)),
                taglineFade,
                new PauseTransition(Duration.millis(250)),
                hintFade
        );
        sequence.play();

        // Advance on key press or mouse click.
        root.setFocusTraversable(true);
        root.setOnKeyPressed(e -> advance());
        root.setOnMouseClicked(e -> advance());
        // Ensure the root can receive key events as soon as the scene is attached.
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(e -> advance());
                root.requestFocus();
            }
        });
    }

    private void advance() {
        if (advanced) return;
        advanced = true;
        FadeTransition fadeOut = new FadeTransition(Duration.millis(350), root);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> SceneManager.switchTo("login.fxml", "Login"));
        fadeOut.play();
    }
}
