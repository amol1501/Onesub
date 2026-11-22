FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace
COPY onesub-backend/pom.xml ./pom.xml
COPY onesub-backend/src ./src
RUN mvn --batch-mode -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -S onesub && adduser -S onesub -G onesub
COPY --from=build --chown=onesub:onesub /workspace/target/onesub-backend-0.0.1-SNAPSHOT.jar /app/onesub.jar
USER onesub
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/onesub.jar"]
