package com.chahinaz.pos.catalogue;

import static com.chahinaz.pos.catalogue.CatalogueDtos.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;

@RestController @RequestMapping("/api/public")
public class PublicCatalogueController {
  private final CatalogueService catalogue;
  private final ProductImageStorage images;
  public PublicCatalogueController(CatalogueService catalogue, ProductImageStorage images) { this.catalogue=catalogue; this.images=images; }
  @GetMapping({"/categories","/catalogue/categories"}) public ResponseEntity<List<PublicCategoryView>> categories() {
    return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(2)).cachePublic().mustRevalidate()).body(catalogue.publicCategories());
  }
  @GetMapping({"/products","/catalogue/products"}) public ResponseEntity<List<PublicProductView>> products(
      @RequestParam(required=false) String search, @RequestParam(required=false) UUID category,
      @RequestParam(required=false) Boolean featured, @RequestParam(required=false) Boolean newArrival,
      @RequestParam(required=false) Availability availability, @RequestParam(defaultValue="0") int page,
      @RequestParam(defaultValue="100") int size, @RequestParam(defaultValue="name") String sort) {
    return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic().mustRevalidate())
        .body(catalogue.publicProducts(search,category,featured,newArrival,availability,page,size,sort));
  }
  @GetMapping({"/products/{id}","/catalogue/products/{id}"}) public ResponseEntity<PublicProductView> product(@PathVariable UUID id) {
    return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic().mustRevalidate()).body(catalogue.publicProduct(id));
  }
  @GetMapping("/images/{key}") public ResponseEntity<byte[]> image(@PathVariable String key) {
    if (!catalogue.imageIsPublic(key)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Image not found");
    ProductImageStorage.StoredImage image=images.load(key);
    return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
        .contentType(MediaType.parseMediaType(image.mime())).body(image.bytes());
  }
}
