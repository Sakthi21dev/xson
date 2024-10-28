package org.dev.utility.dev_utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmNode;

public class PrimaryController {

  enum FileType {
    XML, JSON
  }

  @FXML
  RadioButton xPath;

  @FXML
  RadioButton xQuery;

  @FXML
  RadioButton jsonPath;

  @FXML
  RadioButton advJPath;

  @FXML
  TextArea editor;

  @FXML
  TextArea expression;

  @FXML
  TextArea resultBox;

  @FXML
  ToggleGroup groupExpression;

  @FXML
  Label fileNameLabel;

  @FXML
  TextArea jPathRegex;

  FileType fileType;

  Configuration jsonReadConfig = Configuration.builder()
      .jsonProvider(new JacksonJsonNodeJsonProvider()).mappingProvider(new JacksonMappingProvider())
      .build();
  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
  PauseTransition pause;

  @FXML
  public void initialize() {
    pause = new PauseTransition(Duration.millis(100));
    pause.setOnFinished(event -> executeExpression());
    expression.setOnKeyReleased(event -> {
      pause.playFromStart();
    });

    groupExpression.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
      if (newToggle != null) {
        jPathRegex.setDisable(!advJPath.isSelected());
        executeExpression();
      } else {
        jPathRegex.setDisable(true);
      }
    });

    jPathRegex.textProperty().addListener((observable) -> {
      advJPath.setSelected(true);
    });

    editor.textProperty().addListener((observable, oldValue, newValue) -> {
      String value = newValue.trim();
      if (value.isBlank()) {
        return;
      }
      char delimiter = value.charAt(0);
      if (delimiter == '<') {
        fileType = FileType.XML;
        if (jsonPath.isSelected() || advJPath.isSelected()
            || groupExpression.getSelectedToggle() == null)
          xPath.setSelected(true);
      } else if (delimiter == '{' || delimiter == '[') {
        fileType = FileType.JSON;
        if (xPath.isSelected() || xQuery.isSelected()
            || groupExpression.getSelectedToggle() == null)
          jsonPath.setSelected(true);
      }
    });

  }

  @FXML
  private void loadFile() throws IOException {
    FileChooser fileChooser = new FileChooser();
    FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
        "XML files (*.xml), JSON files (*.json) ", Arrays.asList("*.xml", "*.json"));
    fileChooser.getExtensionFilters().add(extFilter);
    File file = fileChooser.showOpenDialog(App.getRoot().getWindow());
    if (file == null) {
      return;
    }
    fileNameLabel.setText(file.getName());
    loadFileToEditor(file);
  }

  private void loadFileToEditor(File file) {

    try {
      if (file.getName().endsWith(".json")) {
        fileType = FileType.JSON;
        jsonPath.setSelected(true);
      } else if (file.getName().endsWith(".xml") && jsonPath.isSelected()) {
        fileType = FileType.XML;
        xPath.setSelected(true);
      } else if (file.getName().endsWith(".xml")) {
        fileType = FileType.XML;
      }

      String context = Files.readString(file.toPath());
      editor.setText(context);

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @FXML
  private void formatContent() throws Exception {

    if (FileType.XML.equals(fileType)) {
      String content = formatXml(editor.getText());
      editor.setText(content);
    } else if (FileType.JSON.equals(fileType)) {
      String content = formatJson(editor.getText());
      editor.setText(content);
    }

  }

  public static String formatXml(String xml) throws Exception {
    Processor processor = new Processor(false);
    Serializer serializer = processor.newSerializer();
    serializer.setOutputProperty(Serializer.Property.METHOD, "xml");
    serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
    XdmNode source = processor.newDocumentBuilder().build(new StreamSource(new StringReader(xml)));
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    serializer.setOutputStream(byteArrayOutputStream);
    serializer.serializeNode(source);
    return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
  }

  public static String formatJson(String jsonString)
      throws JsonMappingException, JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    Object json = mapper.readValue(jsonString, Object.class);
    ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
    return writer.writeValueAsString(json);
  }

  public void executeExpression() {
    try {
      executeExpression(expression.getText());
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void executeExpression(String expression) throws SaxonApiException,
      ParserConfigurationException, SAXException, IOException, XPathExpressionException {
    String content = editor.getText();
    if (expression == null || expression.isBlank() || content == null || content.isBlank())
      return;
    String regex = null;
    if (advJPath.isSelected())
      regex = jPathRegex.getText();

    String result = null;

    if (xPath.isSelected())
      result = executeXpath(content, expression);
    else if (xQuery.isSelected())
      result = executeXQuery(content, expression);
    else if (jsonPath.isSelected())
      result = executeJsonPath(content, expression);
    else if (advJPath.isSelected()) {
      result = executeAdvacedJsonPath(content, expression, regex);
    }
    resultBox.setText(result);
  }

  private String executeAdvacedJsonPath(String content, String expression, String regex) {
    // TODO Auto-generated method stub
    return "!!!--IN DEVELOPMENT--!!!";
  }

  public String executeXpath(String xml, String expression) {

    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      InputStream stream = new ByteArrayInputStream(xml.getBytes());
      Document source = builder.parse(stream);
      XPath xPath = XPathFactory.newInstance().newXPath();
      String value = xPath.compile(expression).evaluate(source);
      return value;
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  public String executeXQuery(String xml, String expression) {

    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      InputStream stream = new ByteArrayInputStream(xml.getBytes());
      Document source = builder.parse(stream);
      Processor processor = new Processor(false);
      XQueryCompiler compiler = processor.newXQueryCompiler();
      XQueryExecutable executable = compiler.compile(expression);
      XQueryEvaluator evaluator = executable.load();
      XdmNode xdmNode = processor.newDocumentBuilder().wrap(source);
      evaluator.setContextItem(xdmNode);
      return evaluator.evaluate().toString();
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  public String executeJsonPath(String json, String expression) {

    try {

      Object result = JsonPath.using(jsonReadConfig).parse(json).read(expression);
      JsonNode resultNode = null;
      if (result == null)
        return null;
      else if (result instanceof JsonNode) {
        resultNode = (JsonNode) result;
        return resultNode.toPrettyString();
      }
      return String.valueOf(result);
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  public void increaseFontSize() {
    AppData.fontSize = AppData.fontSize + 2;
    String fontSizeStyle = setFontSize(AppData.fontSize);
    changeFontSizeToTextAreaGroup(fontSizeStyle);
  }

  public void decreaseFontSize() {

    AppData.fontSize = AppData.fontSize - 2;
    String fontSizeStyle = setFontSize(AppData.fontSize);
    changeFontSizeToTextAreaGroup(fontSizeStyle);
  }

  public void clearAllText() {
    editor.clear();
    expression.clear();
    resultBox.clear();
    jPathRegex.clear();
    fileNameLabel.setText(null);
//    groupExpression.selectToggle(null);
  }

  public void changeFontSizeToTextAreaGroup(String fontSizeStyle) {
    editor.setStyle(fontSizeStyle);
    expression.setStyle(fontSizeStyle);
    resultBox.setStyle(fontSizeStyle);
    jPathRegex.setStyle(fontSizeStyle);
  }

  public static String setFontSize(double fontSize) {
    return "-fx-font-size: " + fontSize + "px;";
  }
}
