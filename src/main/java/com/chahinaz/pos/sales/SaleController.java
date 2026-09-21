package com.chahinaz.pos.sales;
import static com.chahinaz.pos.sales.SaleDtos.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/pos/sales")
public class SaleController {
 private final SaleService service; public SaleController(SaleService s){service=s;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ReceiptView create(@Valid @RequestBody(required=false) CreateSaleInput in,Authentication a){return service.create(in,a);}
 @PostMapping("/{id}/items") public ReceiptView add(@PathVariable UUID id,@Valid @RequestBody AddItemInput in,Authentication a){return service.add(id,in,a);}
 @PutMapping("/{id}/items/{itemId}") public ReceiptView quantity(@PathVariable UUID id,@PathVariable UUID itemId,@Valid @RequestBody QuantityInput in,Authentication a){return service.quantity(id,itemId,in,a);}
 @DeleteMapping("/{id}/items/{itemId}") public ReceiptView remove(@PathVariable UUID id,@PathVariable UUID itemId,Authentication a){return service.remove(id,itemId,a);}
 @PostMapping("/{id}/complete") public ReceiptView complete(@PathVariable UUID id,@Valid @RequestBody CompleteSaleInput in,Authentication a){return service.complete(id,in,a);}
 @GetMapping("/{id}") public ReceiptView get(@PathVariable UUID id,Authentication a){return service.get(id,a);}
 @GetMapping public Page<ReceiptView> history(@PageableDefault(size=25,sort="createdAt",direction=Sort.Direction.DESC) Pageable p,Authentication a){return service.history(p,a);}
}
