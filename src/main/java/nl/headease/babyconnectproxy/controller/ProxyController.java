package nl.headease.babyconnectproxy.controller;

import nl.headease.babyconnectproxy.service.AstraiaConversionService;
import nl.headease.babyconnectproxy.service.FhirService;
import org.hl7.fhir.dstu3.model.Patient;
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

  private final AstraiaConversionService astraiaConversionService;
  private final FhirService fhirService;

  public ProxyController(AstraiaConversionService astraiaConversionService,
      FhirService fhirService) {
    this.astraiaConversionService = astraiaConversionService;
    this.fhirService = fhirService;
  }

  @PostMapping(value = "convert/astraia/patient", consumes = MediaType.APPLICATION_XML_VALUE,  produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> convertAstraiaMessage(@RequestBody String astraiaMessage) {



    final Patient patient = astraiaConversionService.convertToFhirPatient(astraiaMessage);

    String response = fhirService.ensurePatient(patient);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
