package nl.headease.babyconnectproxy.service;

import javax.servlet.http.HttpServletRequest;
import nl.nuts.client.api.AuthApi;
import nl.nuts.client.invoker.ApiClient;
import nl.nuts.client.model.TokenIntrospectionResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service that proxies the Bearer token to the NUTS node and extracts token introspection results
 *
 */
@Service
public class NutsProxyService {
  private static final Logger LOG = LoggerFactory.getLogger(NutsProxyService.class);
  private final AuthApi authApi;

  public NutsProxyService(@Value("${nuts.node.url}") String nutsNodeEndpoint) {
    final RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
    final RestTemplate restTemplate = restTemplateBuilder.rootUri(nutsNodeEndpoint).build();

    authApi = new AuthApi(new ApiClient(restTemplate));
  }

  public TokenIntrospectionResponse introspectBearerToken(HttpServletRequest request) {

    final String authentication = request.getHeader("Authentication");

    if(!StringUtils.startsWith(authentication, "Bearer ")) {
      throw new IllegalArgumentException("No Bearer Token found");
    }

    return authApi.introspectAccessToken(StringUtils.substringAfter(authentication, "Bearer "));
  }
}
