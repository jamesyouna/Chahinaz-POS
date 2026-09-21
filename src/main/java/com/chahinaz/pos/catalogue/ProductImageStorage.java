package com.chahinaz.pos.catalogue;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductImageStorage {
  private static final long MAX_BYTES=5L*1024*1024;
  private final Path root;
  public ProductImageStorage(@Value("${pos.images.directory:./data/product-images}") String directory) {
    root=Path.of(directory).toAbsolutePath().normalize();
  }
  public record StoredImage(byte[] bytes,String mime) {}
  public String store(MultipartFile file) {
    if (file.isEmpty() || file.getSize()>MAX_BYTES) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Image must be 1 byte to 5 MB");
    String mime=file.getContentType();
    String format;
    if ("image/jpeg".equals(mime)) format="jpeg";
    else if ("image/png".equals(mime)) format="png";
    else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only JPEG and PNG images are accepted");
    try (ImageInputStream input=ImageIO.createImageInputStream(file.getInputStream())) {
      Iterator<ImageReader> readers=ImageIO.getImageReaders(input);
      if (!readers.hasNext()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid image data");
      ImageReader reader=readers.next();
      try {
        reader.setInput(input);
        String detected=reader.getFormatName();
        if (!(format.equalsIgnoreCase(detected) || (format.equals("jpeg") && "jpg".equalsIgnoreCase(detected))))
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Image content does not match MIME type");
        int width=reader.getWidth(0),height=reader.getHeight(0);
        if (width<1 || height<1 || (long)width*height>25_000_000)
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Image dimensions are too large");
        BufferedImage decoded=reader.read(0);
        double scale=Math.min(1.0,1600.0/Math.max(width,height));
        int outWidth=Math.max(1,(int)Math.round(width*scale)),outHeight=Math.max(1,(int)Math.round(height*scale));
        BufferedImage output=new BufferedImage(outWidth,outHeight,format.equals("jpeg")?BufferedImage.TYPE_INT_RGB:BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=output.createGraphics();
        try { graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
          graphics.drawImage(decoded,0,0,outWidth,outHeight,null); }
        finally { graphics.dispose(); }
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        if (!ImageIO.write(output,format,bytes)) throw new IOException("No image encoder");
        String key=UUID.randomUUID()+(format.equals("jpeg")?".jpg":".png");
        Files.createDirectories(root);
        Files.write(root.resolve(key),bytes.toByteArray());
        return key;
      } finally { reader.dispose(); }
    } catch (ResponseStatusException e) { throw e; }
      catch (IOException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unable to process image",e); }
  }
  public StoredImage load(String key) {
    if (!key.matches("[0-9a-fA-F-]{36}\\.(jpg|png)")) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Image not found");
    Path path=root.resolve(key).normalize();
    if (!path.startsWith(root) || !Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Image not found");
    try { return new StoredImage(Files.readAllBytes(path),key.endsWith(".jpg")?"image/jpeg":"image/png"); }
    catch (IOException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Image not found",e); }
  }
}
