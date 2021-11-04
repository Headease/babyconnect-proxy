package nl.headease.babyconnectproxy.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import nl.headease.babyconnectproxy.service.AstraiaConversionService;
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
  private final FhirContext fhirContext = FhirContext.forDstu3();
  private final IParser parser = fhirContext.newJsonParser();


  public ProxyController(AstraiaConversionService astraiaConversionService) {
    this.astraiaConversionService = astraiaConversionService;
  }

  @PostMapping(value = "convert/astraia/patient", consumes = MediaType.APPLICATION_XML_VALUE,  produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> convertAstraiaMessage(@RequestBody String astraiaMessage) {

    final Patient patient = astraiaConversionService.convertToFhirPatient(astraiaMessage);

    return new ResponseEntity<>(
        parser.encodeResourceToString(patient),
        HttpStatus.OK
    );
  }
}
