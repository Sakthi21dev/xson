package org.dev.utility.dev_utility.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmNode;

public class XmlService {

  private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

  public static String executeXpath(String xml, String expression, boolean setCurrentNode) {

    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      InputStream stream = new ByteArrayInputStream(xml.getBytes());
      Object source = builder.parse(stream);
      XPath xPath = XPathFactory.newInstance().newXPath();
      source = getCurrentNode(source, setCurrentNode);
      String value = xPath.compile(expression).evaluate(source);
      return value;
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  public static String executeXQuery(String xml, String expression, boolean setCurrentnode) {

    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      InputStream stream = new ByteArrayInputStream(xml.getBytes());
      Object source = builder.parse(stream);
      source = getCurrentNode(source, setCurrentnode);
      Processor processor = new Processor(false);
      XQueryCompiler compiler = processor.newXQueryCompiler();
      XQueryExecutable executable = compiler.compile(expression);
      XQueryEvaluator evaluator = executable.load();
      XdmNode xdmNode = processor.newDocumentBuilder().wrap(source);
      evaluator.setContextItem(xdmNode);
      return evaluator.evaluate().toString();
    } catch (Exception e) {
      ToasterService.showToaster(e.getMessage());
      return e.getMessage();
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

  public static Object getCurrentNode(Object source, boolean setCurrentnode)
      throws XPathExpressionException {
    XPath xPath = XPathFactory.newInstance().newXPath();
    Object newSource = xPath.compile("//*[@current_node]").evaluate(source, XPathConstants.NODE);
    if (setCurrentnode) {
      return newSource;
    }else if(newSource != null) {
      ToasterService.showToaster("Found current_node attribue on source data. But Current Node not enabled!");
    }
    
    return source;
  }

}
