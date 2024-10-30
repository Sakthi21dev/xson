package org.dev.utility.dev_utility;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.dev.utility.dev_utility.services.JsonService;
import org.dev.utility.dev_utility.services.ToasterService;
import org.dev.utility.dev_utility.services.XmlService;
import org.xml.sax.SAXException;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import net.sf.saxon.s9api.SaxonApiException;

public class PrimaryController {

  enum FileType {
    XML, JSON
  }

  RadioButton xPath = new RadioButton("XPath");

  RadioButton xQuery = new RadioButton("XQuery");

  CheckBox setCurrentNode = new CheckBox("Current Node");

  RadioButton jsonPath = new RadioButton("JPath");

  RadioButton advancedJsonPath = new RadioButton("Advanced JPath");

  ToggleGroup expressionTypeGroup = new ToggleGroup();

  @FXML
  TextArea jPathRegex, editor, expression, resultBox;

  @FXML
  Label fileNameLabel;

  @FXML
  HBox buttonContainer;

  @FXML
  SplitPane splitPane;

  FileType fileType;

  private String lastUsedDirectory = "";

  PauseTransition pause;

  public static String setFontSize(double fontSize) {
    return "-fx-font-size: " + fontSize + "px;";
  }

  public void changeFontSizeToTextAreaGroup(String fontSizeStyle) {
    editor.setStyle(fontSizeStyle);
    expression.setStyle(fontSizeStyle);
    resultBox.setStyle(fontSizeStyle);
    jPathRegex.setStyle(fontSizeStyle);
  }

  public void clearAllText() {
    editor.clear();
    expression.clear();
    resultBox.clear();
    jPathRegex.clear();
    fileNameLabel.setText(null);
    expressionTypeGroup.selectToggle(null);
    setCurrentNode.setSelected(false);
  }

  private void clearAndAddButtons(HBox container, Node... buttons) {
    container.getChildren().clear();
    container.getChildren().addAll(buttons);
  }

  public void decreaseFontSize() {

    AppData.fontSize = AppData.fontSize - 2;
    String fontSizeStyle = setFontSize(AppData.fontSize);
    changeFontSizeToTextAreaGroup(fontSizeStyle);
  }

  private String executeAdvacedJsonPath(String content, String expression, String regex) {
    return JsonService.parseJson(content, expression, regex);
  }

  public void executeExpression() {

    executeExpression(expression.getText());

  }

  public void executeExpression(String expression) {
    String content = editor.getText();
    if (expression == null || expression.isBlank() || content == null || content.isBlank()) {
      return;
    }
    String regex = null;
    if (advancedJsonPath.isSelected()) {
      regex = jPathRegex.getText();
    }

    String result = null;

    if (xPath.isSelected()) {
      result = XmlService.executeXpath(content, expression, setCurrentNode.isSelected());
    } else if (xQuery.isSelected()) {
      result = XmlService.executeXQuery(content, expression, setCurrentNode.isSelected());
    } else if (jsonPath.isSelected()) {
      result = JsonService.executeJsonPath(content, expression);
    } else if (advancedJsonPath.isSelected()) {
      result = executeAdvacedJsonPath(content, expression, regex);
    }
    resultBox.setText(result);
  }

  @FXML
  private void formatContent() {

    try {
      String content = editor.getText();
      if (content == null || content.isBlank()) {
        return;
      }

      if (FileType.XML == fileType) {
        content = XmlService.formatXml(content);
        editor.setText(content);
      } else if (FileType.JSON == fileType) {
        content = JsonService.formatJson(content);
        editor.setText(content);
      }
    } catch (Exception e) {
      e.printStackTrace();
      ToasterService.showToaster(e.getMessage());
    }
  }

  public void increaseFontSize() {
    AppData.fontSize = AppData.fontSize + 2;
    String fontSizeStyle = setFontSize(AppData.fontSize);
    changeFontSizeToTextAreaGroup(fontSizeStyle);
  }

