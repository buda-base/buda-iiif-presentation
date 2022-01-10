package io.bdrc.iiif.presentation.resmodels;

import static io.bdrc.iiif.presentation.AppConstants.ADM;
import static io.bdrc.iiif.presentation.AppConstants.BDO;
import static io.bdrc.iiif.presentation.AppConstants.BDR;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.TMPPREFIX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;

public class InstanceInfo extends PartInfo {
    
    private static final Logger logger = LoggerFactory.getLogger(InstanceInfo.class);

    @JsonProperty("rootAccess")
    public AccessType rootAccess = null;
    @JsonProperty("rootRestrictedInChina")
    public Boolean rootRestrictedInChina = false;
    @JsonProperty("rootStatusUri")
    public String rootStatusUri = null;
    @JsonProperty("isRoot")
    public Boolean isRoot = false;
    @JsonProperty("rootInstanceQname")
    public String rootInstanceQname = null;
    @JsonProperty("creatorLabels")
    public List<LangString> creatorLabels = null; // ?
    @JsonProperty("hasLocation")
    public boolean hasLocation = false;
    // prefixed
    @JsonProperty("firstImageGroupQname")
    public String firstImageGroupQname = null;
    // prefixed
    @JsonProperty("imageInstanceId")
    public String imageInstanceQname = null;
    @JsonProperty("isVirtual")
    public boolean isVirtual = false;

    public InstanceInfo() {}

    private void readLocations(final Model m, Resource r) {
        final StmtIterator clItr = r.listProperties(m.getProperty(BDO, "contentLocation"));
        while (clItr.hasNext()) {
            final Statement s = clItr.next();
            final Resource cl = s.getObject().asResource();
            final Property locationInstanceP = m.getProperty(BDO, "contentLocationInstance");
            if (cl.hasProperty(locationInstanceP))
                this.rootInstanceQname = "bdr:"+cl.getProperty(locationInstanceP).getResource().getLocalName();
            if (this.locations == null)
                this.locations = new ArrayList<>();
            this.locations.add(new Location(m, cl));
            this.hasLocation = true;
        }
        if (this.locations != null && this.locations.size() > 1) {
            Collections.sort(this.locations);
        }
    }
    
    public InstanceInfo(final Model m, String instanceId) throws BDRCAPIException {
        // the model is supposed to come from the IIIFPres_workInfo_noItem graph query
        if (instanceId.startsWith("bdr:"))
            instanceId = BDR+instanceId.substring(4);
        final Resource instance = m.getResource(instanceId);
        if (instance == null)
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid model: missing instance "+instanceId);
        // checking type (needs to be a bdo:Work)
        this.partQname = "bdr:"+instance.getLocalName();
        Triple isInstanceT = new Triple(instance.asNode(), RDF.type.asNode(), m.getResource(BDO+"Instance").asNode());
        ExtendedIterator<Triple> ext = m.getGraph().find(isInstanceT);
        if (!ext.hasNext()) {
            isInstanceT = new Triple(instance.asNode(), RDF.type.asNode(), m.getResource(BDO+"ImageInstance").asNode());
            ext = m.getGraph().find(isInstanceT);
            if (!ext.hasNext())
                throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid model, not an instance: "+instanceId);
        }
        final Triple isVirtualInstanceT = new Triple(instance.asNode(), RDF.type.asNode(), m.getResource(BDO+"VirtualInstance").asNode());
        ext = m.getGraph().find(isVirtualInstanceT);
        if (ext.hasNext()) {
            this.isVirtual = true;
        }
        final Resource partOf = instance.getPropertyResourceValue(m.getProperty(BDO, "partOf"));
        this.isRoot = (partOf == null);
        StmtIterator imageInstanceS = instance.listProperties(m.getProperty(TMPPREFIX, "inImageInstance"));
        Resource item = null;
        if (imageInstanceS.hasNext()) {
            item = imageInstanceS.next().getResource();
            // hack for the Taisho: https://github.com/buda-base/buda-iiif-presentation/issues/113
            // could be done better
            if (imageInstanceS.hasNext() && item.getLocalName().equals("W0TT0000"))
                item = imageInstanceS.next().getResource();
            this.imageInstanceQname = "bdr:"+item.getLocalName();
        }
        readLocations(m, instance);
        final Resource firstImageGroup = instance.getPropertyResourceValue(m.getProperty(TMPPREFIX, "firstImageGroup"));
        if (firstImageGroup != null) {
            this.firstImageGroupQname = "bdr:"+firstImageGroup.getLocalName();
        }
        final Resource root_access = instance.getPropertyResourceValue(m.getProperty(TMPPREFIX, "rootAccess"));
        if (root_access != null) {
            this.rootAccess = AccessType.fromString(root_access.getURI());
        }
        final Statement restrictedInChinaS = instance.getProperty(m.getProperty(TMPPREFIX, "rootRestrictedInChina"));
        if (restrictedInChinaS == null) {
            // we allow virtual collections in China. Perhaps some should be restricted in the future?
            this.rootRestrictedInChina = !this.isVirtual;
        } else {
            this.rootRestrictedInChina = restrictedInChinaS.getBoolean();
        }
        final Statement rootStatusS = instance.getProperty(m.getProperty(TMPPREFIX, "rootStatus"));
        if (rootStatusS == null) {
            this.rootStatusUri = null;
            if (this.isVirtual) {
                this.rootStatusUri = "http://purl.bdrc.io/admindata/StatusReleased";
            }
        } else {
            this.rootStatusUri = rootStatusS.getResource().getURI();
        }
        final Resource access = instance.getPropertyResourceValue(m.getProperty(ADM, "access"));
        if (access != null) {
            this.rootAccess = AccessType.fromString(access.getURI());
        }
        if (this.rootAccess == null) {
            if (this.isVirtual) {
                this.rootAccess = AccessType.OPEN;
            } else {
                logger.warn("cannot find model access for {}", instanceId);
                this.rootAccess = AccessType.RESTR_BDRC;                
            }
        }

        final Resource pType = instance.getPropertyResourceValue(partTypeP);
        if (pType != null) {
            final String pTypeURI = pType.getURI();
            if (pTypeURI.endsWith("Section"))
                this.partType = PartType.SECTION;
            else if (pTypeURI.endsWith("Volume"))
                this.partType = PartType.VOLUME;
        } else {
            this.partType = PartType.SECTION;
        }
        
        this.parts = getParts(m, instance, null);
        this.labels = getLabels(m, instance);
        
        final Resource linkTo = instance.getPropertyResourceValue(m.getProperty(BDO, "virtualLinkTo"));
        if (linkTo != null) {
            this.linkToQname = "bdr:"+linkTo.getLocalName();
            final Resource linkToType = linkTo.getPropertyResourceValue(RDF.type);
            if (linkToType != null) {
                this.linkToTypeLname = linkToType.getLocalName();
            }
            if (this.parts == null) {
                this.parts = getParts(m, linkTo, null);
            }
            if (this.labels == null) {
                this.labels = getLabels(m, linkTo);
            }
            if (this.locations == null) {
                readLocations(m, linkTo);
            }
            this.isRoot = (linkTo.getPropertyResourceValue(m.getProperty(BDO, "partOf")) == null);
        }
        
        // creator labels
        final StmtIterator creatorLabelItr = instance.listProperties(m.createProperty(TMPPREFIX, "workCreatorLit"));
        if (creatorLabelItr.hasNext()) {
            final List<LangString> creatorLabels = new ArrayList<>();
            while (creatorLabelItr.hasNext()) {
                final Statement s = creatorLabelItr.next();
                final Literal l = s.getObject().asLiteral();
                creatorLabels.add(new LangString(l));
            }
            this.creatorLabels = creatorLabels;
        }
    }

