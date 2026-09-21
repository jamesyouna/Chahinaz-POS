package com.chahinaz.pos;

import com.chahinaz.pos.employee.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(properties={"POS_BOOTSTRAP_ADMIN_USERNAME=bootstrap","POS_BOOTSTRAP_ADMIN_PASSWORD=development-test-password-only","POS_SECURE_COOKIE=false","POS_IMAGE_DIR=./target/phase2-test-images"})
@AutoConfigureMockMvc
class Phase2PostgresIntegrationTest {
  @Container static PostgreSQLContainer postgres=new PostgreSQLContainer("postgres:16-alpine");
  @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url",postgres::getJdbcUrl);
    properties.add("spring.datasource.username",postgres::getUsername);
    properties.add("spring.datasource.password",postgres::getPassword);
  }
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired EmployeeRepository employees;
  private String unique() { return UUID.randomUUID().toString().substring(0,8); }
  private String categoryBody(String name,boolean active) { return "{\"name\":\""+name+"\",\"description\":\"Household\",\"active\":"+active+"}"; }
  private UUID category(String name) throws Exception {
    String body=mvc.perform(post("/api/management/categories").with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(categoryBody(name,true)))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    return UUID.fromString(body.replaceAll(".*\"id\":\"([^\"]+)\".*","$1"));
  }
  private String productBody(String sku,String barcode,UUID category,int stock,String cost,String price) {
    return "{\"product\":{\"sku\":\""+sku+"\",\"barcode\":"+(barcode==null?"null":"\""+barcode+"\"")+
      ",\"name\":\"Test item\",\"description\":\"Useful item\",\"categoryId\":\""+category+
      "\",\"costPriceUsd\":"+cost+",\"sellingPriceUsd\":"+price+
      ",\"lowStockThreshold\":2,\"featured\":true,\"newArrival\":false},\"initialStock\":"+stock+"}";
  }
  private UUID product(String sku,String barcode,UUID category,int stock) throws Exception {
    String body=mvc.perform(post("/api/management/products").with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(productBody(sku,barcode,category,stock,"0.75","2.50")))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    return UUID.fromString(body.replaceAll(".*\"id\":\"([^\"]+)\".*","$1"));
  }
  @Test void v2MigrationCategoriesPermissionsAndAudit() throws Exception {
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='2' AND success=true",Integer.class));
    String name="Category"+unique(); UUID id=category(name);
    mvc.perform(post("/api/management/categories").with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(categoryBody(name.toLowerCase(),true))).andExpect(status().isConflict());
    mvc.perform(put("/api/management/categories/{id}",id).with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(categoryBody(name,false)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
    mvc.perform(post("/api/management/categories").with(user("teller").roles("TELLER")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(categoryBody("Blocked"+unique(),true))).andExpect(status().isForbidden());
    mvc.perform(get("/api/management/categories")).andExpect(status().is3xxRedirection());
    mvc.perform(get("/api/pos/categories").with(user("teller").roles("TELLER"))).andExpect(status().isOk());
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='CATEGORY_CREATED' AND entity_id=?",Integer.class,id.toString()));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='CATEGORY_UPDATED' AND entity_id=?",Integer.class,id.toString()));
  }
  @Test void managerMayEditButTellerCannotAndCsrfIsRequired() throws Exception {
    String name="manager"+unique(); employees.saveAndFlush(new Employee(name,"Manager","not-used-in-mock-auth",Role.MANAGER));
    mvc.perform(post("/api/management/categories").with(user(name).roles("MANAGER"))
        .contentType(MediaType.APPLICATION_JSON).content(categoryBody("Manager"+unique(),true))).andExpect(status().isForbidden());
    mvc.perform(post("/api/management/categories").with(user(name).roles("MANAGER")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(categoryBody("Manager"+unique(),true))).andExpect(status().isCreated());
    mvc.perform(get("/api/management/products").with(user("teller").roles("TELLER"))).andExpect(status().isForbidden());
    mvc.perform(post("/api/public/products").with(user(name).roles("MANAGER")).with(csrf())).andExpect(status().isForbidden());
  }
  @Test void productUniquenessMoneyValidationAndPublication() throws Exception {
    UUID category=category("Category"+unique()); String sku="SKU-"+unique(),barcode="BAR-"+unique(); UUID id=product(sku,barcode,category,3);
    assertEquals(new BigDecimal("2.50"),jdbc.queryForObject("SELECT selling_price_usd FROM product WHERE id=?",BigDecimal.class,id));
    mvc.perform(post("/api/management/products").with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(productBody(sku,null,category,0,"1","2"))).andExpect(status().isConflict());
    mvc.perform(post("/api/management/products").with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(productBody("SKU-"+unique(),barcode,category,0,"1","2"))).andExpect(status().isConflict());
    mvc.perform(post("/api/management/products").with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(productBody("SKU-"+unique(),null,category,0,"-1","2"))).andExpect(status().isBadRequest());
    mvc.perform(post("/api/management/products").with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(productBody("SKU-"+unique(),null,category,0,"1","-2"))).andExpect(status().isBadRequest());
    mvc.perform(get("/api/public/catalogue/products/{id}",id)).andExpect(status().isNotFound());
    mvc.perform(patch("/api/management/products/{id}/publication",id).with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("\"PUBLISHED\"")).andExpect(status().isOk());
    String publicBody=mvc.perform(get("/api/public/catalogue/products/{id}",id)).andExpect(status().isOk())
        .andExpect(jsonPath("$.sellingPriceUsd").value(2.5)).andExpect(jsonPath("$.costPriceUsd").doesNotExist())
        .andExpect(jsonPath("$.stockQuantity").doesNotExist()).andReturn().getResponse().getContentAsString();
    assertFalse(publicBody.contains("actorEmployeeId"));
    mvc.perform(patch("/api/management/products/{id}/active",id).with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("false")).andExpect(status().isOk());
    mvc.perform(get("/api/public/catalogue/products/{id}",id)).andExpect(status().isNotFound());
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='PRODUCT_PUBLICATION_CHANGED' AND entity_id=?",Integer.class,id.toString()));
  }
  @Test void inactiveCategoryHidesPublishedProductsUntilReactivated() throws Exception {
    String name="Visibility"+unique(); UUID category=category(name); UUID product=product("SKU-"+unique(),null,category,2);
    mvc.perform(patch("/api/management/products/{id}/publication",product).with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("\"PUBLISHED\"")).andExpect(status().isOk());
    mvc.perform(put("/api/management/categories/{id}",category).with(user("bootstrap").roles("MANAGER")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(categoryBody(name,false))).andExpect(status().isOk());
    mvc.perform(get("/api/public/catalogue/products/{id}",product)).andExpect(status().isNotFound());
    mvc.perform(get("/api/public/catalogue/categories")).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(name))));
    mvc.perform(put("/api/management/categories/{id}",category).with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(categoryBody(name,true))).andExpect(status().isOk());
    mvc.perform(get("/api/public/catalogue/products/{id}",product)).andExpect(status().isOk());
  }
  @Test void stockMovementsAreAtomicAndQueriesIdentifyLowStock() throws Exception {
    UUID category=category("Category"+unique()); UUID id=product("SKU-"+unique(),null,category,3);
    mvc.perform(post("/api/management/products/{id}/restock",id).with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":2,\"reason\":\"Delivery\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.resultingQuantity").value(5));
    mvc.perform(post("/api/management/products/{id}/adjust",id).with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"quantityChange\":-4,\"reason\":\"Count correction\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.resultingQuantity").value(1));
    mvc.perform(post("/api/management/products/{id}/adjust",id).with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"quantityChange\":-5,\"reason\":\"Invalid\"}"))
        .andExpect(status().isConflict());
    assertEquals(1,jdbc.queryForObject("SELECT stock_quantity FROM product WHERE id=?",Integer.class,id));
    assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM inventory_movement WHERE product_id=?",Integer.class,id));
    mvc.perform(get("/api/management/products/low-stock").with(user("bootstrap").roles("ADMIN")))
        .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(id.toString())));
    mvc.perform(post("/api/management/products/{id}/adjust",id).with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"quantityChange\":-1,\"reason\":\"Count correction\"}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/management/products/out-of-stock").with(user("bootstrap").roles("ADMIN")))
        .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(id.toString())));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='INVENTORY_RESTOCK' AND entity_id=?",Integer.class,id.toString()));
  }
  @Test void imageUploadValidatesContentAndHidesDraftImages() throws Exception {
    UUID category=category("Category"+unique()); UUID id=product("SKU-"+unique(),null,category,0);
    MockMultipartFile bad=new MockMultipartFile("file","../../bad.jpg","image/jpeg","not an image".getBytes());
    mvc.perform(multipart("/api/management/products/{id}/image",id).file(bad).with(user("bootstrap").roles("ADMIN")).with(csrf()))
        .andExpect(status().isBadRequest());
    BufferedImage pixel=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB); ByteArrayOutputStream bytes=new ByteArrayOutputStream();
    ImageIO.write(pixel,"png",bytes);
    MockMultipartFile good=new MockMultipartFile("file","../../original.png","image/png",bytes.toByteArray());
    String body=mvc.perform(multipart("/api/management/products/{id}/image",id).file(good).with(user("bootstrap").roles("ADMIN")).with(csrf()))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    assertFalse(body.contains("original.png")); String key=body.replaceAll(".*\"imageUrl\":\"/api/public/images/([^\"]+)\".*","$1");
    assertTrue(key.matches("[0-9a-f-]{36}\\.png"));
    mvc.perform(get("/api/public/images/{key}",key)).andExpect(status().isNotFound());
    mvc.perform(get("/api/public/images/{key}","..%2F..%2Fsecret")).andExpect(status().is4xxClientError());
    mvc.perform(patch("/api/management/products/{id}/publication",id).with(user("bootstrap").roles("ADMIN")).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("\"PUBLISHED\"")).andExpect(status().isOk());
    mvc.perform(get("/api/public/images/{key}",key)).andExpect(status().isOk()).andExpect(header().string("Content-Type","image/png"));
    assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='PRODUCT_IMAGE_CHANGED' AND entity_id=?",Integer.class,id.toString()));
  }
}
