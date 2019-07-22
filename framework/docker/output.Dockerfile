#FROM azul/zulu-openjdk-alpine:11
#VOLUME /tmp
#ARG DEPENDENCY=target/dependency
#COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
#COPY ${DEPENDENCY}/META-INF /app/META-INF
#COPY ${DEPENDENCY}/BOOT-INF/classes /app
#ENTRYPOINT ["java","-cp","app:app/lib/*","IntueriDetectorApplication"]

# Create slim Running container
FROM openjdk:11-jre-slim
COPY --from=intueri/framework:1.0.0 /home/gradle/src/output/build/distributions/output-boot-1.0-SNAPSHOT.tar /app/output.tar
WORKDIR /app
RUN tar -xvf output.tar
WORKDIR /app/output-boot-1.0-SNAPSHOT
CMD bin/output
