package nl.headease.babyconnectproxy.config;

import nl.nuts.client.auth.api.AuthApi;
import nl.nuts.client.auth.invoker.ApiClient;
import nl.nuts.client.vcr.api.CredentialApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class NutsClientBeans {

  private final String nutsNodeEndpoint;

  public NutsClientBeans(@Value("${nuts.node.url}") String nutsNodeEndpoint) {
    this.nutsNodeEndpoint = nutsNodeEndpoint;
  }

  @Bean
  public AuthApi authApi() {
    final RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
    final RestTemplate restTemplate = restTemplateBuilder.rootUri(nutsNodeEndpoint).build();

    return new AuthApi(new ApiClient(restTemplate));
  }

  @Bean
  public CredentialApi credentialApi() {
    final RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
    final RestTemplate restTemplate = restTemplateBuilder.rootUri(nutsNodeEndpoint).build();

    return new CredentialApi(new nl.nuts.client.vcr.invoker.ApiClient(restTemplate));
  }

}