  @FXML
  public void initialize() {

    setButtonContainer();

    pause = new PauseTransition(Duration.millis(500));
    pause.setOnFinished(event -> executeExpression());

    expression.setOnKeyReleased(event -> {
      pause.playFromStart();
    });

    advancedJsonPath.setOnKeyReleased(event -> {
      pause.playFromStart();
    });

    editor.setOnKeyReleased(event -> {
      pause.playFromStart();
    });

    setCurrentNode.selectedProperty().addListener((observable) -> {
      executeExpression();
    });

    expressionTypeGroup.getToggles().addAll(xPath, xQuery, jsonPath, advancedJsonPath);

    expressionTypeGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
      if (newToggle != null) {
        if ((!splitPane.getItems().contains(jPathRegex)) && advancedJsonPath.isSelected()
            && FileType.JSON == fileType) {
          splitPane.getItems().add(1, jPathRegex);
          splitPane.getDividers().get(1).setPosition(0.05);
        } else if (splitPane.getItems().contains(jPathRegex)) {
          splitPane.getItems().remove(jPathRegex);
        }
        executeExpression();
      } else if (splitPane.getItems().contains(jPathRegex)) {
        splitPane.getItems().remove(jPathRegex);
      }
    });

    jPathRegex.textProperty().addListener((observable) -> {
      advancedJsonPath.setSelected(true);
    });

    editor.textProperty().addListener((observable, oldValue, newValue) -> {
      String value = newValue.trim();
      if (value.isBlank()) {
        return;
      }

      detectContentType(value);
      // Change buttons appropriate to file
      setButtonContainer();

    });

  }

  public void detectContentType(String value) {
    char delimiter = value.charAt(0);
    if (delimiter == '<') {
      fileType = FileType.XML;
      if (jsonPath.isSelected() || advancedJsonPath.isSelected()
          || expressionTypeGroup.getSelectedToggle() == null) {
        xPath.setSelected(true);
      }
    } else if (delimiter == '{' || delimiter == '[') {
      fileType = FileType.JSON;
      if (xPath.isSelected() || xQuery.isSelected()
          || expressionTypeGroup.getSelectedToggle() == null) {
        jsonPath.setSelected(true);
      }
    }
  }

  @FXML
  private void loadFile() throws IOException {

    FileChooser fileChooser = new FileChooser();
    if (!lastUsedDirectory.isEmpty()) {
      fileChooser.setInitialDirectory(new File(lastUsedDirectory));
    }
    FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
        "XML files (*.xml), JSON files (*.json) ", Arrays.asList("*.xml", "*.json"));
    fileChooser.getExtensionFilters().add(extFilter);
    File file = fileChooser.showOpenDialog(App.getRoot().getWindow());
    if (file == null) {
      return;
    }
    lastUsedDirectory = file.getParent();
    System.out.println("Selected file: " + file.getAbsolutePath());
    fileNameLabel.setText(file.getName());
    loadFileToEditor(file);

  }

  private void loadFileToEditor(File file) {

    try {
//      if (file.getName().endsWith(".json")) {
//        fileType = FileType.JSON;
//      } else if (file.getName().endsWith(".xml")) {
//        fileType = FileType.XML;
//      }

      String context = Files.readString(file.toPath());
      editor.setText(context);

    } catch (IOException e) {
      ToasterService.showToaster(e.getMessage());
      e.printStackTrace();
    }

  }

  public void setButtonContainer() {

    if (fileType == null || fileType == FileType.XML) {

      clearAndAddButtons(buttonContainer, xPath, xQuery, setCurrentNode);
      if (splitPane.getItems().contains(jPathRegex)) {
        splitPane.getItems().remove(jPathRegex);
        splitPane.setDividerPosition(0, 0.05);
      }
    } else if (fileType == FileType.JSON) {
      clearAndAddButtons(buttonContainer, jsonPath, advancedJsonPath);
    }

  }

}
