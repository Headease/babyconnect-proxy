package nl.headease.babyconnectproxy.converter;

import java.util.Arrays;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.xerces.dom.DeferredElementImpl;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.HumanName.NameUse;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Identifier.IdentifierUse;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class Astraia2FhirPatientXmlConverter {

  public static final String FHIR__IDENTIFIER_SYSTEM_BSN = "http://fhir.nl/fhir/NamingSystem/bsn";

  private final static String XPATH__PATIENT = "/export/Patient/record";

  private final static String XPATH__BSN = "data[@name='nhs_number']/@value";
  private final static String XPATH__NAME_FAMILY = "data[@name='name']/@value";
  private final static String XPATH__NAME_GIVEN = "data[@name='other_names']/@value";

  private final static List<String> profiles = Arrays.asList(
      "http://fhir.nl/fhir/StructureDefinition/nl-core-patient",
      "http://nictiz.nl/fhir/StructureDefinition/bc-woman");

  private final XPath xPath = XPathFactory.newInstance().newXPath();
  private final XPathExpression xPathExpressionPatient;
  private final XPathExpression xPathExpressionBsn;
  private final XPathExpression xPathExpressionNameFamily;
  private final XPathExpression xPathExpressionNameGiven;

  public Astraia2FhirPatientXmlConverter() {
    xPathExpressionPatient = getXPathExpression(XPATH__PATIENT);
    xPathExpressionBsn = getXPathExpression(XPATH__BSN);
    xPathExpressionNameFamily = getXPathExpression(XPATH__NAME_FAMILY);
    xPathExpressionNameGiven = getXPathExpression(XPATH__NAME_GIVEN);
  }

  public Patient convert(Document astraiaDocument) {
    try {
      final Node patientNode = (Node) xPathExpressionPatient.evaluate(astraiaDocument, XPathConstants.NODE);

      if(patientNode == null) {
        throw new IllegalArgumentException("No  patient record found");
      }

      final String id = "astraia-" + ((DeferredElementImpl) patientNode).getAttribute("id");

      final Node bsnNode = (Node) xPathExpressionBsn.evaluate(patientNode, XPathConstants.NODE);
      final Node familyNameNode = (Node) xPathExpressionNameFamily.evaluate(patientNode, XPathConstants.NODE);
      final NodeList givenNameNodes = (NodeList) xPathExpressionNameGiven.evaluate(patientNode, XPathConstants.NODESET);

      final Patient patient = new Patient();
      patient.setId(id);

      final Meta meta = new Meta();
      profiles.forEach(meta::addProfile);
      patient.setMeta(meta);

      final Identifier bsn = new Identifier();
      bsn.setSystem(FHIR__IDENTIFIER_SYSTEM_BSN);
      bsn.setUse(IdentifierUse.OFFICIAL);
      bsn.setValue(bsnNode.getNodeValue());

      patient.addIdentifier(bsn);

      final HumanName name = new HumanName();
      name.setUse(NameUse.OFFICIAL);
      name.setFamily(familyNameNode.getNodeValue());

      for (int i = 0; i < givenNameNodes.getLength(); i++) {
        final Node givenNameNode = givenNameNodes.item(i);
        name.addGiven(givenNameNode.getNodeValue());
      }

      patient.addName(name);

      return patient;
    } catch (XPathExpressionException e) {
      throw new IllegalArgumentException("Cannot evaluate Astraia document from XPath", e);
    }

  }

  private XPathExpression getXPathExpression(String xpathExpression) {
    try {
      return xPath.compile(xpathExpression);
    } catch (XPathExpressionException e) {
      throw new IllegalArgumentException(
          "Cannot create XPathExpression for expression: " + xpathExpression, e);
    }
  }
}