    public static List<LangString> getLabels(final Model m, final Resource work) {
        final StmtIterator labelItr = work.listProperties(SKOS.prefLabel);
        if (labelItr.hasNext()) {
            final List<LangString> labels = new ArrayList<>();
            while (labelItr.hasNext()) {
                final Statement s = labelItr.next();
                final Literal l = s.getObject().asLiteral();
                labels.add(new LangString(l));
            }
            return labels;
        }
        return null;
    }
    
    final static public Property partTypeP = ResourceFactory.createProperty(BDO, "partType");
    
    // this is recursive, and assumes no loop
    public static List<PartInfo> getParts(final Model m, final Resource work, final Map<String,PartInfo> shortIdPartMap) {
        final StmtIterator partsItr = work.listProperties(m.getProperty(BDO, "hasPart"));
        if (partsItr.hasNext()) {
            final Property partIndexP = m.getProperty(BDO, "partIndex");
            final List<PartInfo> parts = new ArrayList<>();
            while (partsItr.hasNext()) {
                final Statement s = partsItr.next();
                final Resource part = s.getObject().asResource();
                final String partId = "bdr:"+part.getLocalName(); // TODO: could be handled better
                final Statement partIndexS = part.getProperty(partIndexP);
                final PartInfo partInfo;
                if (partIndexS == null)
                    partInfo = new PartInfo(partId, null);
                else
                    partInfo = new PartInfo(partId, partIndexS.getInt());
                if (shortIdPartMap != null)
                    shortIdPartMap.put(partId, partInfo);
                final Resource linkTo = part.getPropertyResourceValue(m.getProperty(BDO, "virtualLinkTo"));
                if (linkTo != null) {
                    partInfo.linkToQname = "bdr:"+linkTo.getLocalName();
                    final Resource linkToType = linkTo.getPropertyResourceValue(RDF.type);
                    if (linkToType != null) {
                        partInfo.linkToTypeLname = linkToType.getLocalName();
                    }
                }
                final Resource pType = part.getPropertyResourceValue(partTypeP);
                if (pType != null) {
                    final String pTypeURI = pType.getURI();
                    if (pTypeURI.endsWith("Section"))
                        partInfo.partType = PartType.SECTION;
                    else if (pTypeURI.endsWith("Volume"))
                        partInfo.partType = PartType.VOLUME;
                }
                partInfo.locations = ImageInstanceInfo.getLocations(m, part);
                partInfo.labels = getLabels(m, part);
                partInfo.parts = getParts(m, part, shortIdPartMap);
                if (partInfo.labels == null && linkTo != null) {
                    partInfo.labels = getLabels(m, linkTo);
                }
                if (partInfo.parts == null && linkTo != null) {
                    partInfo.parts = getParts(m, linkTo, shortIdPartMap);
                }
                if (partInfo.locations != null || partInfo.labels != null || partInfo.parts != null || partInfo.linkToQname != null)
                    parts.add(partInfo);
            }
            Collections.sort(parts);
            return parts;
        }
        return null;
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
