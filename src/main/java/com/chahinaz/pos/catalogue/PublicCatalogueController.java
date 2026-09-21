package com.chahinaz.pos.catalogue;

import static com.chahinaz.pos.catalogue.CatalogueDtos.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/public")
public class PublicCatalogueController {
  private final CatalogueService catalogue;
  private final ProductImageStorage images;
  public PublicCatalogueController(CatalogueService catalogue, ProductImageStorage images) { this.catalogue=catalogue; this.images=images; }
  @GetMapping("/categories") public List<CategoryView> categories() { return catalogue.publicCategories(); }
  @GetMapping("/products") public List<PublicProductView> products() { return catalogue.publicProducts(); }
  @GetMapping("/products/{id}") public PublicProductView product(@PathVariable UUID id) { return catalogue.publicProduct(id); }
  @GetMapping("/images/{key}") public ResponseEntity<byte[]> image(@PathVariable String key) {
    if (!catalogue.imageIsPublic(key)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Image not found");
    ProductImageStorage.StoredImage image=images.load(key);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.mime())).body(image.bytes());
  }
}
