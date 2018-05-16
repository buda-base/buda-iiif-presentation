package io.bdrc.iiif.presentation.models;

import static io.bdrc.iiif.presentation.AppConstants.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.SKOS;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.ItemInfo.VolumeInfoSmall;

public class WorkInfo {
    
    static public class PartInfo implements Comparable<PartInfo> {
        @JsonProperty("partIndex")
        public Integer partIndex;
        @JsonProperty("partId")
        public String partId;
        @JsonProperty("labels")
        public List<LangString> labels;
        
        public PartInfo(String partId, Integer partIndex) {
            this.partId = partId;
            this.partIndex = partIndex;
        }

        @Override
        public int compareTo(PartInfo compared) {
            if (this.partIndex == null || compared.partIndex == null)
                return 0;
            return this.partIndex - compared.partIndex;
        }
    }

    static public class LangString {
        @JsonProperty("@value")
        public String value;
        @JsonProperty("@language")
        public String language;

        public LangString(Literal l) {
            this.value = l.getString();
            this.language = l.getLanguage();
        }
    }
    
    @JsonProperty("rootWorkId")
    public String rootWorkId = null;
    @JsonProperty("hasLocation")
    public boolean hasLocation = false;
    @JsonProperty("parts")
    public List<PartInfo> parts = null;
    @JsonProperty("labels")
    public List<LangString> labels = null; // ?
    @JsonProperty("creatorLabels")
    public List<LangString> creatorLabels = null; // ?
    @JsonProperty("bvolnum")
    public Integer bvolnum = null;
    @JsonProperty("evolnum")
    public Integer evolnum = null;
    @JsonProperty("bpagenum")
    public Integer bpagenum = null;
    @JsonProperty("epagenum") // by convention, epagenum is -1 for the last page
    public Integer epagenum = null;
    @JsonProperty("itemId")
    public String itemId = null;
    
    public WorkInfo() {}
    
    public void readLocation(final Model m, final Resource location) {
        final Property locationVolumeP = m.getProperty(BDO, "workLocationVolume");
        if (!location.hasProperty(locationVolumeP)) 
            return;
        this.bvolnum = location.getProperty(locationVolumeP).getInt();
        final Property locationEndVolumeP = m.getProperty(BDO, "workLocationEndVolume");
        if (location.hasProperty(locationEndVolumeP))
            this.evolnum = location.getProperty(locationEndVolumeP).getInt();
        else
            this.evolnum = this.bvolnum;
        final Property locationPageP = m.getProperty(BDO, "workLocationPage");
        if (location.hasProperty(locationPageP))
            this.bpagenum = location.getProperty(locationPageP).getInt();
        else
            this.bpagenum = 0;
        final Property locationEndPageP = m.getProperty(BDO, "workLocationEndPage");
        if (location.hasProperty(locationEndPageP))
            this.epagenum = location.getProperty(locationEndPageP).getInt();
        else
            this.epagenum = -1;
        final Property locationWorkP = m.getProperty(BDO, "workLocationWork");
        if (location.hasProperty(locationWorkP))
            this.rootWorkId = location.getProperty(locationWorkP).getResource().getURI();
        this.hasLocation = true;
    }
    
    public WorkInfo(final Model m, String workId) throws BDRCAPIException {
        // the model is supposed to come from the IIIFPres_itemInfo graph query
        if (workId.startsWith("bdr:"))
            workId = BDR+workId.substring(4);
        final Resource work = m.getResource(workId);
        if (work == null)
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "invalid model: missing work");
        final Resource item = work.getPropertyResourceValue(m.getProperty(TMPPREFIX, "inItem"));
        if (item == null) {
            this.hasLocation = false;
        } else {
            this.itemId = item.getURI();
            final Resource location = work.getPropertyResourceValue(m.getProperty(BDO, "workLocation"));
            readLocation(m, location);
        }

        // TODO: add access and license of the root work in request

        final StmtIterator partsItr = work.listProperties(m.getProperty(BDO, "workHasPart"));
        if (partsItr.hasNext()) {
            final Property partIndexP = m.getProperty(BDO, "workPartIndex");
            final List<PartInfo> parts = new ArrayList<>();
            while (partsItr.hasNext()) {
                final Statement s = partsItr.next();
                final Resource part = s.getObject().asResource();
                final String partId = part.getURI();
                final Statement partIndexS = part.getProperty(partIndexP);
                final PartInfo partInfo;
                if (partIndexS == null)
                    partInfo = new PartInfo(partId, null);
                else
                    partInfo = new PartInfo(partId, partIndexS.getInt());
                // part labels
                final StmtIterator labelItr = part.listProperties(SKOS.prefLabel);
                if (labelItr.hasNext()) {
                    final List<LangString> labels = new ArrayList<>();
                    while (labelItr.hasNext()) {
                        final Statement pls = labelItr.next();
                        final Literal l = pls.getObject().asLiteral();
                        labels.add(new LangString(l));
                    }
                    partInfo.labels = labels;
                }
                parts.add(partInfo);
            }
            Collections.sort(parts);
            this.parts = parts;
        }

        // labels
        final StmtIterator labelItr = work.listProperties(SKOS.prefLabel);
        if (labelItr.hasNext()) {
            final List<LangString> labels = new ArrayList<>();
            while (labelItr.hasNext()) {
                final Statement s = labelItr.next();
                final Literal l = s.getObject().asLiteral();
                labels.add(new LangString(l));
            }
            this.labels = labels;
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
}
