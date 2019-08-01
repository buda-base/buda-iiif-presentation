package io.bdrc.iiif.presentation.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static io.bdrc.iiif.presentation.AppConstants.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.digitalcollections.iiif.model.PropertyValue;
import io.bdrc.iiif.presentation.CollectionService;
import io.bdrc.iiif.presentation.ManifestService;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;


public class ItemInfo {
    
    static public class VolumeInfoSmall implements Comparable<VolumeInfoSmall> {
        @JsonProperty("volumeNumber")
        public Integer volumeNumber;
        @JsonProperty("volumeId")
        public String volumeId;
        @JsonProperty("iiifManifest")
        public String iiifManifest;
        @JsonIgnore
        public String prefixedId;
        
        public VolumeInfoSmall(String volumeId, Integer volumeNumber, String iiifManifest) {
            this.volumeId = volumeId;
            this.prefixedId = CollectionService.getPrefixedForm(volumeId);
            this.volumeNumber = volumeNumber;
            this.iiifManifest = iiifManifest;
        }

        public String getPrefixedUri() {
            if (prefixedId == null && volumeId != null) {
                prefixedId = CollectionService.getPrefixedForm(volumeId);
            }
            return prefixedId;
        }
        
        public PropertyValue getLabel() {
            final PropertyValue label = new PropertyValue();
            if (volumeNumber == null) {
                label.addValue(prefixedId);
            } else {
                label.addValue(ManifestService.getLocaleFor("en"), "volume "+volumeNumber);
                label.addValue(ManifestService.getLocaleFor("bo-x-ewts"), "pod"+volumeNumber+"/");
            }
            return label;
        }

        @Override
        public int compareTo(VolumeInfoSmall compared) {
            if (this.volumeNumber == null || compared.volumeNumber == null)
                return 0;
            return this.volumeNumber - compared.volumeNumber;
        }
    }
    
    @JsonProperty("workId")
    public String workId;
    @JsonProperty("access")
    public AccessType access;
    @JsonProperty("restrictedInChina")
    public Boolean restrictedInChina;
    @JsonProperty("statusUri")
    public String statusUri;
    @JsonProperty("license")
    public LicenseType license;
    @JsonProperty("volumes")
    public List<VolumeInfoSmall> volumes;
    
    public ItemInfo() {}
    
    public static final Property adminAbout = ResourceFactory.createProperty(ADM+"adminAbout");
    
    public static Resource getAdminForResource(final Model m, final Resource r) {
        final StmtIterator si = m.listStatements(null, adminAbout, r);
        while (si.hasNext()) {
            Statement st = si.next();
            return st.getSubject();
        }
        return null;
    }
    
    public ItemInfo(final Model m, String itemId) throws BDRCAPIException {
        // the model is supposed to come from the IIIFPres_itemGraph graph query
        if (itemId.startsWith("bdr:"))
            itemId = BDR+itemId.substring(4);
        final Resource item = m.getResource(itemId);
        this.workId = item.getPropertyResourceValue(m.getProperty(BDO, "itemForWork")).getURI();
        final Resource itemAdmin =  getAdminForResource(m, item);
        if (itemAdmin == null) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "invalid model: no admin data for item");
        }
        final Resource itemStatus = itemAdmin.getPropertyResourceValue(m.getProperty(ADM, "status"));
        if (itemStatus == null) {
            this.statusUri = null;
        } else {
            this.statusUri = itemStatus.getURI();
        }
        final Resource itemAccess = itemAdmin.getPropertyResourceValue(m.getProperty(ADM, "access"));
        final Statement restrictedInChinaS = itemAdmin.getProperty(m.getProperty(ADM, "restrictedInChina"));
        if (restrictedInChinaS == null) {
            this.restrictedInChina = true;
        } else {
            this.restrictedInChina = restrictedInChinaS.getBoolean();
        }
        final Resource legalData = itemAdmin.getPropertyResourceValue(m.getProperty(ADM, "contentLegal"));
        if (legalData == null) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "invalid model: no legal data for item admin data");
        }
        final Resource workLicense = legalData.getPropertyResourceValue(m.getProperty(ADM, "license"));
        if (itemAccess == null || workLicense == null)
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "invalid model: no access or license");
        this.access = AccessType.fromString(itemAccess.getURI());
        this.license = LicenseType.fromString(workLicense.getURI());
        final StmtIterator volumesItr = item.listProperties(m.getProperty(BDO, "itemHasVolume"));
        if (!volumesItr.hasNext())
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "no volume in item");
        final List<VolumeInfoSmall> volumes = new ArrayList<>();
        final Property volumeNumberP = m.getProperty(BDO, "volumeNumber");
        final Property hasIIIFManifestP = m.getProperty(BDO, "hasIIIFManifest");
        while (volumesItr.hasNext()) {
            final Statement s = volumesItr.next();
            final Resource volume = s.getObject().asResource();
            final String volumeId = volume.getURI();
            final Statement volumeNumberS = volume.getProperty(volumeNumberP);
            final Statement volumeIiifManifest = volume.getProperty(hasIIIFManifestP);
            final String iiifmanifest = volumeIiifManifest == null ? null : volumeIiifManifest.getResource().getURI();
            if (volumeNumberS == null) {
                volumes.add(new VolumeInfoSmall(volumeId, null, iiifmanifest));
            } else {
                final Integer volNum = volumeNumberS.getInt();
                volumes.add(new VolumeInfoSmall(volumeId, volNum, iiifmanifest));
            }
        }
        Collections.sort(volumes);
        this.volumes = volumes;
    }
    
    public VolumeInfoSmall getVolumeNumber(int volumeNumber) {
        for (VolumeInfoSmall vi : volumes) {
            final Integer viVolNum = vi.volumeNumber;
            if (viVolNum == volumeNumber)
                return vi;
            if (viVolNum != null && viVolNum > volumeNumber)
                return null;
        }
        return null;
    }
}
