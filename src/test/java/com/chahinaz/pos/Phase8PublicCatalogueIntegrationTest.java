package com.chahinaz.pos;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(properties={"POS_BOOTSTRAP_ADMIN_USERNAME=bootstrap","POS_BOOTSTRAP_ADMIN_PASSWORD=development-test-password-only","POS_SECURE_COOKIE=false"})
@AutoConfigureMockMvc
class Phase8PublicCatalogueIntegrationTest {
  @Container static PostgreSQLContainer postgres=new PostgreSQLContainer("postgres:16-alpine");
  @DynamicPropertySource static void database(DynamicPropertyRegistry p){p.add("spring.datasource.url",postgres::getJdbcUrl);p.add("spring.datasource.username",postgres::getUsername);p.add("spring.datasource.password",postgres::getPassword);}
  @Autowired MockMvc mvc; @Autowired JdbcTemplate db;
  UUID category(String name,boolean active){UUID id=UUID.randomUUID();Timestamp now=Timestamp.from(Instant.now());db.update("insert into category(id,name,active,created_at,updated_at) values(?,?,?,?,?)",id,name,active,now,now);return id;}
  UUID product(UUID category,String name,String status,boolean active,int stock,int threshold,boolean featured,boolean arrival){UUID id=UUID.randomUUID();Timestamp now=Timestamp.from(Instant.now());db.update("insert into product(id,sku,name,description,category_id,cost_price_usd,selling_price_usd,stock_quantity,low_stock_threshold,active,publication_status,featured,new_arrival,created_at,updated_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",id,"SKU-"+id,name,"Safe description",category,new BigDecimal("0.25"),new BigDecimal("1.00"),stock,threshold,active,status,featured,arrival,now,now);return id;}
  @Test void boundaryPublicationAvailabilityFiltersAndFields()throws Exception{
    UUID toys=category("Toys-"+UUID.randomUUID(),true);
    UUID in=product(toys,"Public Car","PUBLISHED",true,10,2,true,true);
    product(toys,"Low Item","PUBLISHED",true,2,2,false,false);
    product(toys,"Sold Item","PUBLISHED",true,0,2,false,false);
    product(toys,"Draft Secret","DRAFT",true,10,2,true,true);
    product(toys,"Inactive Secret","PUBLISHED",false,10,2,true,true);
    mvc.perform(get("/api/public/catalogue/products"))
      .andExpect(status().isOk()).andExpect(header().doesNotExist("Location")).andExpect(jsonPath("$.length()").value(3))
      .andExpect(jsonPath("$[?(@.name=='Public Car')].availability").value("IN_STOCK"))
      .andExpect(jsonPath("$[?(@.name=='Low Item')].availability").value("LOW_STOCK"))
      .andExpect(jsonPath("$[?(@.name=='Sold Item')].availability").value("OUT_OF_STOCK"))
      .andExpect(jsonPath("$[0].costPriceUsd").doesNotExist()).andExpect(jsonPath("$[0].stockQuantity").doesNotExist())
      .andExpect(jsonPath("$[0].lowStockThreshold").doesNotExist()).andExpect(jsonPath("$[0].employee").doesNotExist());
    mvc.perform(get("/api/public/catalogue/products").param("search","Public").param("category",toys.toString()).param("featured","true").param("newArrival","true"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(in.toString()));
    mvc.perform(get("/api/public/catalogue/products").param("page","1").param("size","2")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    mvc.perform(get("/api/public/catalogue/categories")).andExpect(status().isOk()).andExpect(header().doesNotExist("Location"))
      .andExpect(jsonPath("$[?(@.id=='"+toys+"')].name").exists());
    mvc.perform(get("/api/public/catalogue/products/{id}",in)).andExpect(status().isOk()).andExpect(header().doesNotExist("Location"));
    mvc.perform(head("/api/public/catalogue/products")).andExpect(status().isOk()).andExpect(header().doesNotExist("Location"));
  }
  @Test void corsIsNarrowAndPublicApiIsReadOnly()throws Exception{
    mvc.perform(options("/api/public/catalogue/products").header("Origin","https://chahinazdollar.store").header("Access-Control-Request-Method","GET"))
      .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","https://chahinazdollar.store"));
    mvc.perform(options("/api/public/catalogue/products").header("Origin","https://evil.example").header("Access-Control-Request-Method","GET")).andExpect(status().isForbidden());
    mvc.perform(post("/api/public/catalogue/products")).andExpect(status().isForbidden());
    mvc.perform(get("/api/public/products")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    mvc.perform(get("/api/auth/me")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    mvc.perform(get("/api/pos/products")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    mvc.perform(get("/api/management/products")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    mvc.perform(post("/api/management/products")).andExpect(status().isForbidden());
  }
}
