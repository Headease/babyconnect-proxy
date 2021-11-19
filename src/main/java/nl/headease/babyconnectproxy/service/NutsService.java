package nl.headease.babyconnectproxy.service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import nl.headease.babyconnectproxy.model.CreateVerifiableCredentialRequest;
import nl.nuts.client.auth.api.AuthApi;
import nl.nuts.client.auth.model.TokenIntrospectionResponse;
import nl.nuts.client.did.api.SearchApi;
import nl.nuts.client.did.model.OrganizationSearchResult;
import nl.nuts.client.vcr.api.CredentialApi;
import nl.nuts.client.vcr.model.CredentialSubject;
import nl.nuts.client.vcr.model.IssueVCRequest;
import nl.nuts.client.vcr.model.LegalBase;
import nl.nuts.client.vcr.model.LegalBase.ConsentTypeEnum;
import nl.nuts.client.vcr.model.LegalBaseEvidence;
import nl.nuts.client.vcr.model.ResolutionResult;
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
 * Service handles communication with the NUTS node
 */
@Service
public class NutsService {
  private static final Logger LOG = LoggerFactory.getLogger(NutsService.class);

  private final DateTimeFormatter rfc3337Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
  private final List<String> resourceOperations = Collections.singletonList("read"); //can never change in the current Bolt spec

  private final AuthApi authApi;
  private final CredentialApi credentialApi;
  private final SearchApi didSearchApi;
  private final FhirService fhirService;


  private final String careOrganisationDid;

  public NutsService(AuthApi authApi,
      CredentialApi credentialApi,
      SearchApi didSearchApi, FhirService fhirService,
      @Value("${nuts.node.did.careOrganisation}") String careOrganisationDid) {
    this.authApi = authApi;
    this.credentialApi = credentialApi;
    this.didSearchApi = didSearchApi;
    this.fhirService = fhirService;
    this.careOrganisationDid = careOrganisationDid;
  }

  public TokenIntrospectionResponse introspectBearerToken(HttpServletRequest request) {

    final String authentication = request.getHeader("Authorization");

    if(!StringUtils.startsWith(authentication, "Bearer ")) {
      throw new IllegalArgumentException("No Bearer Token found");
    }

    return authApi.introspectAccessToken(StringUtils.substringAfter(authentication, "Bearer "));
  }

  public ResolutionResult getAuthorization(String did) {
    return credentialApi.resolve(did, null);
  }


  public VerifiableCredential createVerifiableCredential(CreateVerifiableCredentialRequest createRequest) {

    LOG.info("Handling request: \n" + createRequest);

    //TODO: Only create if not exists
    final IssueVCRequest vcRequest = new IssueVCRequest();

    vcRequest.setCredentialSubject(getCredentialSubject(createRequest));
    vcRequest.setIssuer(careOrganisationDid);
    vcRequest.setType("NutsAuthorizationCredential");

    //TODO: 2 days is too long, only for hackathon!
    final ZonedDateTime expirationDate = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS)
        .plus(2, ChronoUnit.DAYS);
    vcRequest.setExpirationDate(expirationDate.format(rfc3337Formatter));

    final VerifiableCredential result = credentialApi.create(vcRequest);

    LOG.info("Successfully created VC request: \n" + result);

    return result;
  }

  public List<OrganizationSearchResult> searchOrganizations() {
    return didSearchApi.searchOrganizations("", null);
  }

  private CredentialSubject getCredentialSubject(CreateVerifiableCredentialRequest createRequest) {
    final CredentialSubject credentialSubject = new CredentialSubject();

    credentialSubject.setId(createRequest.getSubjectDid());

    final LegalBase legalBase = new LegalBase();
    legalBase.setConsentType(ConsentTypeEnum.EXPLICIT);
    final LegalBaseEvidence evidence = new LegalBaseEvidence();
    evidence.setPath("path");
    evidence.setType("type");
    legalBase.setEvidence(evidence);

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
          resource.setUserContext(true);
          return resource;
        })
        .collect(Collectors.toList());
  }
}
