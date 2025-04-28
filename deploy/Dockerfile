FROM amazoncorretto:21-alpine-jdk

ARG JAR_FILE=deploy/libs/*.jar

ADD ${JAR_FILE} KorConverter.jar

ENTRYPOINT ["java","-jar","KorConverter.jar"]