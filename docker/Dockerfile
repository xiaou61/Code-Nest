FROM maven:3.9.11-eclipse-temurin-17 AS builder

WORKDIR /build
COPY . .
RUN mvn -pl xiaou-bootstrap -am clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app
ENV TZ=Asia/Shanghai
ENV SPRING_PROFILES_ACTIVE=docker
COPY --from=builder /build/xiaou-bootstrap/target/xiaou-bootstrap-*.jar /app/app.jar

EXPOSE 9999

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
