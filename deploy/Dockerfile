FROM amazoncorretto:21-alpine-jdk
LABEL devKshull="devkshull@gmail.com"

ARG JAR_FILE=deploy/libs/*.jar

ADD ${JAR_FILE} app/KorConverter.jar

WORKDIR /app

ENTRYPOINT ["java", "-Dspring.config.location=file:/app/cfg/application.yml", "-Dlogging.config=file:/app/cfg/logback-spring.xml","-jar", "KorConverter.jar"]