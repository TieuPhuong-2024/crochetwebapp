# Stage 1: Build app và custom JRE
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app
COPY . .

ARG ENV
ARG EMAIL
ARG DB_HOST
ARG DB_PORT
ARG DB_NAME
ARG DB_USERNAME
ARG DB_PASSWORD
ARG GOOGLE_CLIENT_ID
ARG GOOGLE_CLIENT_SECRET
ARG TOKEN_SECRET
ARG APP_PASSWORD
ARG SERVICE_ACCOUNT_KEY
ARG ALLOWED_ORIGINS

# Environment variables
ENV ENV ${ENV} \
    EMAIL ${EMAIL} \
    DB_HOST ${DB_HOST} \
    DB_PORT ${DB_PORT} \
    DB_NAME ${DB_NAME} \
    DB_USERNAME ${DB_USERNAME} \
    DB_PASSWORD ${DB_PASSWORD} \
    GOOGLE_CLIENT_ID ${GOOGLE_CLIENT_ID} \
    GOOGLE_CLIENT_SECRET ${GOOGLE_CLIENT_SECRET} \
    TOKEN_SECRET ${TOKEN_SECRET} \
    APP_PASSWORD ${APP_PASSWORD} \
    SERVICE_ACCOUNT_KEY ${SERVICE_ACCOUNT_KEY} \
    ALLOWED_ORIGINS ${ALLOWED_ORIGINS}

RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon -x test

RUN mkdir -p build/extracted && (cd build/extracted && jar xf ../libs/*.jar)
RUN jdeps --ignore-missing-deps -q --recursive --multi-release 21 \
    --print-module-deps --class-path 'build/extracted/BOOT-INF/lib/*' build/libs/*.jar > deps.info
RUN jlink --add-modules $(cat deps.info),jdk.crypto.ec,jdk.crypto.cryptoki --compress 2 --no-header-files --no-man-pages --output /custom_jre

# Stage 2: Runtime siêu nhỏ bằng Alpine
FROM alpine:3.18
RUN apk add --no-cache ca-certificates bash libc6-compat
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=build /custom_jre $JAVA_HOME
WORKDIR /app
COPY --from=build /workspace/app/build/extracted/BOOT-INF/lib /app/lib
COPY --from=build /workspace/app/build/extracted/META-INF /app/META-INF
COPY --from=build /workspace/app/build/extracted/BOOT-INF/classes /app

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-cp", ".:/app/lib/*", "org.crochet.CrochetApplication"]