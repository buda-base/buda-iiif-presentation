package io.bdrc.iiif.presentation.resmodels;

import static io.bdrc.iiif.presentation.AppConstants.BDO;
import static io.bdrc.iiif.presentation.AppConstants.BDR;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.TMPPREFIX;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resservices.ImageGroupInfoService;

public class ImageGroupInfo {

    @JsonProperty("access")
    public AccessType access;
    @JsonProperty("restrictedInChina")
    public Boolean restrictedInChina = false;
    @JsonProperty("status")
    public String statusUri;
    @JsonProperty("instanceUri")
    public String instanceUri;
    @JsonProperty("imageInstanceUri")
    public String imageInstanceUri;
    @JsonProperty("copyrightStatusLname")
    public String copyrightStatusLname = "CopyrightPublicDomain";
    @JsonProperty("pagesIntroTbrc")
    public Integer pagesIntroTbrc = 0;
    @JsonProperty("volumeNumber")
    public Integer volumeNumber = 1;
    @JsonProperty("imageGroup")
    public String imageGroup = null;
    @JsonProperty("iiifManifestUri")
    public URI iiifManifestUri = null;
    @JsonProperty("partInfo")
    public List<PartInfo> partInfo = null;
    @JsonProperty("labels")
    public List<LangString> labels = null;

    private static final Logger logger = LoggerFactory.getLogger(ImageGroupInfoService.class);

    // result of volumeInfo query
    public ImageGroupInfo(final QuerySolution sol, final String volumeId) {
        logger.debug("creating VolumeInfo for solution {}", sol.toString());
        if (volumeId.startsWith("bdr:")) {
            this.imageGroup = volumeId.substring(4);
        } else {
            this.imageGroup = volumeId;
        }
        this.access = AccessType.fromString(sol.getResource("access").getURI());
        this.statusUri = sol.getResource("status").getURI();
        this.instanceUri = sol.getResource("instanceId").getURI();
        this.imageInstanceUri = sol.getResource("iinstanceId").getURI();
        if (sol.contains("?ric")) {
            this.restrictedInChina = sol.get("?ric").asLiteral().getBoolean();
        }
        if (sol.contains("?volumeLabel")) {
            if (this.labels == null) {
                this.labels = new ArrayList<>();
            }
            final Literal l = sol.get("?volumeLabel").asLiteral();
            this.labels.add(new LangString(l)); 
        }
        if (sol.contains("?volumeNumber")) {
            this.volumeNumber = sol.get("?volumeNumber").asLiteral().getInt();
        }
        if (sol.contains("?pagesIntroTbrc")) {
            this.pagesIntroTbrc = sol.get("?pagesIntroTbrc").asLiteral().getInt();
        }
        if (sol.contains("?iiifManifest")) {
            final String manifestURIString = sol.getResource("?iiifManifest").getURI();
            try {
                this.iiifManifestUri = new URI(manifestURIString);
            } catch (URISyntaxException e) {
                logger.error("problem converting sparql result to URI: " + manifestURIString, e);
            }
        }
    }

    private void fromModel(final Model m, String volumeId) throws BDRCAPIException {
        logger.debug("creating VolumeInfo for model, volumeId {}", volumeId);
        // the model is supposed to come from the IIIFPres_volumeOutline graph query
        if (volumeId.startsWith("bdr:"))
            volumeId = BDR + volumeId.substring(4);
        final Resource volume = m.getResource(volumeId);
        if (volume == null)
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid model: missing volume");
        // checking type (needs to be a bdo:Volume)
        final Triple t = new Triple(volume.asNode(), RDF.type.asNode(), m.getResource(BDO + "VolumeImageAsset").asNode());
        final ExtendedIterator<Triple> ext = m.getGraph().find(t);
        if (!ext.hasNext()) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid model: not a volume");
        }
        final Resource imageInstance = volume.getPropertyResourceValue(m.getProperty(BDO, "volumeOf"));
        if (imageInstance == null) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid model: no associated item");
        }
        this.imageInstanceUri = imageInstance.getURI();

        this.imageGroup = volume.getLocalName();

        final Statement iiifManifestS = volume.getProperty(m.getProperty(BDO, "hasIIIFManifest"));
        if (iiifManifestS != null) {
            try {
                this.iiifManifestUri = new URI(iiifManifestS.getResource().getURI());
            } catch (URISyntaxException e) {
                logger.error("problem converting sparql graph result to URI: " + iiifManifestS.getResource().getURI(), e);
            }
        }

        final Statement volumeNumberS = volume.getProperty(m.getProperty(BDO, "volumeNumber"));
        if (volumeNumberS != null) {
            this.volumeNumber = volumeNumberS.getInt();
        }
        
        // TODO: use statement iterator
        final Statement prefLabelS = volume.getProperty(SKOS.prefLabel);
        if (prefLabelS != null) {
            if (this.labels == null) {
                this.labels = new ArrayList<>();
            }
            this.labels.add(new LangString(prefLabelS.getLiteral())); 
        }

        final Statement volumePagesTbrcIntroS = volume.getProperty(m.getProperty(BDO, "volumePagesTbrcIntro"));
        if (volumePagesTbrcIntroS != null) {
            this.pagesIntroTbrc = volumePagesTbrcIntroS.getInt();
        }

        final Resource instance = imageInstance.getPropertyResourceValue(m.getProperty(BDO, "instanceReproductionOf"));
        if (instance == null) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid model: no associated work");
        }
        this.instanceUri = instance.getURI();

        final Resource access = volume.getPropertyResourceValue(m.getProperty(TMPPREFIX, "rootAccess"));
        if (access != null) {
            this.access = AccessType.fromString(access.getURI());
        } else {
            logger.warn("cannot find model access for {}", instanceUri);
            this.access = AccessType.fromString(BDR + "AccessRestrictedByTbrc");
        }

        final Resource status = volume.getPropertyResourceValue(m.getProperty(TMPPREFIX, "rootStatus"));
        if (access != null) {
            this.statusUri = status.getURI();
        } else {
            logger.warn("cannot find model status for {}", instanceUri);
            this.statusUri = null;
        }

        final Statement restrictedInChinaS = volume.getProperty(m.getProperty(TMPPREFIX, "rootRestrictedInChina"));
        if (restrictedInChinaS == null) {
            this.restrictedInChina = true;
        } else {
            this.restrictedInChina = restrictedInChinaS.getBoolean();
        }

        this.partInfo = InstanceInfo.getParts(m, instance, null);
        // this.labels = getLabels(m, volume);
    }

    public ImageGroupInfo(final Model m, final String volumeId) throws BDRCAPIException {
        fromModel(m, volumeId);
    }

    public ImageGroupInfo(final Model m, String instanceId, String volumeId) throws BDRCAPIException {
        // result of IIIFPres_workGraph_noItem
        if (volumeId != null) {
            fromModel(m, volumeId);
            return;
        }
        if (instanceId.startsWith("bdr:"))
            instanceId = BDR + instanceId.substring(4);
        final Resource instance = m.getResource(instanceId);
        final Resource firstImageGroup = instance.getPropertyResourceValue(m.getProperty(TMPPREFIX, "firstImageGroup"));
        if (firstImageGroup != null) {
            volumeId = firstImageGroup.getURI();
            fromModel(m, volumeId);
        } else {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "cannot get image group from instance model");
        }
    }

    public ImageGroupInfo() {
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "toString objectmapper exception, this shouldn't happen";
        }
    }
}
