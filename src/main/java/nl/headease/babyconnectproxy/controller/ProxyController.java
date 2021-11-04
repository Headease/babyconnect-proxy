package nl.headease.babyconnectproxy.controller;

import org.hl7.fhir.dstu3.model.Patient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/proxy")
public class ProxyController {

  @PostMapping(value = "convert/astraia/patients", consumes = MediaType.APPLICATION_XML_VALUE)
  public Patient convertAstraiaMessage(@RequestBody String astraiaMessage) {

    return new Patient();
  }
}
