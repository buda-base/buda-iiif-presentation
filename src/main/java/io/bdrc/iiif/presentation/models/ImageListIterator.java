package io.bdrc.iiif.presentation.models;

import java.util.Iterator;

public class ImageListIterator implements Iterator <String> {
    
    private String imageListElts[] = null;
    private int eltIdx = 0;
    private int maxEltIdx = 0;
    private int imgIdx = 0;
    private int maxImgIdx = -1;
    private boolean oneFileOnly = false;
    private String prefixStr = null;
    private String suffixStr = null;
    private int imageSeqNum = 0;
    private int maxImageSeqNum = -1;

    @Override
    public boolean hasNext() {
        return (imgIdx <= maxImgIdx || eltIdx < maxEltIdx) && (maxImageSeqNum == -1 || imageSeqNum < maxImageSeqNum);
    }

    private void nextElement() {
        this.eltIdx += 1;
        final String elt = imageListElts[eltIdx];
        final int colonIdx = elt.indexOf(':');
        if (colonIdx == -1) {
            imgIdx = 1;
            maxImgIdx = 0;
            prefixStr = null;
            suffixStr = null;
            oneFileOnly = true;
            return;
        }
        oneFileOnly = false;
        final int dotIdx = elt.lastIndexOf('.');
        final String idxStr = elt.substring(dotIdx-4, dotIdx);
        imgIdx = Integer.valueOf(idxStr);
        final String nbImagesStr = elt.substring(colonIdx+1);
        maxImgIdx = imgIdx+Integer.valueOf(nbImagesStr)-1;
        prefixStr = elt.substring(0,dotIdx-4);
        suffixStr = elt.substring(dotIdx, colonIdx);
    }
    
    @Override
    public String next() {
        if (imgIdx > maxImgIdx)
            nextElement();
        imageSeqNum += 1;
        if (oneFileOnly)
            return imageListElts[eltIdx];
        final String imgNumStr = String.format("%04d", imgIdx);
        imgIdx += 1;
        return prefixStr+imgNumStr+suffixStr;
    }
    
    public ImageListIterator(String imageListString) {
        imageListElts = imageListString.split("\\|");
        eltIdx = -1;
        maxEltIdx = imageListElts.length-1;
    }

    public ImageListIterator(String imageListString, int beginImageSeqNum, int endImageSeqNum) {
        this(imageListString);
        assert(endImageSeqNum == -1 || endImageSeqNum >= beginImageSeqNum);
        maxImageSeqNum = endImageSeqNum;
        if (beginImageSeqNum > 1) {
            // advance until beginImageSeqNum
            while (imageSeqNum < beginImageSeqNum-1) {
                if (eltIdx > maxEltIdx)
                    break;
                if (imgIdx > maxImgIdx) {
                    if (eltIdx < maxEltIdx)
                        nextElement();
                    else {
                        eltIdx = maxEltIdx+1;
                        break;
                    }
                }
                if (oneFileOnly) {
                    imageSeqNum += 1;
                    continue;
                }
                final int finalImageSeqNum = imageSeqNum + (maxImgIdx-imgIdx)+1;
                if (finalImageSeqNum >= beginImageSeqNum) {
                    imgIdx = imgIdx+beginImageSeqNum-imageSeqNum-1;
                    imageSeqNum = beginImageSeqNum-1;
                    break;
                } else {
                    imgIdx = maxImgIdx+1;
                    imageSeqNum = finalImageSeqNum;
                }
            }
        }
    }

}
