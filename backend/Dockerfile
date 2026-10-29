# Imagen para Render: compila con el wrapper de Maven y corre con un JRE liviano.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=America/Lima
COPY --from=build /app/target/restaurant-comida-criolla-0.0.1-SNAPSHOT.jar app.jar
# Render asigna el puerto en $PORT; el plan gratuito tiene 512 MB.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
