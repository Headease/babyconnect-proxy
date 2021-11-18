package nl.headease.babyconnectproxy.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.apache.xerces.dom.DeferredElementImpl;
import org.hl7.fhir.dstu3.model.EpisodeOfCare;
import org.hl7.fhir.dstu3.model.EpisodeOfCare.EpisodeOfCareStatus;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Identifier.IdentifierUse;
import org.hl7.fhir.dstu3.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class Astraia2FhirEpisodeOfCareXmlConverter extends BaseConverter {
  public static final String FHIR__IDENTIFIER_SYSTEM_ASTRAIA_EPISODE_ID = "http://astraia.nl/fhir/episode-id";

  private static final Logger LOG = LoggerFactory.getLogger(Astraia2FhirEpisodeOfCareXmlConverter.class);
  private final static String XPATH__EPISODES = "/export/Episode/record";

  private final XPathExpression xPathExpressionEpisodes;

  public Astraia2FhirEpisodeOfCareXmlConverter() {
    xPathExpressionEpisodes = getXPathExpression(XPATH__EPISODES);
  }

  public List<EpisodeOfCare> convert(Document astraiaDocument, Reference patientReference) {

    List<EpisodeOfCare> episodesOfCare = new ArrayList<>();

    try {
      final NodeList episodes = (NodeList) xPathExpressionEpisodes.evaluate(astraiaDocument, XPathConstants.NODESET);

      if(episodes != null) {
        for (int i = 0; i < episodes.getLength(); i++) {
          Node episode = episodes.item(i);
          final String id = "astraia-" + ((DeferredElementImpl) episode).getAttribute("id");

          EpisodeOfCare episodeOfCare = new EpisodeOfCare();
          episodeOfCare.setId(id);
          episodeOfCare.setStatus(EpisodeOfCareStatus.ACTIVE);
          episodeOfCare.setPatient(patientReference);

          final Identifier identifier = new Identifier();
          identifier.setSystem(FHIR__IDENTIFIER_SYSTEM_ASTRAIA_EPISODE_ID);
          identifier.setUse(IdentifierUse.SECONDARY);
          identifier.setValue(id);

          episodeOfCare.setIdentifier(Collections.singletonList(identifier));

          episodesOfCare.add(episodeOfCare);
        }
      }

      return episodesOfCare;

    } catch (XPathExpressionException e) {
      throw new IllegalArgumentException("Cannot evaluate Astraia document from XPath", e);
    }
  }
}
