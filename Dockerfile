# La API: se compila con el JDK y se ejecuta con solo el JRE. Los tests se pasan en la integración continua
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
# Sin privilegios: la app no necesita ser root
RUN useradd --system --uid 1001 gespatiens
COPY --from=build /app/target/*.jar app.jar
USER gespatiens
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
