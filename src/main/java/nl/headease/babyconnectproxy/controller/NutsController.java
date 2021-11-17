package nl.headease.babyconnectproxy.controller;

import nl.headease.babyconnectproxy.model.CreateVerifiableCredentialRequest;
import nl.headease.babyconnectproxy.service.NutsService;
import nl.nuts.client.vcr.model.VerifiableCredential;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/nuts")
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
}
