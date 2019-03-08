package io.bdrc.iiif.presentation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.junit.Test;

public class IIIFImageTest {

    @Test
    public void iiiftext() throws IOException {
        InputStream input = IIIFImageTest.class.getClassLoader().getResourceAsStream("08860041.tif");
        ImageInputStream iis;
        try {
          iis = ImageIO.createImageInputStream(input);
        } catch (IOException e) {
          e.printStackTrace();
          return;
        }
        ImageReader reader = ImageIO.getImageReaders(iis).next();
        System.out.println("using reader "+(reader.getClass()));
        reader.setInput(iis);
        ImageReadParam readParam = reader.getDefaultReadParam();
        BufferedImage outImg = reader.read(0, readParam);
        ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/jpeg").next();
        System.out.println("using writer "+(writer.getClass()));
        OutputStream os = new FileOutputStream(new File("/tmp/imgresult.jpg"));
        ImageOutputStream ios = ImageIO.createImageOutputStream(os);
        writer.setOutput(ios);
        System.out.println("debug step 1");
        writer.write(outImg);
        writer.dispose();
        ios.flush();
    }
    
}
