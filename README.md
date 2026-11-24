# 헌혈플러스

## 파일 루트

프로젝트 루트는 이 README가 있는 `bloodplus-web/` 폴더입니다. 아래 경로는 모두 프로젝트 루트를 기준으로 표시합니다.

```text
bloodplus-web/
├─ build.gradle                              라이브러리·빌드 설정
├─ src/main/java/bloodplus/
│  ├─ BloodplusApplication.java              실행 시작 파일
│  ├─ controller/                            웹 요청 처리
│  ├─ service/                               기능 처리
│  ├─ repository/                            데이터 저장·SQL 실행
│  └─ domain/                                데이터 객체
├─ src/main/resources/
│  ├─ application.properties                 서버·DB 설정
│  ├─ templates/                             HTML 화면
│  └─ static/css/app.css                     화면 디자인
├─ gradlew.bat                               Windows Gradle 실행
└─ run-windows.cmd                           Windows 서버 실행
```

## 실행 방법

### IntelliJ

1. **File > Open**에서 위 프로젝트 루트 폴더를 엽니다.
2. 프로젝트 SDK와 Gradle JVM을 **JDK 17**로 설정하고 Gradle 동기화가 끝날 때까지 기다립니다.
3. `src/main/java/bloodplus/BloodplusApplication.java`를 열고 `main()` 옆 **▶**를 누릅니다. 또는 실행 구성 **Bloodplus (memory)**를 선택해 실행합니다.
4. 콘솔에 `Tomcat started on port 8080`이 표시되면 [http://localhost:8080](http://localhost:8080)에 접속합니다.
5. 시연 계정 **user01 / 1234**로 로그인합니다. `user01`~`user20`을 사용할 수 있습니다.
6. 종료할 때는 IntelliJ의 **■ 중지** 버튼을 누릅니다.

### Windows에서 바로 실행

프로젝트 루트의 `run-windows.cmd`를 더블클릭합니다. 서버가 시작되면 [http://localhost:8080](http://localhost:8080)에 접속합니다. 종료는 실행 창에서 **Ctrl+C**를 누릅니다.

현재는 Oracle 없이 실행되는 **memory 모드**입니다. 서버를 재시작하면 메모리의 변경 데이터는 초기화됩니다.

