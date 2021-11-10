package nl.headease.babyconnectproxy.service;

import static nl.headease.babyconnectproxy.converter.Astraia2FhirPatientXmlConverter.FHIR__IDENTIFIER_SYSTEM_BSN;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import nl.headease.babyconnectproxy.converter.Astraia2FhirObservationsXmlConverter;
import nl.headease.babyconnectproxy.converter.Astraia2FhirPatientXmlConverter;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleType;
import org.hl7.fhir.dstu3.model.Bundle.HTTPVerb;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Service
public class AstraiaConversionService {

  private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
  private final DocumentBuilder builder;
  private final Astraia2FhirPatientXmlConverter astraia2FhirPatientXmlConverter;
  private final Astraia2FhirObservationsXmlConverter astraia2FhirObservationsXmlConverter;

  public AstraiaConversionService(
      Astraia2FhirPatientXmlConverter astraia2FhirPatientXmlConverter,
      Astraia2FhirObservationsXmlConverter astraia2FhirObservationsXmlConverter) {
    this.astraia2FhirPatientXmlConverter = astraia2FhirPatientXmlConverter;
    this.astraia2FhirObservationsXmlConverter = astraia2FhirObservationsXmlConverter;

    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException("Cannot create document builder", e);
    }
  }

  public Bundle convertToBundle(String astraiaXml) {
    final Document astraiaDocument = convertXmlToDocument(astraiaXml);

    final Bundle bundle = new Bundle();
    bundle.setType(BundleType.TRANSACTION);

    final BundleEntryComponent patientBundleEntryComponent = getPatientBundleEntryComponent(astraiaDocument);
    bundle.addEntry(patientBundleEntryComponent);

    Patient patient = (Patient) patientBundleEntryComponent.getResource();
    final Identifier bsnIdentifier = patient.getIdentifier().stream()
        .filter(identifier -> FHIR__IDENTIFIER_SYSTEM_BSN.equals(identifier.getSystem()))
        .findAny()
        .orElseThrow(() -> new IllegalStateException("No BSN found on Patient resource, cannot create Reference"));

    final Reference patientReference = new Reference();
    patientReference.setIdentifier(bsnIdentifier);

    List<BundleEntryComponent> observations = getObservationBundleEntryComponents(astraiaDocument, patientReference);

    observations.forEach(bundle::addEntry);

    return bundle;
  }

  private BundleEntryComponent getPatientBundleEntryComponent(Document astraiaDocument) {

    final Patient patient = astraia2FhirPatientXmlConverter.convert(astraiaDocument);
    final BundleEntryComponent patientEntry = new BundleEntryComponent();
    final BundleEntryRequestComponent request = new BundleEntryRequestComponent();
    request.setMethod(HTTPVerb.POST);
    request.setUrl("Patient");
    request.setIfNoneExist("identifier=" + getPatientBsnIdentifier(patient));

    patientEntry.setResource(patient);
    patientEntry.setRequest(request);
    return patientEntry;
  }

  private List<BundleEntryComponent> getObservationBundleEntryComponents(Document astraiaDocument,
      Reference patientReference) {

    List<Observation> observations = astraia2FhirObservationsXmlConverter.convert(astraiaDocument, patientReference);

    return observations.stream()
        .map(observation -> {
          final BundleEntryComponent entryComponent = new BundleEntryComponent();
          final BundleEntryRequestComponent request = new BundleEntryRequestComponent();
          request.setMethod(HTTPVerb.POST);
          request.setUrl("Observation");

          entryComponent.setResource(observation);
          entryComponent.setRequest(request);

          return entryComponent;
        })
        .collect(Collectors.toList());
  }

  private String getPatientBsnIdentifier(Patient patient) {

    final List<Identifier> bsnIdentifier = patient.getIdentifier().stream()
        .filter(identifier -> FHIR__IDENTIFIER_SYSTEM_BSN.equals(identifier.getSystem()))
        .collect(Collectors.toList());

    if(bsnIdentifier.size() != 1) {
      throw new IllegalArgumentException(
          String.format("Expected to find a BSN identifier; found [%d] instead.", bsnIdentifier.size()));
    }

    final Identifier identifier = bsnIdentifier.get(0);
    return String.format("%s|%s", identifier.getSystem(), identifier.getValue());
  }

  private Document convertXmlToDocument(String xmlString) {
    try {
      return builder.parse(new InputSource(new StringReader(xmlString)));
    } catch (SAXException | IOException e) {
      throw new IllegalArgumentException("Cannot transform body to XML document", e);
    }
  }
}
