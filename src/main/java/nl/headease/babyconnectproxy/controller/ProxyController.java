package nl.headease.babyconnectproxy.controller;

import static nl.headease.babyconnectproxy.converter.Astraia2FhirPatientXmlConverter.FHIR__IDENTIFIER_SYSTEM_BSN;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import nl.headease.babyconnectproxy.config.FhirStoreConfiguration;
import nl.headease.babyconnectproxy.service.AstraiaConversionService;
import nl.headease.babyconnectproxy.service.FhirService;
import nl.headease.babyconnectproxy.service.NutsService;
import nl.nuts.client.auth.model.TokenIntrospectionResponse;
import nl.nuts.client.vcr.model.CredentialSubject;
import nl.nuts.client.vcr.model.ResolutionResult;
import nl.nuts.client.vcr.model.ResolutionResult.CurrentStatusEnum;
import nl.nuts.client.vcr.model.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IBaseResource;
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

  private final NutsService nutsService;
  private final AstraiaConversionService astraiaConversionService;
  private final FhirService fhirService;
  private final FhirStoreConfiguration fhirStoreConfiguration;
  private final RestTemplate restTemplate = new RestTemplate();

  public ProxyController(NutsService nutsService,
      AstraiaConversionService astraiaConversionService,
      FhirService fhirService,
      FhirStoreConfiguration fhirStoreConfiguration) {
    this.nutsService = nutsService;
    this.astraiaConversionService = astraiaConversionService;
    this.fhirService = fhirService;
    this.fhirStoreConfiguration = fhirStoreConfiguration;
  }

//  /**
//   * proxies GET /Patient.
//   *
//   * @param request
//   * @return
//   */
//  @GetMapping("fhir/Patient")
//  public ResponseEntity<String> getPatients(HttpServletRequest request) {
//    final CredentialSubject credentialSubject = introspectAndAuthenticate(request);
//
//    final Bundle patientByBsn = fhirService.getPatientByBsn(
//        credentialSubject.getSubject().replace("urn:oid:2.16.840.1.113883.2.4.6.3:", ""));
//
//    final String response = fhirService.encodeResourceToString(patientByBsn);
//
//    return new ResponseEntity<>(response, HttpStatus.OK);
//  }
//
//  /**
//   * Proxies specific read for a Patient
//   *
//   * @param id
//   * @param request
//   * @return
//   */
//  @GetMapping(value = "fhir/Patient/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
//  public ResponseEntity<String> getPatients(@PathVariable String id, HttpServletRequest request) {
//
//    final CredentialSubject credentialSubject = introspectAndAuthenticate(request);
//    final String requestingUserBsn = credentialSubject.getSubject()
//        .replace("urn:oid:2.16.840.1.113883.2.4.6.3:", "");
//
//    final Patient patient = fhirService.getPatient(id);
//
//    validateMatchingBsn(requestingUserBsn, patient);
//
//    final String response = fhirService.encodeResourceToString(patient);
//
//    return new ResponseEntity<>(response, HttpStatus.OK);
//  }

