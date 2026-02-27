FROM maven:3-eclipse-temurin-21 as build
WORKDIR /app
COPY . ./
RUN mvn -B -e -s settings.xml -Pprod clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
LABEL org.opencontainers.image.authors=contact@pcagrade.com
COPY --from=build /app/target/*.jar retriever.jar
EXPOSE 8080
CMD ["java", "-jar", "retriever.jar"]
