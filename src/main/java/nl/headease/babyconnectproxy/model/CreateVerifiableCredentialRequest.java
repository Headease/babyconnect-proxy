package nl.headease.babyconnectproxy.model;

public class CreateVerifiableCredentialRequest {
  private String subjectDid;
  private String bsn;

  public String getSubjectDid() {
    return subjectDid;
  }

  public void setSubjectDid(String subjectDid) {
    this.subjectDid = subjectDid;
  }

  public String getBsn() {
    return bsn;
  }

  public void setBsn(String bsn) {
    this.bsn = bsn;
  }
}
