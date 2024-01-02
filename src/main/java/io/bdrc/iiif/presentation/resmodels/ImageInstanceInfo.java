package io.bdrc.iiif.presentation.resmodels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static io.bdrc.iiif.presentation.AppConstants.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.model.PropertyValue;
import io.bdrc.iiif.presentation.CollectionService;
import io.bdrc.iiif.presentation.ManifestService;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;


public class ImageInstanceInfo {
    
    static public class VolumeInfoSmall implements Comparable<VolumeInfoSmall> {
        @JsonProperty("volumeNumber")
        public Integer volumeNumber;
        @JsonProperty("imageGroupUri")
        public String imageGroupUri;
        @JsonProperty("iiifManifestUri")
        public String iiifManifestUri;
        @JsonIgnore
        public String imageGroupQname;
        
        public VolumeInfoSmall(String imageGroupUri, Integer volumeNumber, String iiifManifestUri) {
            this.imageGroupUri = imageGroupUri;
            this.imageGroupQname = CollectionService.getQname(imageGroupUri);
            this.volumeNumber = volumeNumber;
            this.iiifManifestUri = iiifManifestUri;
        }

        public String getPrefixedUri() {
            if (imageGroupQname == null && imageGroupUri != null) {
                imageGroupQname = CollectionService.getQname(imageGroupUri);
            }
            return imageGroupQname;
        }
        
        public PropertyValue getLabel() {
            final PropertyValue label = new PropertyValue();
            if (volumeNumber == null) {
                label.addValue(imageGroupQname);
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
    
    @JsonProperty("instanceUri")
    public String instanceUri;
    @JsonProperty("rootIinstanceUri")
    public String rootIinstanceUri = null;
    @JsonProperty("access")
    public AccessType access;
    @JsonProperty("restrictedInChina")
    public Boolean restrictedInChina;
    @JsonProperty("statusUri")
    public String statusUri;
    @JsonProperty("volumes")
    public List<VolumeInfoSmall> volumes;
    @JsonProperty("locations")
    public List<Location> locations = null;
    @JsonProperty("virtual")
    public boolean virtual = false;
    
    public ImageInstanceInfo() {}
    
    public static final Property adminAbout = ResourceFactory.createProperty(ADM+"adminAbout");
    
    public static Resource getAdminForResource(final Model m, final Resource r) {
        final StmtIterator si = m.listStatements(null, adminAbout, r);
        while (si.hasNext()) {
            Statement st = si.next();
            return st.getSubject();
        }
        return null;
    }
    
    public static List<Location> getLocations(final Model m, Resource r) {
        List<Location> res = null;
        final StmtIterator clItr = r.listProperties(m.getProperty(BDO, "contentLocation"));
        while (clItr.hasNext()) {
            final Statement s = clItr.next();
            final Resource cl = s.getObject().asResource();
            if (res == null)
                res = new ArrayList<>();
            res.add(new Location(m, cl));
        }
        if (res != null && res.size() > 1) {
            Collections.sort(res);
        }
        return res;
    }
    
    public ImageInstanceInfo(final Model m, String iinstanceId) throws BDRCAPIException {
        // the model is supposed to come from the IIIFPres_itemGraph graph query
        if (iinstanceId.startsWith("bdr:"))
            iinstanceId = BDR+iinstanceId.substring(4);
        final Resource iinstance = m.getResource(iinstanceId);
        this.instanceUri = iinstance.getPropertyResourceValue(m.getProperty(BDO, "instanceReproductionOf")).getURI();
        Resource rootImageInstance = iinstance.getPropertyResourceValue(m.getProperty(BDO, "inRootInstance"));
        if (rootImageInstance != null) {
            this.rootIinstanceUri = rootImageInstance.getURI();
        } else {
            rootImageInstance = iinstance;
        }
        this.virtual = iinstance.hasLiteral(m.getProperty(BDO, "virtualImageInstance"), true);
        final Resource iinstanceAdmin =  getAdminForResource(m, rootImageInstance);
        if (iinstanceAdmin == null) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "invalid model: no admin data for item");
        }
        final Resource itemStatus = iinstanceAdmin.getPropertyResourceValue(m.getProperty(ADM, "status"));
        if (itemStatus == null) {
            this.statusUri = null;
        } else {
            this.statusUri = itemStatus.getURI();
        }
        final Resource iinstanceAccess = iinstanceAdmin.getPropertyResourceValue(m.getProperty(ADM, "access"));
        final Statement restrictedInChinaS = iinstanceAdmin.getProperty(m.getProperty(ADM, "restrictedInChina"));
        if (restrictedInChinaS == null) {
            this.restrictedInChina = true;
        } else {
            this.restrictedInChina = restrictedInChinaS.getBoolean();
        }
        if (iinstanceAccess == null)
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "invalid model: no access");

        final StmtIterator volumesItr = rootImageInstance.listProperties(m.getProperty(BDO, "instanceHasVolume"));
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
        this.locations = getLocations(m, iinstance);
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
    
    public VolumeInfoSmall getImageGroup(String igQname) {
        for (VolumeInfoSmall vi : volumes) {
            if (vi.imageGroupQname.equals(igQname))
                return vi;
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
