package nl.headease.babyconnectproxy.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import nl.headease.babyconnectproxy.config.FhirStoreConfiguration;
import nl.headease.babyconnectproxy.model.NutsIntrospectionResult;
import org.hl7.fhir.dstu3.model.Bundle;
import org.springframework.stereotype.Service;

@Service
public class FhirService {

  private final FhirContext fhirContext = FhirContext.forDstu3();
  private final IParser parser = fhirContext.newJsonParser();
  private final IGenericClient fhirClient;

  public FhirService(FhirStoreConfiguration configuration) {
    fhirClient = fhirContext.newRestfulGenericClient(configuration.getEndpoint());
  }


  /**
   * Stores the {@link Bundle} to the FHIR store found in the {@link NutsIntrospectionResult}.
   *
   * @param bundle
   * @return the FHIR store response
   */
  public String sendBundle(Bundle bundle) {

    final Bundle result = fhirClient.transaction().withBundle(bundle).execute();

    return parser.encodeResourceToString(result);
  }

}
