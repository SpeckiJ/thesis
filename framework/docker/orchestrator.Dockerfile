
# Build from sources
FROM gradle:jdk11 as builder
COPY . /home/gradle/src/
WORKDIR /home/gradle/src/
RUN gradle build --no-daemon -p orchestrator

# Create slim Running container
FROM openjdk:11-jre-slim
COPY --from=builder /home/gradle/src/orchestrator/build/distributions/orchestrator-boot-1.0-SNAPSHOT.tar /app/orchestrator.tar
WORKDIR /app
RUN tar -xvf orchestrator.tar
WORKDIR /app/orchestrator-boot-1.0-SNAPSHOT
CMD bin/orchestrator
