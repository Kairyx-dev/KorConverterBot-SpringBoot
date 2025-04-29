FROM amazoncorretto:21-alpine-jdk
LABEL devKshull="devkshull@gmail.com"

ARG JAR_FILE=deploy/libs/*.jar
ARG JVM_OPTION="-Dspring.config.location=cfg/application.yml -Dlogging.config=cfg/logback-spring.xml"

ADD ${JAR_FILE} app/KorConverter.jar

WORKDIR /app

ENTRYPOINT exec java ${JVM_OPTION} -jar KorConverter.jar