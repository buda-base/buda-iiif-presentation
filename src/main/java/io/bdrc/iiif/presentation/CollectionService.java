package io.bdrc.iiif.presentation;

import java.util.Locale;

import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.sharedcanvas.Collection;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.models.Identifier;

public class CollectionService {
    public static String getLabelFormImage(final int imageIndex) {
        if (imageIndex < 2)
            return "tbrc-"+(imageIndex+1);
        return "p. "+(imageIndex-1);
    }
    
    public static String getImageServiceUrl(final String filename, final Identifier id) {
        return "https://images.bdrc.io/iiif/2/"+id.getItemId()+"::"+filename;
    }
    
    public static Collection getCollectionForIdentifier(final Identifier id) throws BDRCAPIException {
        final Collection collection = new Collection("http://presentation.bdrc.io/2.1.1/collection/"+id.getId()+"", "Dergue Kangyur");
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
