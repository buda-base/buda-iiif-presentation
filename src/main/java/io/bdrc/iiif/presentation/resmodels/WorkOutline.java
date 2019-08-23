package io.bdrc.iiif.presentation.resmodels;

import static io.bdrc.iiif.presentation.AppConstants.BDR;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;
import static io.bdrc.iiif.presentation.AppConstants.TMPPREFIX;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;

public class WorkOutline {

    @JsonProperty("firstVolumeId")
    public String firstVolumeId = null;
    @JsonProperty("rootPi")
    public PartInfo rootPi = null;
    
    public WorkOutline(final Model m, String workId) throws BDRCAPIException {
        if (workId.startsWith("bdr:"))
            workId = BDR+workId.substring(4);
        final Resource work = m.getResource(workId);
        if (work == null)
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "invalid model: missing work");
        // we currently ignore linkTo, this should never happen with the current
        // code although it would probably be good to implement it at some point
        this.rootPi = new PartInfo();
        this.rootPi.labels = WorkInfo.getLabels(m, work);
        // this is recursive
        this.rootPi.subparts = WorkInfo.getParts(m, work);
        final Resource firstVolume = work.getPropertyResourceValue(m.getProperty(TMPPREFIX, "firstVolume"));
        if (firstVolume != null) {
            this.firstVolumeId = "bdr:"+firstVolume.getLocalName();
        }
    }
    
    public PartInfo getPartForWorkId(String workId) {
        // TODO: implement
        return rootPi;
    }
    
    // function finding the first node of the tree having two or more
    // children in the specified volume
    public static PartInfo getRootPiForVolumeR(PartInfo pi, int volNum) {
        final Location loc = pi.location;
        if (loc != null && (loc.bvolnum > volNum || loc.evolnum < volNum)) {
            // if the part is not in the volume, no need to go any further
            return null;
        }
        PartInfo aChildInVolume = null;
        int nbChildrenInVolume = 0;
        for (final PartInfo child : pi.subparts) {
            if (isInVolumeR(volNum, child)) {
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
    
    public static boolean isInVolumeR(int volNum, final PartInfo pi) {
        final Location loc = pi.location;
        if (loc != null) {
            return loc.bvolnum <= volNum && loc.evolnum >= volNum;
        }
        for (final PartInfo child : pi.subparts) {
            if (isInVolumeR(volNum, child)) {
                return true;
            }
        }
        return false;
    }

}
