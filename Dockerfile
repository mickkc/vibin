# Stage 1: Cache Gradle dependencies
FROM gradle:jdk-21-and-24-corretto AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME /home/gradle/cache_home
COPY backend/build.gradle.* backend/gradle.properties /home/gradle/app/
COPY backend/gradle/ /home/gradle/app/gradle/
WORKDIR /home/gradle/app
RUN gradle clean build -i --stacktrace

# Stage 2: Build Application
FROM gradle:jdk-21-and-24-corretto AS build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY backend/ /usr/src/app/
WORKDIR /usr/src/app
COPY --chown=gradle:gradle backend/ /home/gradle/src
WORKDIR /home/gradle/src
# Build the fat JAR, Gradle also supports shadow
# and boot JAR by default.
RUN gradle buildFatJar --no-daemon

# Stage 3: Build Frontend
FROM ghcr.io/cirruslabs/flutter:stable AS frontend

ARG VIBIN_TAG=0.0.1-beta.4

# Install git
RUN apt-get update && apt-get install -y --no-install-recommends git && rm -rf /var/lib/apt/lists/*

# Clone the vibin-app repository
WORKDIR /tmp/app/
RUN git clone --depth 1 --branch ${VIBIN_TAG} https://github.com/mickkc/vibin-app .

# Set up Flutter environment
RUN flutter config --no-analytics --enable-web
RUN flutter pub get --offline || flutter pub get

# Build the web application
RUN flutter pub run build_runner build --delete-conflicting-outputs
RUN flutter build web --debug --pwa-strategy=none --dart-define=VIBIN_EMBEDDED_MODE=true --base-href "/web/"

# Stage 4: Create the Runtime Image
FROM debian:latest AS runtime

# Install Chromium and ChromeDriver
RUN apt-get update && apt-get install -y \
    openjdk-21-jre-headless \
    chromium \
    chromium-driver \
    && rm -rf /var/lib/apt/lists/*

# Set environment variables for Selenium
ENV CHROME_BINARY_PATH /usr/bin/chromium
ENV CHROMEDRIVER /usr/bin/chromedriver

RUN mkdir /app

# Copy the backend jar and static frontend files
COPY --from=build /home/gradle/src/build/libs/*.jar /app/backend.jar
COPY --from=frontend /tmp/app/build/web/ /app/frontend/

ENV FRONTEND_DIR="/app/frontend"

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/backend.jar"]
