# Backend image for hosting (Vercel container runtime, or any Docker host).
# Build:  docker build -t centsible-backend .
# Run:    docker run -p 8080:8080 -e PORT=8080 -e JWT_SECRET=... -e DATABASE_URL=postgres://... centsible-backend

# ---- Build stage: compile and package the jar ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# Copy the pom first so dependency downloads are cached unless it changes
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- Run stage: JRE only ----
# The full JRE image (not a "headless"/slim one): the PDF reader (PDFBox) needs the java.desktop module.
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 centsible
COPY --from=build /build/target/*.jar app.jar
USER centsible

# Always run with the production profile: it has no fallback secrets, so a missing JWT_SECRET
# or DATABASE_URL stops the app at startup instead of running with the public dev defaults.
ENV SPRING_PROFILES_ACTIVE=prod \
    PORT=8080 \
    JAVA_TOOL_OPTIONS="-Djava.awt.headless=true -XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
