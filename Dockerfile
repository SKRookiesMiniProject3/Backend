# 빌드 환경 
FROM openjdk:17-alpine AS builder

RUN apk update && \
    apk add findutils && \
    rm -rf /var/cache/apk/*

WORKDIR /app

COPY .mvn .mvn/
COPY mvnw .
COPY mvnw.cmd .
COPY ./.mvn/wrapper/maven-wrapper.properties ./.mvn/wrapper/maven-wrapper.properties
RUN chmod +x mvnw


# 의존성 다운로드
COPY pom.xml ./
RUN ./mvnw dependency:go-offline -B 

# 소스 코드 복사
COPY src ./src

# 빌드
RUN ./mvnw clean package -DskipTests


# 런타임 환경 
FROM openjdk:17-alpine

RUN addgroup -S -g 1000 spring && adduser -S -u 1000 -G spring spring
USER spring

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar


EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]