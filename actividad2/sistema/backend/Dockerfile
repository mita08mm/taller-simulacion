FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY lib lib
COPY src/main/java src/main/java
RUN javac -encoding UTF-8 -cp lib/postgresql-42.7.5.jar -d target/classes src/main/java/com/actividad2/*.java

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/classes target/classes
COPY lib/postgresql-42.7.5.jar lib/postgresql-42.7.5.jar
COPY src/main/resources/schema.sql src/main/resources/schema.sql
EXPOSE 7070
CMD ["sh", "-c", "PUERTO=${PORT:-7070} java -cp target/classes:src/main/resources:lib/postgresql-42.7.5.jar com.actividad2.Main"]
