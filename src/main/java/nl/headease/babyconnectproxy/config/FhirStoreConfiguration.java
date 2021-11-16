package nl.headease.babyconnectproxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "fhir.server")
public class FhirStoreConfiguration {

  private String protocol;
  private String hosname;
  private Integer port;
  private String endpoint;

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getHosname() {
    return hosname;
  }

  public void setHosname(String hosname) {
    this.hosname = hosname;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public String toString() {
    return "FhirStoreConfiguration{" +
        "protocol='" + protocol + '\'' +
        ", port='" + port + '\'' +
        ", hosname='" + hosname + '\'' +
        ", endpoint='" + endpoint + '\'' +
        '}';
  }
}
