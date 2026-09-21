FROM node:24-alpine AS frontend
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.11-eclipse-temurin-21-alpine AS backend
WORKDIR /build
COPY pom.xml ./
COPY src ./src
COPY --from=frontend /build/frontend/dist ./frontend/dist
RUN mkdir -p frontend && mvn -DskipTests -Dskip.frontend=true package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S pos && adduser -S pos -G pos
WORKDIR /app
COPY --from=backend /build/target/pos-*.jar /app/pos.jar
RUN mkdir -p /app/data/product-images /app/logs && chown -R pos:pos /app
USER pos
EXPOSE 8080
ENTRYPOINT ["java","-Dspring.profiles.active=prod","-jar","/app/pos.jar"]
