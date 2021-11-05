package nl.headease.babyconnectproxy.model;

//TODO: Figure out what the actual response looks like, and how properly map the returned DIDs onto endspoint from the registry
public class NutsIntrospectionResult {

  private String serviceEndpoint;

  public String getServiceEndpoint() {
    return serviceEndpoint;
  }

  public void setServiceEndpoint(String serviceEndpoint) {
    this.serviceEndpoint = serviceEndpoint;
  }

  @Override
  public String toString() {
    return "NutsIntrospectionResult{" +
        "serviceEndpoint='" + serviceEndpoint + '\'' +
        '}';
  }
}
