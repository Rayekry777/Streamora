# syntax=docker/dockerfile:1

# 使用与项目 Java 21 和 Maven Wrapper 一致的构建环境。
FROM eclipse-temurin:21-jdk-alpine AS builder

ARG SERVICE
WORKDIR /workspace

# 先复制完整 Reactor 的构建描述，让依赖下载层可被多个服务镜像复用。
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY packages/proto/pom.xml packages/proto/pom.xml
COPY services/pom.xml services/pom.xml
COPY services/gateway-service/pom.xml services/gateway-service/pom.xml
COPY services/admin-service/pom.xml services/admin-service/pom.xml
COPY services/identity-service/pom.xml services/identity-service/pom.xml
COPY services/user-service/pom.xml services/user-service/pom.xml
COPY services/video-service/pom.xml services/video-service/pom.xml
COPY services/media-service/pom.xml services/media-service/pom.xml
COPY services/transcode-worker/pom.xml services/transcode-worker/pom.xml
COPY services/playback-service/pom.xml services/playback-service/pom.xml
COPY services/danmaku-service/pom.xml services/danmaku-service/pom.xml
COPY services/comment-service/pom.xml services/comment-service/pom.xml
COPY services/engagement-service/pom.xml services/engagement-service/pom.xml
COPY services/feed-service/pom.xml services/feed-service/pom.xml
COPY services/search-service/pom.xml services/search-service/pom.xml
COPY services/pet-service/pom.xml services/pet-service/pom.xml
COPY services/agent-service/pom.xml services/agent-service/pom.xml
COPY services/moderation-service/pom.xml services/moderation-service/pom.xml
COPY services/notification-service/pom.xml services/notification-service/pom.xml
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
