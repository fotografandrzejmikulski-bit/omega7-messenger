FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/omega7-server-1.0.0.jar /app/app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
