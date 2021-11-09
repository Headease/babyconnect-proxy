package nl.headease.babyconnectproxy.controller;

import javax.servlet.http.HttpServletRequest;
import nl.headease.babyconnectproxy.model.NutsIntrospectionResult;
import nl.headease.babyconnectproxy.service.AstraiaConversionService;
import nl.headease.babyconnectproxy.service.FhirService;
import nl.headease.babyconnectproxy.service.NutsProxyService;
import org.hl7.fhir.dstu3.model.Bundle;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/proxy")
public class ProxyController {

  private final NutsProxyService nutsProxyService;
  private final AstraiaConversionService astraiaConversionService;
  private final FhirService fhirService;

  public ProxyController(NutsProxyService nutsProxyService,
      AstraiaConversionService astraiaConversionService,
      FhirService fhirService) {
    this.nutsProxyService = nutsProxyService;
    this.astraiaConversionService = astraiaConversionService;
    this.fhirService = fhirService;
  }

  @PostMapping(value = "convert/astraia/patient", consumes = MediaType.APPLICATION_XML_VALUE,  produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> convertAstraiaMessage(@RequestBody String astraiaMessage, HttpServletRequest  request) {

    final NutsIntrospectionResult nutsIntrospectionResult = nutsProxyService.introspectBearerToken(request);

    final Bundle bundle =  astraiaConversionService.convertToBundle(astraiaMessage);

    String response = fhirService.sendBundle(bundle, nutsIntrospectionResult);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
