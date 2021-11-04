package nl.headease.babyconnectproxy.service;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import nl.headease.babyconnectproxy.converter.Astraia2FhirPatientXmlConverter;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Service
public class AstraiaConversionService {

  private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
  private final DocumentBuilder builder;
  private final Astraia2FhirPatientXmlConverter astraia2FhirPatientXmlConverter;

  public AstraiaConversionService(
      Astraia2FhirPatientXmlConverter astraia2FhirPatientXmlConverter) {
    this.astraia2FhirPatientXmlConverter = astraia2FhirPatientXmlConverter;

    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException("Cannot create document builder", e);
    }
  }

  public Patient convertToFhirPatient(String astraiaXml) {
    final Document astraiaDocument;

    try {
      astraiaDocument = builder.parse(new InputSource(new StringReader(astraiaXml)));
    } catch (SAXException | IOException e) {
      throw new IllegalArgumentException("Cannot transform body to XML document", e);
    }

    return astraia2FhirPatientXmlConverter.convert(astraiaDocument);
  }

}
