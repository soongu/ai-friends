# =========================================================
# 1단계: Gradle 빌드 스테이지
# =========================================================
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /workspace

# Gradle wrapper 및 빌드 스크립트 먼저 복사 → 의존성 캐시 활용
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle build.gradle ./

RUN chmod +x gradlew \
    && ./gradlew --version \
    && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

# 소스 복사 후 부트 JAR 빌드
COPY src ./src

RUN ./gradlew clean bootJar -x test --no-daemon

# =========================================================
# 2단계: 런타임 스테이지 (슬림 JRE)
# =========================================================
FROM eclipse-temurin:25-jre

WORKDIR /app

# 헬스체크용 curl 설치 (Debian 기반 이미지)
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /workspace/build/libs/*.jar app.jar

ENV JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE=docker \
    TZ=Asia/Seoul

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
