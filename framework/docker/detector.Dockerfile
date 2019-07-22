# Create slim Running container
FROM openjdk:11-jre-slim
COPY --from=intueri/framework:1.0.0 /home/gradle/src/detector/build/distributions/detector-boot-1.0-SNAPSHOT.tar /app/detector.tar
WORKDIR /app
RUN tar -xvf detector.tar
WORKDIR /app/detector-boot-1.0-SNAPSHOT
CMD bin/detector
