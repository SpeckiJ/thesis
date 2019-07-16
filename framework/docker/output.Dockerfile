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
RUN gradle build --no-daemon -p output

# Create slim Running container
FROM openjdk:11-jre-slim
COPY --from=builder /home/gradle/src/output/build/distributions/output-boot-1.0-SNAPSHOT.tar /app/output.tar
WORKDIR /app
RUN tar -xvf output.tar
WORKDIR /app/output-boot-1.0-SNAPSHOT
CMD bin/output
