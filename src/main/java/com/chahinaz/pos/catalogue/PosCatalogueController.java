package com.chahinaz.pos.catalogue;

import static com.chahinaz.pos.catalogue.CatalogueDtos.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/pos")
public class PosCatalogueController {
  private final CatalogueService catalogue;
  public PosCatalogueController(CatalogueService catalogue) { this.catalogue=catalogue; }
  @GetMapping("/categories") public List<CategoryView> categories() { return catalogue.publicCategories(); }
  @GetMapping("/products") public List<PublicProductView> products() { return catalogue.posProducts(); }
}
