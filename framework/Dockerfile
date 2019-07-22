FROM gradle:jdk11 as builder
COPY . /home/gradle/src/
WORKDIR /home/gradle/src/
RUN gradle build --no-daemon
