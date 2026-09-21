package com.chahinaz.pos.catalogue;

import static com.chahinaz.pos.catalogue.CatalogueDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/management")
public class ManagementCatalogueController {
  private final CatalogueService catalogue;
  private final ProductImageStorage images;
  public ManagementCatalogueController(CatalogueService catalogue, ProductImageStorage images) {
    this.catalogue=catalogue; this.images=images;
  }
  @GetMapping("/categories") public List<CategoryView> categories() { return catalogue.categories(); }
  @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED)
  public CategoryView createCategory(@Valid @RequestBody CategoryInput input, Authentication auth) { return catalogue.createCategory(input,auth); }
  @PutMapping("/categories/{id}") public CategoryView updateCategory(@PathVariable UUID id,@Valid @RequestBody CategoryInput input,Authentication auth) {
    return catalogue.updateCategory(id,input,auth);
  }
  @GetMapping("/products") public List<ProductView> products() { return catalogue.products(); }
  @GetMapping("/products/{id}") public ProductView product(@PathVariable UUID id) { return catalogue.productView(id); }
  @PostMapping("/products") @ResponseStatus(HttpStatus.CREATED)
  public ProductView createProduct(@Valid @RequestBody CreateProductInput input,Authentication auth) { return catalogue.createProduct(input,auth); }
  @PutMapping("/products/{id}") public ProductView updateProduct(@PathVariable UUID id,@Valid @RequestBody ProductInput input,Authentication auth) {
    return catalogue.updateProduct(id,input,auth);
  }
  @PatchMapping("/products/{id}/publication") public ProductView publication(@PathVariable UUID id,@RequestBody PublicationStatus status,Authentication auth) {
    return catalogue.publication(id,status,auth);
  }
  @PatchMapping("/products/{id}/active") public ProductView activation(@PathVariable UUID id,@RequestBody boolean active,Authentication auth) {
    return catalogue.activation(id,active,auth);
  }
  @PostMapping("/products/{id}/image") public ProductView uploadImage(@PathVariable UUID id,@RequestParam("file") MultipartFile file,Authentication auth) {
    catalogue.productView(id);
    String key=images.store(file);
    return catalogue.setImage(id,key,auth);
  }
  @PostMapping("/products/{id}/restock") public MovementView restock(@PathVariable UUID id,@Valid @RequestBody StockInput input,Authentication auth) {
    return catalogue.restock(id,input,auth);
  }
  @PostMapping("/products/{id}/adjust") public MovementView adjust(@PathVariable UUID id,@Valid @RequestBody AdjustmentInput input,Authentication auth) {
    return catalogue.adjust(id,input,auth);
  }
  @GetMapping("/products/{id}/movements") public List<MovementView> movements(@PathVariable UUID id) { return catalogue.movements(id); }
  @GetMapping("/products/low-stock") public List<ProductView> lowStock() { return catalogue.lowStock(); }
  @GetMapping("/products/out-of-stock") public List<ProductView> outOfStock() { return catalogue.outOfStock(); }
}
