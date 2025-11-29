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
FROM debian:latest AS frontend
RUN apt-get update
RUN apt-get install -y libxi6 libgtk-3-0 libxrender1 libxtst6 libxslt1.1 curl git wget unzip gdb libstdc++6 libglu1-mesa fonts-droid-fallback lib32stdc++6 python3
RUN apt-get clean
RUN git clone https://github.com/flutter/flutter.git /usr/local/flutter
ENV PATH="/usr/local/flutter/bin:/usr/local/flutter/bin/cache/dart-sdk/bin:${PATH}"
RUN flutter doctor -v
RUN flutter channel stable
RUN flutter upgrade
RUN flutter config --enable-web
RUN mkdir /app/
RUN git clone https://github.com/mickkc/vibin-app /app/
WORKDIR /app/
RUN git checkout 0.0.1-beta.2
RUN flutter clean
RUN flutter pub get
RUN flutter pub run build_runner build --delete-conflicting-outputs
RUN flutter build web --debug --pwa-strategy=none --dart-define=VIBIN_EMBEDDED_MODE=true --base-href "/web/"

# Stage 4: Create the Runtime Image
FROM amazoncorretto:21 AS runtime

EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/backend.jar
COPY --from=frontend /app/build/web/ /app/frontend/

ENV FRONTEND_DIR="/app/frontend"

ENTRYPOINT ["java","-jar","/app/backend.jar"]
