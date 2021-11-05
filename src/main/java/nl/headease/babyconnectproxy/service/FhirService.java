package nl.headease.babyconnectproxy.service;

import static nl.headease.babyconnectproxy.converter.Astraia2FhirPatientXmlConverter.FHIR__IDENTIFIER_SYSTEM_BSN;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import java.util.List;
import java.util.stream.Collectors;
import nl.headease.babyconnectproxy.model.NutsIntrospectionResult;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.stereotype.Service;

@Service
public class FhirService {

  private final FhirContext fhirContext = FhirContext.forDstu3();
  private final IParser parser = fhirContext.newJsonParser();

  /**
   * Stores the {@link Patient} in the configured FHIR store. This endpoint should eventually
   * be provided by the NUTS node that executed the token introspection.
   *
   *
   * @param patient
   * @param nutsIntrospectionResult
   * @return the FHIR store response
   */
  public String ensurePatient(Patient patient,
      NutsIntrospectionResult nutsIntrospectionResult) {

    IGenericClient fhirClient = fhirContext.newRestfulGenericClient(nutsIntrospectionResult.getServiceEndpoint());

    final List<Patient> patients = searchPatientByBsn(patient, fhirClient);

    if(!patients.isEmpty()) {
      return parser.encodeResourceToString(patients.get(0));
    }

    final MethodOutcome execute = fhirClient.create().resource(patient).execute();

    return parser.encodeResourceToString(execute.getResource());
  }

  /**
   * Would expect 0..1 results, but never hurts to assume we can have multiple
   *
   * @param patient
   * @param fhirClient
   * @return
   */
  private List<Patient> searchPatientByBsn(Patient patient, IGenericClient fhirClient) {

    final List<Identifier> bsnIdentifier = patient.getIdentifier().stream()
        .filter(identifier -> FHIR__IDENTIFIER_SYSTEM_BSN.equals(identifier.getSystem()))
        .collect(Collectors.toList());

    if(bsnIdentifier.size() != 1) {
      throw new IllegalArgumentException("Expected to find a BSN identifier; not present.");
    }

    ICriterion<TokenClientParam> criterion = new TokenClientParam("identifier")
        .exactly()
        .systemAndIdentifier(FHIR__IDENTIFIER_SYSTEM_BSN, bsnIdentifier.get(0).getValue());

    final Bundle patientSearchByBsn = fhirClient.search().forResource(Patient.class)
        .where(criterion).returnBundle(Bundle.class).execute();

    return patientSearchByBsn.getEntry().stream()
        .map(bundleEntryComponent -> (Patient) bundleEntryComponent.getResource())
        .collect(Collectors.toList());
  }
}
