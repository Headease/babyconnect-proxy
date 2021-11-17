package nl.headease.babyconnectproxy.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import nl.headease.babyconnectproxy.model.CreateVerifiableCredentialRequest;
import nl.nuts.client.auth.api.AuthApi;
import nl.nuts.client.auth.model.TokenIntrospectionResponse;
import nl.nuts.client.vcr.api.CredentialApi;
import nl.nuts.client.vcr.model.CredentialSubject;
import nl.nuts.client.vcr.model.IssueVCRequest;
import nl.nuts.client.vcr.model.LegalBase;
import nl.nuts.client.vcr.model.LegalBase.ConsentTypeEnum;
import nl.nuts.client.vcr.model.Resource;
import nl.nuts.client.vcr.model.VerifiableCredential;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.EpisodeOfCare;
import org.hl7.fhir.dstu3.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service that proxies the Bearer token to the NUTS node and extracts token introspection results
 *
 */
@Service
public class NutsService {
  private static final Logger LOG = LoggerFactory.getLogger(NutsService.class);

  private final List<String> resourceOperations = Collections.singletonList("read"); //can never change in the current Bolt spec

  private final AuthApi authApi;
  private final CredentialApi credentialApi;
  private final FhirService fhirService;

  private final String careOrganisationDid;

  public NutsService(AuthApi authApi,
      CredentialApi credentialApi,
      FhirService fhirService,
      @Value("${nuts.node.did.careOrganisation}") String careOrganisationDid) {
    this.authApi = authApi;
    this.credentialApi = credentialApi;
    this.fhirService = fhirService;
    this.careOrganisationDid = careOrganisationDid;
  }

  public TokenIntrospectionResponse introspectBearerToken(HttpServletRequest request) {

    final String authentication = request.getHeader("Authentication");

    if(!StringUtils.startsWith(authentication, "Bearer ")) {
      throw new IllegalArgumentException("No Bearer Token found");
    }

    return authApi.introspectAccessToken(StringUtils.substringAfter(authentication, "Bearer "));
  }


  public VerifiableCredential createVerifiableCredential(CreateVerifiableCredentialRequest createRequest) {

    LOG.info("Handling request: \n" + createRequest);

    //TODO: Only create if not exists
    final IssueVCRequest vcRequest = new IssueVCRequest();

    vcRequest.setCredentialSubject(getCredentialSubject(createRequest));
    vcRequest.setIssuer(careOrganisationDid);
    vcRequest.setType("NutsOrganizationCredential");

    final VerifiableCredential result = credentialApi.create(vcRequest);

    LOG.info("Successfully created VC request: \n" + result);

    return result;
  }

  private CredentialSubject getCredentialSubject(CreateVerifiableCredentialRequest createRequest) {
    final CredentialSubject credentialSubject = new CredentialSubject();

    credentialSubject.setId(createRequest.getSubjectDid());

    final LegalBase legalBase = new LegalBase();
    legalBase.setConsentType(ConsentTypeEnum.EXPLICIT);
    legalBase.setEvidence("evidence");

    credentialSubject.setLegalBase(legalBase);

    credentialSubject.setPurposeOfUse("zorginzage-demo");

    final List<Resource> resources = getResources(createRequest.getBsn());

    if(resources.isEmpty()) {
      throw new IllegalStateException("No EpisodeOfCare resources found");
    }

    credentialSubject.setResources(resources);

    credentialSubject.setSubject("urn:oid:2.16.840.1.113883.2.4.6.3:" + createRequest.getBsn());
    return credentialSubject;
  }

  private List<Resource> getResources(String subjectBsn) {

    final Patient patient = fhirService.getPatientByBsn(subjectBsn);
    final List<EpisodeOfCare> episodeOfCares = fhirService.getEpisodesOfCare(patient);

    return episodeOfCares.stream()
        .map(episodeOfCare -> {
          final Resource resource = new Resource();
          resource.setOperations(resourceOperations);
          resource.setPath(episodeOfCare.getIdElement().toUnqualifiedVersionless().getValue());
          resource.setUserContext(false);
          return resource;
        })
        .collect(Collectors.toList());
  }
}
