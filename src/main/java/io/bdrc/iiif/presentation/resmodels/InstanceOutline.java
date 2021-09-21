package io.bdrc.iiif.presentation.resmodels;

import static io.bdrc.iiif.presentation.AppConstants.BDO;
import static io.bdrc.iiif.presentation.AppConstants.BDR;
import static io.bdrc.iiif.presentation.AppConstants.BDR_len;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.TMPPREFIX;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resmodels.PartInfo.PartType;

public class InstanceOutline {

    @JsonProperty("firstImageGroupQname")
    public String firstImageGroupQname = null;
    @JsonProperty("shortIdPartMap")
    public Map<String,PartInfo> partQnameToPartInfo = new HashMap<>();
    
    public InstanceOutline(final Model m, String intanceId) throws BDRCAPIException {
        if (intanceId.startsWith("bdr:"))
            intanceId = BDR+intanceId.substring(4);
        final Resource instance = m.getResource(intanceId);
        if (instance == null)
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid model: missing work");
        // we currently ignore linkTo, this should never happen with the current
        // code although it would probably be good to implement it at some point
        PartInfo rootPi = new PartInfo();
        final Resource pType = instance.getPropertyResourceValue(InstanceInfo.partTypeP);
        if (pType != null) {
            if (pType.getURI().endsWith("Section"))
                rootPi.partType = PartType.SECTION;
            else if (pType.getURI().endsWith("Volume"))
                rootPi.partType = PartType.VOLUME;
        } else {
            // hacky, this is likely the case for the root node, we want to consider it as we consider sections
            rootPi.partType = PartType.SECTION;
        }
        this.partQnameToPartInfo.put("bdr:"+instance.getLocalName(), rootPi);
        rootPi.labels = InstanceInfo.getLabels(m, instance);
        // this is recursive
        rootPi.parts = InstanceInfo.getParts(m, instance, this.partQnameToPartInfo);
        final Resource firstVolume = instance.getPropertyResourceValue(m.getProperty(TMPPREFIX, "firstImageGroup"));
        if (firstVolume != null) {
            this.firstImageGroupQname = "bdr:"+firstVolume.getLocalName();
        }
        rootPi.locations = ImageInstanceInfo.getLocations(m, instance);
        final Resource linkTo = instance.getPropertyResourceValue(m.getProperty(BDO, "virtualLinkTo"));
        if (linkTo != null) {
            rootPi.linkToQname = "bdr:"+linkTo.getLocalName();
            final Resource linkToType = linkTo.getPropertyResourceValue(RDF.type);
            if (linkToType != null) {
                rootPi.linkToTypeLname = linkToType.getLocalName();
            }
            if (rootPi.parts == null) {
                rootPi.parts = InstanceInfo.getParts(m, linkTo, null);
            }
            if (rootPi.labels == null) {
                rootPi.labels = InstanceInfo.getLabels(m, linkTo);
            }
            if (rootPi.locations == null) {
                rootPi.locations = ImageInstanceInfo.getLocations(m, linkTo);
            }
        }
    }
    
    public PartInfo getPartForInstanceId(String instanceId) {
        if (instanceId.startsWith(BDR)) {
            instanceId = "bdr:"+instanceId.substring(BDR_len);
        }
        return this.partQnameToPartInfo.get(instanceId);
    }
    
    // function finding the first node of the tree having two or more
    // children in the specified volume
    public static PartInfo getRootPiForVolumeR(PartInfo pi, int volNum) {
        if (pi.locations != null) {
            // if there are locations but none in the volume, no need to go further
            boolean hasLocInVolume = false;
            for (final Location loc : pi.locations) {
                if (loc.bvolnum <= volNum && loc.evolnum >= volNum) {
                    hasLocInVolume = true;
                    break;
                }
            }
            if (!hasLocInVolume) {
                return null;
            }
        }
        if (pi.parts == null)
            return null;
        PartInfo aChildInVolume = null;
        int nbChildrenInVolume = 0;
        for (final PartInfo child : pi.parts) {
            if (child.isInVolumeR(volNum)) {
                aChildInVolume = child;
                nbChildrenInVolume += 1;
            } else if (nbChildrenInVolume > 0) {
                // this is an optimization assuming that the parts
                // are in order
                break;
            }
        }
        switch (nbChildrenInVolume) {
        case 0:
            return null;
        case 1:
            return getRootPiForVolumeR(aChildInVolume, volNum);
        default:
            return pi;
        }
    }

}
