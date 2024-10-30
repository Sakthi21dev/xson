package org.dev.utility.dev_utility.services;

import javafx.animation.FadeTransition;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class ToasterService {

  public static void showToaster(String message) {
    Stage toaster = new Stage();
    toaster.initStyle(StageStyle.TRANSPARENT); // Make the stage transparent
  
    Button messageButton = new Button(message);
    messageButton.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-text-fill: white;");
  
    StackPane toasterLayout = new StackPane(messageButton);
    Scene toasterScene = new Scene(toasterLayout);
    toaster.setScene(toasterScene);
  
    // Set position of the toaster
    toaster.setX(0); // X position
    toaster.setY(0); // Y position
  
    // Fade in and out
    FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), messageButton);
    fadeIn.setFromValue(0);
    fadeIn.setToValue(1);
  
    FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), messageButton);
    fadeOut.setFromValue(1);
    fadeOut.setToValue(0);
    fadeOut.setDelay(Duration.seconds(2)); // Show for 2 seconds
  
    fadeIn.play();
    fadeOut.play();
  
    // Show the toaster and set it to close after fading out
    toaster.show();
  
    fadeOut.setOnFinished(event -> toaster.close());
  }

}
