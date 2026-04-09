FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw ./

RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw package -DskipTests --no-transfer-progress

FROM eclipse-temurin:25-jre AS extractor
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM gcr.io/distroless/java25-debian13
WORKDIR /app
COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]