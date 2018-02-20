package io.bdrc.iiif.presentation;

import static io.bdrc.iiif.presentation.AppConstants.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import de.digitalcollections.iiif.model.ImageContent;
import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.enums.ViewingDirection;
import de.digitalcollections.iiif.model.image.ImageApiProfile;
import de.digitalcollections.iiif.model.image.ImageService;
import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Collection;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import de.digitalcollections.iiif.model.sharedcanvas.Sequence;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;

public class CollectionService {
    public static String getLabelFormImage(final int imageIndex) {
        if (imageIndex < 2)
            return "tbrc-"+(imageIndex+1);
        return "p. "+(imageIndex-1);
    }
    
    public static String getImageServiceUrl(final String filename, final Identifier id) {
        return "https://images.bdrc.io/iiif/2/"+id.itemId+"::"+filename;
    }
    
    public static Collection getCollectionForIdentifier(final Identifier id) throws BDRCAPIException {
        final Collection collection = new Collection("http://presentation.bdrc.io/2.1.1/collection/"+id.id+"", "Dergue Kangyur");
        final PropertyValue attr = new PropertyValue();
        attr.addValue(Locale.ENGLISH, "Buddhist Digital Resource Center");
        collection.setAttribution(attr);
        collection.addLicense("https://creativecommons.org/publicdomain/mark/1.0/");
        collection.addLogo("https://eroux.fr/logo.png");
        for (int i = 886 ; i < 989 ; i++) {
            Manifest manifest = new Manifest("https://eroux.fr/manifest-0"+i+".json", "Volume "+(i-885));
            collection.addManifest(manifest);
        }
        return collection;
    }
}
