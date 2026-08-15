# syntax=docker/dockerfile:1

# 使用与项目 Java 21 和 Maven Wrapper 一致的构建环境。
FROM eclipse-temurin:21-jdk-alpine AS builder

ARG SERVICE
WORKDIR /workspace

# 先复制包装器和构建描述，让依赖下载层可被多个服务镜像复用。
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY packages/proto/pom.xml packages/proto/pom.xml
COPY services/pom.xml services/pom.xml
COPY services/${SERVICE}/pom.xml services/${SERVICE}/pom.xml
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -pl services/${SERVICE} -am dependency:go-offline

# 仅复制当前服务及其公共 Protobuf 源码，避免无关服务改变导致缓存失效。
COPY packages/proto/src packages/proto/src
COPY services/${SERVICE}/src services/${SERVICE}/src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp -DskipTests -pl services/${SERVICE} -am package

# JRE 运行层只保留确定命名的可执行 JAR，并使用非 root 用户。
FROM eclipse-temurin:21-jre-alpine AS runtime

ARG SERVICE
WORKDIR /app
RUN addgroup -S streamora && adduser -S -G streamora streamora
COPY --from=builder /workspace/services/${SERVICE}/target/${SERVICE}.jar /app/application.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    SPRING_PROFILES_ACTIVE=compose

USER streamora:streamora

# Alpine 自带的 wget 用于 Actuator 存活探测；端口由各运行单元注入。
HEALTHCHECK --interval=20s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -q -O /dev/null "http://127.0.0.1:${SERVER_PORT:-8080}/actuator/health/liveness" || exit 1

# exec 形式让 JVM 直接接收终止信号，配合 Spring 优雅关闭。
ENTRYPOINT ["java", "-jar", "/app/application.jar"]

