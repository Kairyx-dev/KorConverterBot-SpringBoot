# KorConverter bot

> 영타를 한글로 변환해주는 기존 KorConverter bot 의 Spring boot 버전입니다.
> 
> ex) dkssudgktpdy => 안녕하세요

## 프로젝트 구성

- jdk 25
- Spring boot 4.0.x

port 와 adapter 로 구성된 Hexagonal 아키텍처

- boot
- common
- hexagonal
    - adapter
        - bot -- 디스코드 봇과 연결되어 메시지를 수,발신을 담당
        - jpa -- database 연결 담당
    - application
        - inputPort -- adapter 로 부터 이벤트 수신
        - outputPort -- adapter 로 이벤트 발신
        - useCase -- inputPort 구현체
    - domain
        - 키보드 인덱스 변환을 담당하는 핵심로직

## 개발 환경 설정

clone 직후 1회 실행 (선택이지만 권장):

```bash
# 재포맷 커밋을 git blame 에서 건너뛴다
git config blame.ignoreRevsFile .git-blame-ignore-revs
```

### 코드 포맷팅

Spotless + [palantir-java-format](https://github.com/palantir/palantir-java-format)
(4-space 들여쓰기, 120 column). 근거는 `docs/decisions/0003-palantir-java-format.md` 참조.

```bash
./gradlew spotlessApply   # 포맷 적용
./gradlew spotlessCheck   # 검증 (lefthook pre-commit 이 자동 실행)
```

IntelliJ 에서 Cmd+Opt+L 을 쓰려면 **Palantir Java Format** 플러그인을 설치하고
`Settings > palantir-java-format` 에서 활성화한다. 미설치 시 IDE 포맷이 Spotless 결과와
어긋나 커밋 훅에서 되돌려진다. 최종 판정은 언제나 `./gradlew spotlessApply` 다.

## 프로젝트 실행

IDE 실행 방법

1. ./runtime/cfg/.env 파일 생성
    ```
    DB_HOST=
    DB_PORT=
    DB_NAME=
    DB_USER_NAME=
    DB_PASSWORD=
    DISCORD_BOT_TOKEN=
    ```
   Database 정보와 Discord Bot 의 토큰 지정

2. VM 옵션에 설정파일 위치 추가

```
-Dspring.config.location=./runtime/cfg/application.yml 
-Dlogging.config=./runtime/cfg/logback-spring.xml
```

Docker Container 생성 및 실행방법
> 현재 CI/CD 가 구성되어 registry 없이 docker file 을 tar 파일로 압축하여 바로 서버로 전송하기 때문에 아래 방법은 불가능

1. deploy 폴더의 `docker-compose.yml` 을 참고하여 컨테이너 생성
2. `docker-compose.yml` 파일을 위치시킨 디렉토리에 .env 파일 생성
    ```
    DB_HOST=
    DB_PORT=
    DB_NAME=
    DB_USER_NAME=
    DB_PASSWORD=
    DISCORD_BOT_TOKEN=
    ```
   Database 정보와 Discord Bot 의 토큰 지정
3. docker 명령어 실행
    ```terminaloutput
   docker compose up -d --build
   ```