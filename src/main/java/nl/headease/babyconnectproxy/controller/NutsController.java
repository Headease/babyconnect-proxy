package nl.headease.babyconnectproxy.controller;

import java.util.List;
import nl.headease.babyconnectproxy.model.CreateVerifiableCredentialRequest;
import nl.headease.babyconnectproxy.service.NutsService;
import nl.nuts.client.did.model.OrganizationSearchResult;
import nl.nuts.client.vcr.model.VerifiableCredential;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/nuts")
@CrossOrigin(origins = "*")
public class NutsController {

  private final NutsService nutsService;

  public NutsController(NutsService nutsService) {
    this.nutsService = nutsService;
  }

  @PostMapping("vc")
  public ResponseEntity<VerifiableCredential> createVerifiableCredential(@RequestBody CreateVerifiableCredentialRequest createRequest) {

    //TODO: Open endpoint, this should be embedded into BovenMaas eventually - POC CODE

    final VerifiableCredential verifiableCredential = nutsService.createVerifiableCredential(
        createRequest);

    return new ResponseEntity<>(verifiableCredential, HttpStatus.OK);
  }

  @GetMapping(value = "organizations",  produces = MediaType.APPLICATION_JSON_VALUE)
  public List<OrganizationSearchResult> getTrustedOrganizations() {
    return nutsService.searchOrganizations();
  }
}
