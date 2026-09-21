package com.chahinaz.pos.catalogue;

import com.chahinaz.pos.audit.AuditService;
import com.chahinaz.pos.employee.EmployeeRepository;
import static com.chahinaz.pos.catalogue.CatalogueDtos.*;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Comparator;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogueService {
  private final CategoryRepository categories;
  private final ProductRepository products;
  private final InventoryMovementRepository movements;
  private final EmployeeRepository employees;
  private final AuditService audit;
  public CatalogueService(CategoryRepository categories, ProductRepository products, InventoryMovementRepository movements,
      EmployeeRepository employees, AuditService audit) {
    this.categories=categories; this.products=products; this.movements=movements; this.employees=employees; this.audit=audit;
  }
  private UUID actor(Authentication auth) { return employees.findByUsernameIgnoreCase(auth.getName()).orElseThrow().id; }
  private Category category(UUID id) { return categories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Category not found")); }
  private Product product(UUID id) { return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found")); }
  private Product lockedProduct(UUID id) { return products.findLockedById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found")); }
  private static String name(String value) { return value.trim(); }
  private static String optional(String value) { return value == null || value.isBlank() ? null : value.trim(); }
  private static String sku(String value) { return value.trim().toUpperCase(Locale.ROOT); }

  @Transactional(readOnly=true) public List<CategoryView> categories() { return categories.findAll().stream().map(CategoryView::of).toList(); }
  @Transactional(readOnly=true) public List<PublicCategoryView> publicCategories() { return categories.findByActiveTrueOrderByNameAsc().stream().map(PublicCategoryView::of).toList(); }
  @Transactional public CategoryView createCategory(CategoryInput input, Authentication auth) {
    String n=name(input.name());
    if (categories.existsByNameIgnoreCase(n)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Category name already exists");
    Category c=new Category(n,optional(input.description())); c.active=input.active(); categories.saveAndFlush(c);
    audit.record(actor(auth),"CATEGORY_CREATED","category",c.id.toString(),null,c.name);
    return CategoryView.of(c);
  }
  @Transactional public CategoryView updateCategory(UUID id, CategoryInput input, Authentication auth) {
    Category c=category(id); String n=name(input.name());
    if (!c.name.equalsIgnoreCase(n) && categories.existsByNameIgnoreCase(n)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Category name already exists");
    String before=c.name+"; active="+c.active;
    c.name=n; c.description=optional(input.description()); c.active=input.active(); c.updatedAt=Instant.now(); categories.saveAndFlush(c);
    audit.record(actor(auth),"CATEGORY_UPDATED","category",id.toString(),before,c.name+"; active="+c.active);
    return CategoryView.of(c);
  }
  @Transactional(readOnly=true) public List<ProductView> products() { return products.findAll().stream().map(ProductView::of).toList(); }
  @Transactional(readOnly=true) public ProductView productView(UUID id) { return ProductView.of(product(id)); }
  @Transactional(readOnly=true) public List<PublicProductView> publicProducts() {
    return publicProducts(null,null,null,null,null,0,100,"name");
  }
  @Transactional(readOnly=true) public List<PublicProductView> publicProducts(String search, UUID category, Boolean featured,
      Boolean newArrival, Availability availability, int page, int size, String sort) {
    if (page < 0 || size < 1 || size > 200) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid pagination");
    String term=search==null?null:search.trim().toLowerCase(Locale.ROOT);
    Comparator<Product> order=switch(sort) {
      case "price" -> Comparator.comparing(p -> p.sellingPriceUsd);
      case "-price" -> Comparator.comparing((Product p) -> p.sellingPriceUsd).reversed();
      case "updated" -> Comparator.comparing((Product p) -> p.updatedAt).reversed();
      case "name" -> Comparator.comparing(p -> p.name.toLowerCase(Locale.ROOT));
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported sort");
    };
    return products.findByPublicationStatusAndActiveTrueAndCategoryActiveTrueOrderByNameAsc(PublicationStatus.PUBLISHED).stream()
        .filter(p -> term==null || term.isBlank() || p.name.toLowerCase(Locale.ROOT).contains(term)
            || (p.description!=null && p.description.toLowerCase(Locale.ROOT).contains(term))
            || p.sku.toLowerCase(Locale.ROOT).contains(term))
        .filter(p -> category==null || p.category.getId().equals(category))
        .filter(p -> featured==null || p.featured==featured)
        .filter(p -> newArrival==null || p.newArrival==newArrival)
        .filter(p -> availability==null || CatalogueDtos.availability(p)==availability)
        .sorted(order).skip((long)page*size).limit(size).map(PublicProductView::of).toList();
  }
  @Transactional(readOnly=true) public List<PublicProductView> posProducts() {
    return products.findByActiveTrueAndCategoryActiveTrueOrderByNameAsc().stream().map(PublicProductView::of).toList();
  }
  @Transactional(readOnly=true) public boolean imageIsPublic(String key) {
    return products.existsByImageKeyAndPublicationStatusAndActiveTrueAndCategoryActiveTrue(key,PublicationStatus.PUBLISHED);
  }
  @Transactional(readOnly=true) public PublicProductView publicProduct(UUID id) {
    return PublicProductView.of(products.findByIdAndPublicationStatusAndActiveTrueAndCategoryActiveTrue(id,PublicationStatus.PUBLISHED)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found")));
  }
  private void apply(Product p, ProductInput input) {
    String newSku=sku(input.sku()), barcode=optional(input.barcode());
    if (!p.sku.equals(newSku) && products.existsBySku(newSku)) throw new ResponseStatusException(HttpStatus.CONFLICT,"SKU already exists");
    if (barcode != null && !barcode.equals(p.barcode) && products.existsByBarcode(barcode)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Barcode already exists");
    p.sku=newSku; p.barcode=barcode; p.name=name(input.name()); p.description=optional(input.description());
    p.category=category(input.categoryId()); p.costPriceUsd=input.costPriceUsd(); p.sellingPriceUsd=input.sellingPriceUsd();
    p.lowStockThreshold=input.lowStockThreshold(); p.featured=input.featured(); p.newArrival=input.newArrival(); p.updatedAt=Instant.now();
  }
  @Transactional public ProductView createProduct(CreateProductInput input, Authentication auth) {
    ProductInput data=input.product(); Product p=new Product(sku(data.sku()),category(data.categoryId()));
    if (products.existsBySku(p.sku)) throw new ResponseStatusException(HttpStatus.CONFLICT,"SKU already exists");
    apply(p,data); products.saveAndFlush(p);
    UUID actor=actor(auth);
    if (input.initialStock()>0) {
      p.stockQuantity=input.initialStock(); products.saveAndFlush(p);
      movements.saveAndFlush(new InventoryMovement(p,MovementType.INITIAL_STOCK,input.initialStock(),p.stockQuantity,"Initial stock",actor));
    }
    audit.record(actor,"PRODUCT_CREATED","product",p.id.toString(),null,p.sku+"; initial stock="+p.stockQuantity);
    return ProductView.of(p);
  }
  @Transactional public ProductView updateProduct(UUID id, ProductInput input, Authentication auth) {
    Product p=product(id); String before=p.sku+"; price="+p.sellingPriceUsd;
    apply(p,input); products.saveAndFlush(p);
    audit.record(actor(auth),"PRODUCT_UPDATED","product",id.toString(),before,p.sku+"; price="+p.sellingPriceUsd);
    return ProductView.of(p);
  }
  @Transactional public ProductView publication(UUID id, PublicationStatus status, Authentication auth) {
    Product p=product(id); PublicationStatus before=p.publicationStatus;
    if (status == PublicationStatus.PUBLISHED && (!p.active || !p.category.active))
      throw new ResponseStatusException(HttpStatus.CONFLICT,"Activate product and category before publishing");
    p.publicationStatus=status; p.updatedAt=Instant.now(); products.saveAndFlush(p);
    audit.record(actor(auth),"PRODUCT_PUBLICATION_CHANGED","product",id.toString(),before.name(),status.name());
    return ProductView.of(p);
  }
  @Transactional public ProductView activation(UUID id, boolean active, Authentication auth) {
    Product p=product(id); boolean before=p.active; p.active=active; p.updatedAt=Instant.now(); products.saveAndFlush(p);
    audit.record(actor(auth),"PRODUCT_ACTIVATION_CHANGED","product",id.toString(),String.valueOf(before),String.valueOf(active));
    return ProductView.of(p);
  }
  @Transactional public ProductView setImage(UUID id, String imageKey, Authentication auth) {
    Product p=product(id); String before=p.imageKey; p.imageKey=imageKey; p.updatedAt=Instant.now(); products.saveAndFlush(p);
    audit.record(actor(auth),"PRODUCT_IMAGE_CHANGED","product",id.toString(),before,imageKey);
    return ProductView.of(p);
  }
  @Transactional public MovementView restock(UUID id, StockInput input, Authentication auth) {
    return move(id,input.quantity(),MovementType.RESTOCK,optional(input.reason()),auth);
  }
  @Transactional public MovementView adjust(UUID id, AdjustmentInput input, Authentication auth) {
    if (input.quantityChange()==0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quantity change must not be zero");
    return move(id,input.quantityChange(),input.quantityChange()>0?MovementType.ADJUSTMENT_INCREASE:MovementType.ADJUSTMENT_DECREASE,input.reason().trim(),auth);
  }
  private MovementView move(UUID id, int change, MovementType type, String reason, Authentication auth) {
    Product p=lockedProduct(id);
    long result=(long)p.stockQuantity+change;
    if (result<0 || result>Integer.MAX_VALUE) throw new ResponseStatusException(HttpStatus.CONFLICT,"Invalid resulting stock quantity");
    int before=p.stockQuantity; p.stockQuantity=(int)result; p.updatedAt=Instant.now(); products.saveAndFlush(p);
    UUID actor=actor(auth);
    InventoryMovement m=movements.saveAndFlush(new InventoryMovement(p,type,change,p.stockQuantity,reason,actor));
    audit.record(actor,"INVENTORY_"+type.name(),"product",id.toString(),String.valueOf(before),String.valueOf(result));
    return MovementView.of(m);
  }
  @Transactional(readOnly=true) public List<MovementView> movements(UUID id) {
    product(id); return movements.findByProductIdOrderByOccurredAtDesc(id).stream().map(MovementView::of).toList();
  }
  @Transactional(readOnly=true) public List<ProductView> lowStock() { return products.lowStock().stream().map(ProductView::of).toList(); }
  @Transactional(readOnly=true) public List<ProductView> outOfStock() { return products.findByStockQuantityOrderByNameAsc(0).stream().map(ProductView::of).toList(); }
}
