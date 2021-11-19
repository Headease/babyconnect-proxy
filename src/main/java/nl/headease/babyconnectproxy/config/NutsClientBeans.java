package nl.headease.babyconnectproxy.config;

import nl.nuts.client.auth.api.AuthApi;
import nl.nuts.client.auth.invoker.ApiClient;
import nl.nuts.client.did.api.SearchApi;
import nl.nuts.client.vcr.api.CredentialApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class NutsClientBeans {

  private final RestTemplate restTemplate;

  public NutsClientBeans(@Value("${nuts.node.url}") String nutsNodeEndpoint) {
    final RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
    restTemplate = restTemplateBuilder.rootUri(nutsNodeEndpoint).build();
  }

  @Bean
  public AuthApi authApi() {
    return new AuthApi(new ApiClient(restTemplate));
  }

  @Bean
  public CredentialApi credentialApi() {
    return new CredentialApi(new nl.nuts.client.vcr.invoker.ApiClient(restTemplate));
  }

  @Bean
  public SearchApi searchApi() {
    return new SearchApi(new nl.nuts.client.did.invoker.ApiClient(restTemplate));
  }
}
