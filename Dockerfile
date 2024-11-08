FROM openjdk:21-jdk

ARG JAR_FILE=korConverter/framework/boot/build/libs/boot-2.0.1.jar

ADD ${JAR_FILE} KorConverter.jar

ENTRYPOINT ["java","-jar","/KorConverter.jar"]