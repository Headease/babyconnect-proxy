package nl.headease.babyconnectproxy.service;

import static nl.headease.babyconnectproxy.converter.Astraia2FhirPatientXmlConverter.FHIR__IDENTIFIER_SYSTEM_BSN;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.EpisodeOfCare;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Service;

@Service
public class FhirService {

  private final IGenericClient fhirClient;
  private final IParser parser;

  public FhirService(IGenericClient fhirClient, IParser parser) {
    this.fhirClient = fhirClient;
    this.parser = parser;
  }

  /**
   * Stores the {@link Bundle} in the FHIR store.
   *
   * @param bundle
   * @return the FHIR store response
   */
  public String sendBundle(Bundle bundle) {

    final Bundle result = fhirClient.transaction().withBundle(bundle).execute();

    return encodeResourceToString(result);
  }

  public Patient getPatientByBsn(String bsn) {

    final ICriterion<TokenClientParam> criterion = Patient.IDENTIFIER
        .exactly()
        .systemAndIdentifier(FHIR__IDENTIFIER_SYSTEM_BSN, bsn);

    final Bundle patientBundle = fhirClient.search()
        .forResource(Patient.class)
        .where(criterion)
        .returnBundle(Bundle.class)
        .execute();

    if(patientBundle.getEntry().isEmpty()) {
      //TODO: Don't throw such specific exceptions, only for the POC
      throw new IllegalArgumentException("Cannot find BSN");
    }

    return (Patient) patientBundle.getEntry().get(0).getResource();
  }

  public List<EpisodeOfCare> getEpisodesOfCare(Patient patient) {

    final ICriterion<ReferenceClientParam> criterion = EpisodeOfCare.PATIENT.hasId(patient.getIdElement().toUnqualifiedVersionless().getValue());

    final Bundle episodesOfCareBundle = fhirClient.search()
        .forResource(EpisodeOfCare.class)
        .where(criterion)
        .returnBundle(Bundle.class)
        .execute();

    return episodesOfCareBundle.getEntry().stream()
        .map(bundleEntryComponent -> (EpisodeOfCare) bundleEntryComponent.getResource())
        .collect(Collectors.toList());
  }
  public String encodeResourceToString(IBaseResource resource) {
    return parser.encodeResourceToString(resource);
  }

  public Patient getPatient(String id) {

    final ICriterion<TokenClientParam> criterion = Patient.RES_ID.exactly().identifier(id);

    final Bundle patientBundle = fhirClient.search()
        .forResource(Patient.class)
        .where(criterion)
        .returnBundle(Bundle.class)
        .execute();

    if(patientBundle.getEntry().isEmpty()) {
      //TODO: Don't throw such specific exceptions, only for the POC
      throw new IllegalArgumentException("Cannot find Patient by id: " + id);
    }

    return (Patient) patientBundle.getEntry().get(0).getResource();
  }

  public IBaseResource parseResource(String resourceString) {
    return parser.parseResource(resourceString);
  }


  public IBaseBundle proxyTest(String url) {

    return fhirClient.search()
        .byUrl(url)
        .execute();
  }
}
