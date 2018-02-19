package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;

public class ImageInfoListService {

    final static String prefix = "/home/eroux/BUDA/softs/METStodim/output/60_W22084_W22084-";
    final static ObjectMapper mapper = new ObjectMapper();
    
    static List<ImageInfo> getImageInfoList(Identifier id) throws BDRCAPIException {
        final String jsonfilepath = prefix+id.volNum+"_dimensions.json";
        final File json = new File(jsonfilepath);  
        final List<ImageInfo> imageList;
        try {
            //imageList = Arrays.asList(mapper.readValue(json, ImageInfo[].class));
            imageList = mapper.readValue(json, new TypeReference<List<ImageInfo>>(){});
        } catch (IOException e) {
            throw new BDRCAPIException(500, FILE_LIST_ACCESS_ERROR_CODE, e);
        }
        return imageList;
    }
}
