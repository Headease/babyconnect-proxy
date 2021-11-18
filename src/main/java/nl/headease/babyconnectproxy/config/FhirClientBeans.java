package nl.headease.babyconnectproxy.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirClientBeans {

  @Bean
  public FhirContext fhirContext()  {
    return FhirContext.forDstu3();
  }

  @Bean
  public IGenericClient fhirClient(FhirContext fhirContext, FhirStoreConfiguration configuration) {
    final IGenericClient iGenericClient = fhirContext.newRestfulGenericClient(
        configuration.getEndpoint());

    //TODO: Probably a lot nicer to use for validating the bolt access policy
//    iGenericClient.registerInterceptor();
    return iGenericClient;
  }

  @Bean
  public IParser getParser(FhirContext fhirContext) {
    return fhirContext.newJsonParser();
  }
}
