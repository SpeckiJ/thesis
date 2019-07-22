
# Create slim Running container
FROM openjdk:11-jre-slim
COPY --from=intueri/framework:1.0.0 /home/gradle/src/orchestrator/build/distributions/orchestrator-boot-1.0-SNAPSHOT.tar /app/orchestrator.tar
WORKDIR /app
RUN tar -xvf orchestrator.tar
WORKDIR /app/orchestrator-boot-1.0-SNAPSHOT
CMD bin/orchestrator
