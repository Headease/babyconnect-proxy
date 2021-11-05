package nl.headease.babyconnectproxy.service;

import javax.servlet.http.HttpServletRequest;
import nl.headease.babyconnectproxy.model.NutsIntrospectionResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service that proxies the Bearer token to the NUTS node and extracts token introspection results
 *
 */
@Service
public class NutsProxyService {
  private static final Logger LOG = LoggerFactory.getLogger(NutsProxyService.class);
  private final String fhirEndpoint;

  public NutsProxyService(@Value("${fhir.server.url}") String fhirEndpoint) {
    this.fhirEndpoint = fhirEndpoint;
  }

  public NutsIntrospectionResult introspectBearerToken(HttpServletRequest request) {

    final String authentication = request.getHeader("Authentication");

    if(StringUtils.isNotBlank(authentication)) {
      LOG.info("Introspecting token: " + authentication);
    }

    LOG.warn("MOCKING NutsIntrospectionResponse - not yet implemented");

    //TODO: Let NUTS introspect the bearer and extract the endpoint instead of injecting it from configuration
    final NutsIntrospectionResult nutsIntrospectionResult = new NutsIntrospectionResult();
    nutsIntrospectionResult.setServiceEndpoint(fhirEndpoint);

    return nutsIntrospectionResult;
  }
}
