package nl.headease.babyconnectproxy.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.apache.xerces.dom.DeferredElementImpl;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class Astraia2FhirObservationsXmlConverter extends BaseConverter {
  private static final Logger LOG = LoggerFactory.getLogger(Astraia2FhirObservationsXmlConverter.class);

  final CodeableConcept parityCode = new CodeableConcept(
      new Coding("http://loinc.org", "11977-6", "Parity"));

  final CodeableConcept gravidityCode = new CodeableConcept(
      new Coding("http://loinc.org", "161714006", "Gravidity"));

  final CodeableConcept eddCode = new CodeableConcept(
      new Coding("https://snomed.info/sct", "11996-6", "geschatte datum van partus (waarneembare entiteit)"));

  final CodeableConcept eddMethodCoding = new CodeableConcept(
      new Coding("https://snomed.info/sct", "31541000146106", "Date of delivery estimation based on diagnostic ultrasonography")
  );

  final CodeableConcept eddLmpMethodCoding = new CodeableConcept(
      new Coding("https://snomed.info/sct", "31521000146100", "Date of delivery estimation based on last period")
  );

  final CodeableConcept alcoholCoding = new CodeableConcept(
      new Coding("https://snomed.info/sct", "228273003", "Finding relating to alcohol drinking behavior (finding)")
  );

  final CodeableConcept tobaccoCoding = new CodeableConcept(
      new Coding("https://snomed.info/sct", "365980008", "Finding of tobacco use and exposure (finding)")
  );

  private final static String XPATH__EPISODES = "/export/Episode/record";

  private final static String XPATH__PARITY = "data[@name='parity']/@value";
  private final static String XPATH__GRAVIDITY = "data[@name='gravida']/@value";
  private final static String XPATH__EXPECTED_DATE_DELIVERY = "data[@name='edd_us']/@value";
  private final static String XPATH__EXPECTED_DATE_DELIVERY_LMP = "data[@name='edd_lmp']/@value";
  private final static String XPATH__ALCOHOL_USE = "data[@name='alcohol']/@value";
  private final static String XPATH__TOBACCO_USE = "data[@name='cigarettes']/@value";

  private final Meta nlCoreMeta = new Meta();
  private final Meta parityMeta = new Meta();
  private final Meta gravidityMeta = new Meta();
  private final Meta pregnancyMeta = new Meta();
  private final Meta alcoholMeta = new Meta();
  private final Meta tobaccoMeta = new Meta();

  private final XPathExpression xPathExpressionEpisodes;
  private final XPathExpression xPathExpressionParity;
  private final XPathExpression xPathExpressionGravidity;
  private final XPathExpression xPathExpressionEdd;
  private final XPathExpression xPathExpressionEddLmp;
  private final XPathExpression xPathExpressionAlcohol;
  private final XPathExpression xPathExpressionTobacco;

  public Astraia2FhirObservationsXmlConverter() {

    nlCoreMeta.addProfile("http://nictiz.nl/fhir/StructureDefinition/nl-core-observation");
    parityMeta.addProfile("http://nictiz.nl/fhir/StructureDefinition/zib-Pregnancy-Parity");
    gravidityMeta.addProfile("http://nictiz.nl/fhir/StructureDefinition/zib-Pregnancy-Gravidity");
    pregnancyMeta.addProfile("http://nictiz.nl/fhir/StructureDefinition/bc-PregnancyObservation");
    alcoholMeta.addProfile("http://nictiz.nl/fhir/StructureDefinition/zib-AlcoholUse");
    tobaccoMeta.addProfile("http://nictiz.nl/fhir/StructureDefinition/zib-TobaccoUse");

    xPathExpressionEpisodes = getXPathExpression(XPATH__EPISODES);

    xPathExpressionParity = getXPathExpression(XPATH__PARITY);
    xPathExpressionGravidity = getXPathExpression(XPATH__GRAVIDITY);
    xPathExpressionEdd = getXPathExpression(XPATH__EXPECTED_DATE_DELIVERY);
    xPathExpressionEddLmp = getXPathExpression(XPATH__EXPECTED_DATE_DELIVERY_LMP);
    xPathExpressionAlcohol = getXPathExpression(XPATH__ALCOHOL_USE);
    xPathExpressionTobacco = getXPathExpression(XPATH__TOBACCO_USE);
  }

  public List<Observation> convert(Document astraiaDocument, Reference patientReference) {

    List<Observation> observations = new ArrayList<>();

    try {
      final NodeList episodes = (NodeList) xPathExpressionEpisodes.evaluate(astraiaDocument, XPathConstants.NODESET);

      if(episodes != null) {
        for (int i = 0; i < episodes.getLength(); i++) {
          Node episode = episodes.item(i);
          final String id = ((DeferredElementImpl) episode).getAttribute("id");

          final Reference episodeOfCareReference = new Reference("EpisodeOfCare/astraia-" + id);

          final Node parity = (Node) xPathExpressionParity.evaluate(episode, XPathConstants.NODE);
          final Node gravidity = (Node) xPathExpressionGravidity.evaluate(episode, XPathConstants.NODE);
          final Node edd = (Node) xPathExpressionEdd.evaluate(episode, XPathConstants.NODE);
          final Node eddLmp = (Node) xPathExpressionEddLmp.evaluate(episode, XPathConstants.NODE);
          final Node alcoholUse = (Node) xPathExpressionAlcohol.evaluate(episode, XPathConstants.NODE);
          final Node tobaccoUse = (Node) xPathExpressionTobacco.evaluate(episode, XPathConstants.NODE);

          addQuantityObservation(observations, parity, patientReference, episodeOfCareReference, parityMeta, parityCode);
          addQuantityObservation(observations, gravidity, patientReference, episodeOfCareReference, gravidityMeta, gravidityCode);

          addDateTimeObservation(observations, patientReference, episodeOfCareReference, edd, pregnancyMeta, eddCode, eddMethodCoding);
          addDateTimeObservation(observations, patientReference, episodeOfCareReference, eddLmp, pregnancyMeta, eddCode, eddLmpMethodCoding); //also uses eddCode

          addCodeableConceptObservation(observations, patientReference, episodeOfCareReference, alcoholUse, alcoholMeta, alcoholCoding, "Alcoholgebruik");
          addCodeableConceptObservation(observations, patientReference, episodeOfCareReference, tobaccoUse, tobaccoMeta, tobaccoCoding, "RookgedragVMISLijst");

        }
      }

      return observations;

    } catch (XPathExpressionException e) {
      throw new IllegalArgumentException("Cannot evaluate Astraia document from XPath", e);
    }
  }

  /**
   * Fill an observation like <a href="https://simplifier.net/geboortezorg-stu3/observation-example-duplicate-189" target="_blank">this</a>.
   *
   * The <code>$.valueCodeableConcept.valueCodeableConcept.coding.code</code> is set based on the Astraia value
   *
   * @param observations
   * @param patientReference
   * @param episodeOfCareReference
   * @param node
   * @param meta
   * @param code
   * @param valueCodingSystem
   */
  private void addCodeableConceptObservation(List<Observation> observations, Reference patientReference,
      Reference episodeOfCareReference, Node node, Meta meta, CodeableConcept code, String valueCodingSystem) {

    if(node != null) {
      final Observation observation = getBaseObservation(patientReference, episodeOfCareReference, meta, code);

      final CodeType codeableConceptContainer = new CodeType();
      final Extension valueExtension = new Extension("http://nictiz.nl/fhir/StructureDefinition/code-specification");
      final CodeableConcept valueCodeableConcept = new CodeableConcept();

      final Coding coding = new Coding(valueCodingSystem, node.getNodeValue(), "to be mapped still!");
      valueCodeableConcept.setCoding(Collections.singletonList(coding));
      valueExtension.setValue(valueCodeableConcept);

      codeableConceptContainer.setExtension(Collections.singletonList(valueExtension));

      observation.setValue(codeableConceptContainer);
      observations.add(observation);
    }
  }

  /**
   * Fill an observation like <a href="https://simplifier.net/geboortezorg-stu3/observation-example-duplicate-86" target="_blank">this</a>.
   *
   * The <code>$.ValueDateTime</code> is set based on the Astraia value
   * @param observations
   * @param patientReference
   * @param episodeOfCareReference
   * @param node
   * @param meta
   * @param code
   * @param methodCode
   */
  private void addDateTimeObservation(List<Observation> observations, Reference patientReference, Reference episodeOfCareReference, Node node,
      Meta meta, CodeableConcept code, CodeableConcept methodCode) {

    if(node != null) {
      final Observation observation = getBaseObservation(patientReference, episodeOfCareReference, meta, code);

      observation.setMethod(methodCode);

      final String dateValue = node.getNodeValue();
      final DateTimeType value = new DateTimeType(dateValue);
      observation.setValue(value);

      observations.add(observation);
    }
  }

  /**
   * Fill an observation like <a href="https://simplifier.net/Geboortezorg-STU3/Observation-example-duplicate-90/~json" target="_blank">this</a>.
   *
   * The <code>$.valueQuantity.value</code> is set based on the Astraia value
   *
   * @param observations
   * @param node
   * @param subject
   * @param episodeOfCareReference
   * @param meta
   * @param code
   */
  private void addQuantityObservation(List<Observation> observations, Node node, Reference subject, Reference episodeOfCareReference, Meta meta, CodeableConcept code) {

    if(node != null) {
      final Observation observation = getBaseObservation(subject, episodeOfCareReference, meta, code);

      final Quantity quantity = new Quantity(
          Long.parseLong(node.getNodeValue())
      );
      quantity.setSystem("http://unitsofmeasure.org");
      quantity.setCode("1");

      observation.setValue(quantity);
      observations.add(observation);
    }
  }

  private Observation getBaseObservation(Reference subject, Reference episodeOfCareReference, Meta meta, CodeableConcept code) {
    final Observation observation = new Observation();
    observation.setSubject(subject);
    observation.setMeta(meta);
    observation.setCode(code);
    observation.setContext(episodeOfCareReference);
    return observation;
  }
}
