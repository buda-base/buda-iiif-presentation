package io.bdrc.iiif.presentation.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static io.bdrc.iiif.presentation.AppConstants.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.bdrc.iiif.presentation.CollectionService;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;


public class ItemInfo {
    
    static public class VolumeInfoSmall implements Comparable<VolumeInfoSmall> {
        @JsonProperty("volumeNumber")
        public Integer volumeNumber;
        @JsonProperty("volumeId")
        public String volumeId;
        @JsonProperty("iiifManifest")
        public String iiifManifest;
        
        public VolumeInfoSmall(String volumeId, Integer volumeNumber, String iiifManifest) {
            this.volumeId = volumeId;
            this.volumeNumber = volumeNumber;
            this.iiifManifest = iiifManifest;
        }
        
        public String toDisplay() {
            if (volumeNumber == null)
                return CollectionService.getPrefixedForm(volumeId);
            else
                return "Volume "+volumeNumber;
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
    @JsonProperty("license")
    public LicenseType license;
    @JsonProperty("volumes")
    public List<VolumeInfoSmall> volumes;
    
    public ItemInfo() {}
    
    public ItemInfo(final Model m, String itemId) throws BDRCAPIException {
        // the model is supposed to come from the IIIFPres_itemInfo graph query
        if (itemId.startsWith("bdr:"))
            itemId = BDR+itemId.substring(4);
        final Resource item = m.getResource(itemId);
        if (item == null)
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "invalid model: missing item");
        final Resource work = item.getPropertyResourceValue(m.getProperty(BDO, "itemForWork"));
        if (work == null)
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "invalid model: missing work");
        this.workId = work.getURI();
        final Resource workAccess = work.getPropertyResourceValue(m.getProperty(ADM, "access"));
        final Resource workLicense = work.getPropertyResourceValue(m.getProperty(ADM, "license"));
        if (workAccess == null || workLicense == null)
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "invalid model: no access or license");
        this.access = AccessType.fromString(workAccess.getURI());
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
