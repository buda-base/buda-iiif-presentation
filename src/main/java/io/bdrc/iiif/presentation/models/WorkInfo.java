package io.bdrc.iiif.presentation.models;

import static io.bdrc.iiif.presentation.AppConstants.ADM;
import static io.bdrc.iiif.presentation.AppConstants.BDO;
import static io.bdrc.iiif.presentation.AppConstants.BDR;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.TMPPREFIX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;

public class WorkInfo {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkInfo.class);

    @JsonProperty("rootAccess")
    public AccessType rootAccess = null;
    @JsonProperty("rootRestrictedInChina")
    public Boolean rootRestrictedInChina = false;
    @JsonProperty("rootStatus")
    public String rootStatus = null;
    @JsonProperty("isRoot")
    public Boolean isRoot = false;
    @JsonProperty("rootWorkId")
    public String rootWorkId = null;
    @JsonProperty("parts")
    public List<PartInfo> parts = null;
    @JsonProperty("labels")
    public List<LangString> labels = null; // ?
    @JsonProperty("creatorLabels")
    public List<LangString> creatorLabels = null; // ?
    @JsonProperty("hasLocation")
    public boolean hasLocation = false;
    // prefixed
    @JsonProperty("firstVolumeId")
    public String firstVolumeId = null;
    @JsonProperty("location")
    public Location location = null;
    @JsonProperty("linkTo")
    public String linkTo = null;
    @JsonProperty("linkToType")
    public String linkToType = null;
    // prefixed
    @JsonProperty("itemId")
    public String itemId = null;

    public WorkInfo() {}

    public void readLocation(final Model m, final Resource location) {
        this.location = new Location(m, location);
        final Property locationWorkP = m.getProperty(BDO, "workLocationWork");
        if (location.hasProperty(locationWorkP))
            this.rootWorkId = "bdr:"+location.getProperty(locationWorkP).getResource().getLocalName();
        this.hasLocation = true;
    }

    public WorkInfo(final Model m, String workId) throws BDRCAPIException {
        // the model is supposed to come from the IIIFPres_workInfo_noItem graph query
        if (workId.startsWith("bdr:"))
            workId = BDR+workId.substring(4);
        final Resource work = m.getResource(workId);
        if (work == null)
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid model: missing work");
        // checking type (needs to be a bdo:Work)
        final Triple isWorkT = new Triple(work.asNode(), RDF.type.asNode(), m.getResource(BDO+"Work").asNode());
        ExtendedIterator<Triple> ext = m.getGraph().find(isWorkT);
        if (!ext.hasNext()) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid model: not a work");
        }
        final Triple isVirtualWorkT = new Triple(work.asNode(), RDF.type.asNode(), m.getResource(BDO+"VirtualWork").asNode());
        ext = m.getGraph().find(isVirtualWorkT);
        boolean isVirtual = false;
        if (ext.hasNext()) {
            isVirtual = true;
        }
        final Resource partOf = work.getPropertyResourceValue(m.getProperty(BDO, "workPartOf"));
        if (partOf == null) {
            this.isRoot = true;
        } else {
            this.isRoot = false;
        }
        Resource item = work.getPropertyResourceValue(m.getProperty(TMPPREFIX, "inItem"));
        if (item == null) {
            this.hasLocation = false;
        } else {
            this.itemId = "bdr:"+item.getLocalName();
            final Resource location = work.getPropertyResourceValue(m.getProperty(BDO, "workLocation"));
            if (location == null) {
                this.hasLocation = false;
            } else {
                readLocation(m, location);
            }
        }
        final Resource firstVolume = work.getPropertyResourceValue(m.getProperty(TMPPREFIX, "firstVolume"));
        if (firstVolume != null) {
            this.firstVolumeId = "bdr:"+firstVolume.getLocalName();
        }
        
        final Resource root_access = work.getPropertyResourceValue(m.getProperty(TMPPREFIX, "rootAccess"));
        if (root_access != null) {
            this.rootAccess = AccessType.fromString(root_access.getURI());
        }
        final Statement restrictedInChinaS = work.getProperty(m.getProperty(TMPPREFIX, "rootRestrictedInChina"));
        if (restrictedInChinaS == null) {
            this.rootRestrictedInChina = true;
        } else {
            this.rootRestrictedInChina = restrictedInChinaS.getBoolean();
        }
        final Statement rootStatusS = work.getProperty(m.getProperty(TMPPREFIX, "rootStatus"));
        if (rootStatusS == null) {
            this.rootStatus = null;
        } else {
            this.rootStatus = rootStatusS.getResource().getURI();
        }
        final Resource access = work.getPropertyResourceValue(m.getProperty(ADM, "access"));
        if (access != null) {
            this.rootAccess = AccessType.fromString(access.getURI());
        }
        if (this.rootAccess == null) {
            if (isVirtual) {
                this.rootAccess = AccessType.OPEN;
            } else {
                logger.warn("cannot find model access for {}", workId);
                this.rootAccess = AccessType.RESTR_BDRC;                
            }
        }

        this.parts = getParts(m, work);
        this.labels = getLabels(m, work);
        
        final Resource linkTo = work.getPropertyResourceValue(m.getProperty(BDO, "workLinkTo"));
        if (linkTo != null) {
            this.linkTo = "bdr:"+linkTo.getLocalName();
            final Resource linkToType = linkTo.getPropertyResourceValue(RDF.type);
            if (linkToType != null) {
                this.linkToType = linkToType.getLocalName();
            }
            if (this.parts == null) {
                this.parts = getParts(m, linkTo);
            }
        }
        
        // creator labels
        final StmtIterator creatorLabelItr = work.listProperties(m.createProperty(TMPPREFIX, "workCreatorLit"));
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
    
    // this is recursive, and assumes no loop
    public static List<PartInfo> getParts(final Model m, final Resource work) {
        final StmtIterator partsItr = work.listProperties(m.getProperty(BDO, "workHasPart"));
        if (partsItr.hasNext()) {
            final Property partIndexP = m.getProperty(BDO, "workPartIndex");
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
                final Resource linkTo = work.getPropertyResourceValue(m.getProperty(BDO, "linkTo"));
                if (linkTo != null) {
                    partInfo.linkTo = "bdr:"+linkTo.getLocalName();
                    final Resource linkToType = linkTo.getPropertyResourceValue(RDF.type);
                    if (linkToType != null) {
                        partInfo.linkToType = linkToType.getLocalName();
                    }
                }
                final Resource location = part.getPropertyResourceValue(m.getProperty(BDO, "workLocation"));
                if (location != null)
                    partInfo.location = new Location(m, location);
                partInfo.labels = getLabels(m, part);
                partInfo.subparts = getParts(m, part);
                if (location != null || partInfo.labels != null || partInfo.subparts != null)
                    parts.add(partInfo);
            }
            Collections.sort(parts);
            return parts;
        }
        return null;
    }

    @Override
    public String toString() {
        return "WorkInfo [rootWorkId=" + rootWorkId + ", hasLocation=" + hasLocation + ", parts=" + parts + ", labels="
                + labels + ", creatorLabels=" + creatorLabels + ", itemId=" + itemId + "]";
    }


}
