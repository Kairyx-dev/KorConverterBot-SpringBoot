# Spring boot Hexagonal template
> spring boot 프로젝트를 시작할 때 빠르게 hexagonal architecture 를 구성할 수 있는 템플릿 입니다.


## Package Structure

- framework
  - `boot` 전체 spring boot 를 실행하기 위한 진입점입니다.
- hexagonal
  - adapter
    - `rest` http rest api 관련 동작을 하는 adapter package 입니다.
    - `jpa` db 관련 작업을 하는 adapter package 입니다.
  - `application` 어뎁터들과 통신하기 위한 포트들을 정의하는 package 입니다.
  - `domain` 순수 java 코드로 구현되는 비즈니스 로직이 위치하는 package 입니다.



## Sample code
> 해당 프로젝트는 샘플코드를 담고있으며 바로 실행 가능합니다

Sample code 는 rest api 로 내부 db에 데이터를 저장하기까지 구현되어있습니다.

[Swagger link](http://localhost:8080/swagger-ui/index.html)


## Library
해당 프로젝트는 템플릿 이기 때문에 기초적인 라이브러리만 포함합니다.

* jdk version - 21
* spring boot - 3.3.5

> 멀티모듈로 구현된 Hexagonal architecture 이기 때문에 여러 build.gradle 파일을 쉽게 관리하기 위하여
> Line corp 의 [Build Recipe plugin](https://github.com/line/gradle-multi-project-support/tree/master/build-recipe-plugin) 이 사용되었습니다.


## Guide

해당 프로젝트의 루트 패키지 명은 `org.specter.template` 이며 원하는 패키지 명으로 수정하여 사용하시면 됩니다.

루트 폴더 인 `template`폴더 이름 변경 시 `setting.gradle.kts` 파일의 모듈 루트를 수정하셔야합니다.