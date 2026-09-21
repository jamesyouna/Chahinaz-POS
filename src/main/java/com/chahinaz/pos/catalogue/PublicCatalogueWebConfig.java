package com.chahinaz.pos.catalogue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PublicCatalogueWebConfig implements WebMvcConfigurer {
  private final String[] origins;
  public PublicCatalogueWebConfig(@Value("${pos.public-catalogue.allowed-origins:https://chahinazdollar.store,https://www.chahinazdollar.store}") String origins) {
    this.origins=origins.split(",");
  }
  @Override public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/public/**").allowedOrigins(origins).allowedMethods("GET","HEAD","OPTIONS")
        .allowedHeaders("Accept","Content-Type").allowCredentials(false).maxAge(3600);
  }
}
