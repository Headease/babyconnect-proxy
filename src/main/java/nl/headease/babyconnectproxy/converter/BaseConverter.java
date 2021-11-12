package nl.headease.babyconnectproxy.converter;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class BaseConverter {

  final XPath xPath = XPathFactory.newInstance().newXPath();

  XPathExpression getXPathExpression(String xpathExpression) {
    try {
      return xPath.compile(xpathExpression);
    } catch (XPathExpressionException e) {
      throw new IllegalArgumentException(
          "Cannot create XPathExpression for expression: " + xpathExpression, e);
    }
  }
}