//  @GetMapping("fhir/EpisodeOfCare")
//  public ResponseEntity<String> getEpisodesOfCare() {
//
//  }

  /**
   * PROXY ALL MAPPING - Bolt spec checked here
   *
   * @param body
   * @param method
   * @param request
   * @return
   * @throws URISyntaxException
   */
  @RequestMapping("old/fhir/**")
  public ResponseEntity<String> proxyFhirRequest(@RequestBody Optional<String> body,
      HttpMethod method, HttpServletRequest request) throws URISyntaxException {

    final CredentialSubject credentialSubject = introspectAndAuthenticate(request);

    final List<Resource> allowedResources = credentialSubject.getResources();
    final String nutsAuthenticatedBsn = credentialSubject.getSubject();

    //AUTHORISATION
    //TODO: integrate the new/fhir/** request below into this one after Caresharing can proxy 1-on-1

    //PROXY REQUEST
    final String path = StringUtils.substringAfterLast(request.getRequestURI(), "proxy");
    URI uri = new URI(fhirStoreConfiguration.getProtocol(), null,
        fhirStoreConfiguration.getHosname(), fhirStoreConfiguration.getPort(), path,
        request.getQueryString(), null);

    System.out.println(uri);

    ResponseEntity<String> responseEntity =
        restTemplate.exchange(uri, method, new HttpEntity<>(body), String.class);

    return new ResponseEntity<>(responseEntity.getBody(), HttpStatus.OK);
  }

  /**
   * PROXY ALL MAPPING - not using yet as our Bolt only allows 3 resource types and GETs
   *
   * @param body
   * @param method
   * @param request
   * @return
   * @throws URISyntaxException
   */
  @RequestMapping(value = "fhir/**")
  public ResponseEntity<String> proxyFhirRequestNew(@RequestBody Optional<String> body,
      HttpMethod method, HttpServletRequest request)
      throws URISyntaxException {

    final CredentialSubject credentialSubject = introspectAndAuthenticate(request);

    final List<Resource> allowedResources = credentialSubject.getResources();
    final String requestingUserBsn = credentialSubject.getSubject()
        .replace("urn:oid:2.16.840.1.113883.2.4.6.3:", "");
//    final String requestingUserBsn = "615717341";

    //PROXY REQUEST
    final String path = StringUtils.substringAfterLast(request.getRequestURI(), "proxy");
    final List<NameValuePair> params = URLEncodedUtils.parse(request.getQueryString(),
        StandardCharsets.UTF_8);

    boolean validateBsnInProxyResults = false;

    if (method == HttpMethod.GET) {

      //read one patient
      if (path.matches("^/fhir/Patient/.+\\??+.*")) {
        validateBsnInProxyResults = true;

        //read all Patients
      } else if (path.startsWith("/fhir/Patient")) {

        final String identifier = String.format("%s|%s", FHIR__IDENTIFIER_SYSTEM_BSN,
            requestingUserBsn);
        params.add(new BasicNameValuePair("identifier", identifier));

      //read one
      } else if (path.matches("^/fhir/EpisodeOfCare/.+\\??+.*")) {
        //TODO: Implement
      } else if(path.startsWith("/fhir/EpisodeOfCare")) {
        final Patient requestingPatient = fhirService.getPatientByBsn(requestingUserBsn);

        //TODO: Create criteria based on the credentialSubject.getResources()
        params.add(new BasicNameValuePair("patient", getLogicalId(requestingPatient)));

      //read one
      } else if(path.matches("^/fhir/Observation/.+\\??+.*")) {
        //TODO: Implement
      } else if(path.startsWith("/fhir/Observation")) {
        final Patient requestingPatient = fhirService.getPatientByBsn(requestingUserBsn);

        //TODO: Create criteria based on the credentialSubject.getResources() matching Observation.context
        params.add(new BasicNameValuePair("subject", getLogicalId(requestingPatient)));
      }
    }

    String queryString = params.stream()
        .map(param -> String.format("%s=%s&", param.getName(), param.getValue()))
        .collect(Collectors.joining());

    queryString = StringUtils.removeEnd(queryString, "&");

    URI uri = new URI(fhirStoreConfiguration.getProtocol(), null,
        fhirStoreConfiguration.getHosname(), fhirStoreConfiguration.getPort(), path, queryString,
        null);

    System.out.println(uri);

    ResponseEntity<String> responseEntity =
        restTemplate.exchange(uri, method, new HttpEntity<>(body), String.class);

    final String responseBody = responseEntity.getBody();

    if (validateBsnInProxyResults) {
      final IBaseResource resource = fhirService.parseResource(responseBody);
      if (resource instanceof Patient) {
        validateMatchingBsn(requestingUserBsn, (Patient) resource);
      }
    }

    return new ResponseEntity<>(responseBody, HttpStatus.OK);
  }

  //TODO: move to converion controller

  @PostMapping(value = "convert/astraia/patient", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> convertAstraiaMessage(@RequestBody String astraiaMessage,
      HttpServletRequest request) {

    //TODO: Decide on security

    final Bundle bundle = astraiaConversionService.convertToBundle(astraiaMessage);

    String response = fhirService.sendBundle(bundle);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  private CredentialSubject introspectAndAuthenticate(HttpServletRequest request) {

    final TokenIntrospectionResponse nutsIntrospectionResult = nutsService.introspectBearerToken(
        request);

    if (!nutsIntrospectionResult.isActive()) {
      throw new IllegalStateException("Token introspection returned inactive");
    }

    //GET AUTH
    final ResolutionResult authenticationCredential = nutsService.getAuthorization(
        nutsIntrospectionResult.getVcs().get(0));

    if (authenticationCredential.getCurrentStatus() != CurrentStatusEnum.TRUSTED) {
      //TODO: POC msg, give less info in prd
      throw new IllegalStateException("AUTH VC NOT TRUSTED");
    }

    return authenticationCredential.getVerifiableCredential().getCredentialSubject();
  }

  private String getLogicalId(Patient patient) {
    return patient.getIdElement().toUnqualifiedVersionless().getValue();
  }

  private void validateMatchingBsn(String requestingUserBsn, Patient patient) {

    final boolean bsnMatches = patient.getIdentifier().stream()
        .anyMatch(identifier -> FHIR__IDENTIFIER_SYSTEM_BSN.equals(identifier.getSystem())
            && requestingUserBsn.equals(identifier.getValue()));

    if (!bsnMatches) {
      throw new IllegalArgumentException("BSN does not match - unauthorized");
    }
  }
}
