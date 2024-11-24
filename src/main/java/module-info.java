module org.dev.utility.dev_utility {
  requires javafx.controls;
  requires javafx.fxml;
  requires transitive javafx.graphics;
  requires Saxon.HE;
  requires transitive com.fasterxml.jackson.databind;
  requires java.xml;
  requires json.path;
  requires java.logging;
  requires java.desktop;


  opens org.dev.utility.dev_utility to javafx.fxml;

  exports org.dev.utility.dev_utility;
}
