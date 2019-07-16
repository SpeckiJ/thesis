#FROM azul/zulu-openjdk-alpine:11
#VOLUME /tmp
#ARG DEPENDENCY=target/dependency
#COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
#COPY ${DEPENDENCY}/META-INF /app/META-INF
#COPY ${DEPENDENCY}/BOOT-INF/classes /app
#ENTRYPOINT ["java","-cp","app:app/lib/*","IntueriDetectorApplication"]

# Build from sources
FROM gradle:jdk11 as builder
COPY . /home/gradle/src/
WORKDIR /home/gradle/src/
RUN gradle build --no-daemon -p detector

# Create slim Running container
FROM openjdk:11-jre-slim
COPY --from=builder /home/gradle/src/detector/build/distributions/detector-boot-1.0-SNAPSHOT.tar /app/detector.tar
WORKDIR /app
RUN tar -xvf detector.tar
WORKDIR /app/detector-boot-1.0-SNAPSHOT
CMD bin/detector
