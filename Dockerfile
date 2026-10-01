# =============================================================================
# Stage 1 — Build the React frontend
# =============================================================================
FROM node:22-alpine AS frontend-builder

WORKDIR /build

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# =============================================================================
# Stage 2 — Build the Java backend
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS backend-builder

WORKDIR /build

# Copy Maven wrapper and pom.xml first for dependency caching
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn .mvn

# Download dependencies (cached unless pom.xml changes)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY backend/src src

# Build the application JAR
RUN ./mvnw package -B -DskipTests

# =============================================================================
# Stage 3 — Minimal runtime
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Copy the built JAR from stage 2
COPY --from=backend-builder /build/target/*.jar app.jar

# Copy built frontend from stage 1 into Spring Boot's static resources path
COPY --from=frontend-builder /build/dist ./static

# Create data directory for SQLite persistence (mount a volume here)
RUN mkdir -p /app/data && \
    addgroup -S appgroup && adduser -S appuser -G appgroup && \
    chown -R appuser:appgroup /app
ENV DB_PATH=/app/data/releases.db

USER appuser:appgroup

EXPOSE 8080

CMD ["java", "-jar", "app.jar", "--spring.web.resources.static-locations=file:./static/"]
