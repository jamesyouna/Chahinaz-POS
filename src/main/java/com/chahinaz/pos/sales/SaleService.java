package com.chahinaz.pos.sales;
import com.chahinaz.pos.audit.AuditService;
import com.chahinaz.pos.catalogue.*;
import com.chahinaz.pos.employee.*;
import com.chahinaz.pos.settings.SettingRepository;
import static com.chahinaz.pos.sales.SaleDtos.*;
import java.math.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SaleService {
 private final SaleRepository sales; private final ProductRepository products; private final InventoryMovementRepository movements;
 private final EmployeeRepository employees; private final SettingRepository settings; private final AuditService audit; private final JdbcTemplate jdbc;
 public SaleService(SaleRepository s,ProductRepository p,InventoryMovementRepository m,EmployeeRepository e,SettingRepository st,AuditService a,JdbcTemplate j){sales=s;products=p;movements=m;employees=e;settings=st;audit=a;jdbc=j;}
 private Employee actor(Authentication a){return employees.findByUsernameIgnoreCase(a.getName()).orElseThrow();}
 private Sale locked(UUID id,Authentication auth){Sale s=sales.findLocked(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Sale not found")); authorize(s,auth);return s;}
 private void authorize(Sale s,Authentication auth){boolean elevated=auth.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_ADMIN")||x.getAuthority().equals("ROLE_MANAGER"));if(!elevated&&!s.employee.getUsername().equalsIgnoreCase(auth.getName()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Sale belongs to another cashier");}
 private void open(Sale s){if(s.status!=SaleStatus.OPEN)throw new ResponseStatusException(HttpStatus.CONFLICT,"Only an open sale can be modified");}
 private void totals(Sale s){s.subtotalUsd=s.items.stream().map(i->i.lineTotalUsd).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2);s.totalUsd=s.subtotalUsd;}
 private String number(){Long n=jdbc.queryForObject("SELECT nextval('sale_number_seq')",Long.class);return "CH-"+String.format("%06d",n);}
 @Transactional public ReceiptView create(CreateSaleInput in,Authentication auth){Employee e=actor(auth);Sale s=sales.saveAndFlush(new Sale(number(),e,in==null?null:blank(in.note())));audit.record(e.id,"SALE_CREATED","sale",s.id.toString(),null,s.saleNumber);return view(s);}
 @Transactional public ReceiptView add(UUID id,AddItemInput in,Authentication auth){Sale s=locked(id,auth);open(s);Product p=products.findById(in.productId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found"));if(!p.isActive()||!p.getCategory().isActive())throw new ResponseStatusException(HttpStatus.CONFLICT,"Product is not available for POS");SaleItem item=s.items.stream().filter(i->i.getProduct().getId().equals(p.getId())).findFirst().orElse(null);if(item==null){item=new SaleItem(s,p,in.quantity());s.items.add(item);}else item.setQuantity(Math.addExact(item.getQuantity(),in.quantity()));totals(s);sales.saveAndFlush(s);return view(s);}
 @Transactional public ReceiptView quantity(UUID id,UUID itemId,QuantityInput in,Authentication auth){Sale s=locked(id,auth);open(s);SaleItem i=s.items.stream().filter(x->x.id.equals(itemId)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Sale item not found"));i.setQuantity(in.quantity());totals(s);sales.saveAndFlush(s);return view(s);}
 @Transactional public ReceiptView remove(UUID id,UUID itemId,Authentication auth){Sale s=locked(id,auth);open(s);if(!s.items.removeIf(i->i.id.equals(itemId)))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Sale item not found");totals(s);sales.saveAndFlush(s);return view(s);}
 @Transactional public ReceiptView complete(UUID id,CompleteSaleInput in,Authentication auth){Sale s=locked(id,auth);open(s);if(s.items.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot complete an empty sale");
   BigDecimal rate=rate(); BigDecimal usd=money(in.paidUsd()),lbp=lbp(in.paidLbp()),whish=money(in.whishUsd());
   if(whish.signum()>0&&!Boolean.TRUE.equals(in.whishManuallyConfirmed()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Whish payment must be manually confirmed by the cashier");
   BigDecimal dueLbp=s.totalUsd.multiply(rate).setScale(0,RoundingMode.HALF_UP);
   BigDecimal paidLbpValue=usd.add(whish).multiply(rate).setScale(0,RoundingMode.HALF_UP).add(lbp);
   if(paidLbpValue.compareTo(dueLbp)<0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Payment is insufficient");
   List<SaleItem> ordered=s.items.stream().sorted(Comparator.comparing(i->i.getProduct().getId())).toList();Map<UUID,Product> lockedProducts=new HashMap<>();
   for(SaleItem item:ordered){Product p=products.findLockedById(item.getProduct().getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Product no longer exists"));if(!p.isActive())throw new ResponseStatusException(HttpStatus.CONFLICT,"Product is no longer active: "+item.productNameSnapshot);if(p.getStockQuantity()<item.getQuantity())throw new ResponseStatusException(HttpStatus.CONFLICT,"Insufficient stock for "+item.productNameSnapshot);lockedProducts.put(p.getId(),p);}
   UUID employee=actor(auth).id;
   for(SaleItem item:ordered){Product p=lockedProducts.get(item.getProduct().getId());p.deductStock(item.getQuantity());products.save(p);movements.save(new InventoryMovement(p,MovementType.SALE,-item.getQuantity(),p.getStockQuantity(),"Sale "+s.saleNumber,employee,s.id));}
   BigDecimal change=paidLbpValue.subtract(dueLbp);BigDecimal changeUsd=change.divideToIntegralValue(rate).setScale(2);BigDecimal changeLbp=change.subtract(changeUsd.multiply(rate)).setScale(0,RoundingMode.UNNECESSARY);
   s.exchangeRate=rate;s.amountPaidUsd=usd;s.amountPaidLbp=lbp;s.whishAmountUsd=whish;s.changeUsd=changeUsd;s.changeLbp=changeLbp;s.status=SaleStatus.COMPLETED;s.completedAt=Instant.now();
   if(usd.signum()>0)s.payments.add(new Payment(s,PaymentType.USD_CASH,usd,null,false));if(lbp.signum()>0)s.payments.add(new Payment(s,PaymentType.LBP_CASH,null,lbp,false));if(whish.signum()>0)s.payments.add(new Payment(s,PaymentType.WHISH_MANUAL,whish,null,true));
   sales.saveAndFlush(s);audit.record(employee,"SALE_COMPLETED","sale",s.id.toString(),"OPEN",s.saleNumber+"; total="+s.totalUsd);return view(s);
 }
 @Transactional(readOnly=true) public ReceiptView get(UUID id,Authentication auth){Sale s=sales.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Sale not found"));authorize(s,auth);return view(s);}
 @Transactional(readOnly=true) public Page<ReceiptView> history(Pageable page,Authentication auth){Employee e=actor(auth);boolean elevated=auth.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_ADMIN")||x.getAuthority().equals("ROLE_MANAGER"));return (elevated?sales.findAllByOrderByCreatedAtDesc(page):sales.findByEmployeeIdOrderByCreatedAtDesc(e.id,page)).map(this::view);}
 private BigDecimal rate(){String value=settings.findById("currency.usd_to_lbp").orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Configure currency.usd_to_lbp before checkout")).value;try{BigDecimal r=new BigDecimal(value).setScale(2);if(r.signum()<=0)throw new Exception();return r;}catch(Exception e){throw new ResponseStatusException(HttpStatus.CONFLICT,"Configured exchange rate is invalid");}}
 private static BigDecimal money(BigDecimal x){return (x==null?BigDecimal.ZERO:x).setScale(2,RoundingMode.UNNECESSARY);} private static BigDecimal lbp(BigDecimal x){return (x==null?BigDecimal.ZERO:x).setScale(0,RoundingMode.UNNECESSARY);} private static String blank(String x){return x==null||x.isBlank()?null:x.trim();}
 private ReceiptView view(Sale s){return new ReceiptView(s.id,s.saleNumber,s.status,s.createdAt,s.completedAt,s.employee.getDisplayName(),s.items.stream().map(i->new ItemView(i.id,i.getProduct().getId(),i.skuSnapshot,i.productNameSnapshot,i.unitPriceUsd,i.getQuantity(),i.lineTotalUsd)).toList(),s.subtotalUsd,s.totalUsd,s.exchangeRate,s.payments.stream().map(p->new PaymentView(p.type,p.amountUsd,p.amountLbp,p.manuallyConfirmed)).toList(),s.changeUsd,s.changeLbp,s.note);}
}
