package nl.headease.babyconnectproxy.controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import nl.headease.babyconnectproxy.config.FhirStoreConfiguration;
import nl.headease.babyconnectproxy.service.AstraiaConversionService;
import nl.headease.babyconnectproxy.service.FhirService;
import nl.headease.babyconnectproxy.service.NutsProxyService;
import nl.nuts.client.model.TokenIntrospectionResponse;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("v1/proxy")
public class ProxyController {

  private final NutsProxyService nutsProxyService;
  private final AstraiaConversionService astraiaConversionService;
  private final FhirService fhirService;
  private final FhirStoreConfiguration fhirStoreConfiguration;
  private final RestTemplate restTemplate = new RestTemplate();

  public ProxyController(NutsProxyService nutsProxyService,
      AstraiaConversionService astraiaConversionService,
      FhirService fhirService,
      FhirStoreConfiguration fhirStoreConfiguration) {
    this.nutsProxyService = nutsProxyService;
    this.astraiaConversionService = astraiaConversionService;
    this.fhirService = fhirService;
    this.fhirStoreConfiguration = fhirStoreConfiguration;
  }

  @RequestMapping("fhir/**")
  public ResponseEntity<String> proxyFhirRequest(@RequestBody Optional<String> body, HttpMethod method, HttpServletRequest request)
      throws URISyntaxException {

    final TokenIntrospectionResponse nutsIntrospectionResult = nutsProxyService.introspectBearerToken(request);
    if(!nutsIntrospectionResult.isActive()) throw new IllegalStateException("Token inactive");

    //PROXY REQUEST
    final String path = StringUtils.substringAfterLast(request.getRequestURI(), "proxy");
    URI uri = new URI(fhirStoreConfiguration.getProtocol(), null,
        fhirStoreConfiguration.getHosname(), fhirStoreConfiguration.getPort(), path, request.getQueryString(), null);

    System.out.println(uri);

    ResponseEntity<String> responseEntity =
        restTemplate.exchange(uri, method, new HttpEntity<>(body), String.class);

    return new ResponseEntity<>(responseEntity.getBody(), HttpStatus.OK);
  }

  @PostMapping(value = "convert/astraia/patient", consumes = MediaType.APPLICATION_XML_VALUE,  produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> convertAstraiaMessage(@RequestBody String astraiaMessage, HttpServletRequest request) {

    //TODO: Decide on security

    final Bundle bundle = astraiaConversionService.convertToBundle(astraiaMessage);

    String response = fhirService.sendBundle(bundle);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
