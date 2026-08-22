# LockTodoApp — APK 빌드 안내

## 왜 여기서 .apk 파일 자체를 드릴 수 없나요?
이 코드 실행 환경은 외부 네트워크가 화이트리스트로 제한되어 있어서
`dl.google.com`(Android SDK / Google Maven 저장소), `services.gradle.org`
(Gradle 배포판) 같은 빌드에 꼭 필요한 호스트에 접근할 수 없습니다.
(`curl https://dl.google.com` → `host_not_allowed`로 즉시 차단됨)
즉 Android SDK 자체가 설치되어 있지 않고 받아올 수도 없어서, 컴파일
가능한 `.apk` 바이너리를 이 자리에서 생성하는 것은 물리적으로
불가능합니다.

대신 **Android Studio에서 열자마자 바로 빌드되도록** 프로젝트 설정
파일(`settings.gradle.kts`, 루트 `build.gradle.kts`, 테마, 런처
아이콘 등)을 모두 채워 넣었습니다. 아래 두 가지 방법 중 하나로
1~2분 안에 apk를 뽑을 수 있습니다.

## 방법 A — Android Studio (가장 쉬움, 권장)
1. `LockTodoApp.zip`의 압축을 풀고 **Android Studio → Open** 으로
   `LockTodoApp` 폴더를 엽니다.
2. Gradle Wrapper가 없다는 안내가 뜨면 **"Use Gradle from: (기본값/번들)"**
   를 선택하고 계속 진행합니다 (Android Studio가 자동으로 wrapper를
   만들어줍니다).
3. Gradle Sync가 끝나면 상단 메뉴에서
   **Build → Build Bundle(s) / APK(s) → Build APK(s)** 클릭.
4. 빌드가 끝나면 우측 하단 알림의 **"locate"** 를 눌러
   `app/build/outputs/apk/debug/app-debug.apk` 를 확인합니다.

## 방법 B — 커맨드라인 (Android SDK가 이미 설치된 로컬 PC/Mac)
```bash
cd LockTodoApp
# wrapper가 없다면 로컬에 설치된 gradle로 1회 생성
gradle wrapper --gradle-version 8.7

./gradlew assembleDebug
# 결과물: app/build/outputs/apk/debug/app-debug.apk
```
`ANDROID_HOME` 환경변수가 SDK 경로를 가리키고 있어야 합니다
(Android Studio를 한 번이라도 설치했다면 보통 자동으로 잡혀 있습니다).

## 서명된 릴리스 APK가 필요하다면
디버그 apk는 테스트용 서명이라 기기에 바로 설치는 되지만 배포는
안 됩니다. 배포용이 필요하면 Android Studio의
**Build → Generate Signed Bundle / APK** 로 keystore를 만들고
release 빌드를 진행하세요.

## 실제 기기 테스트 시 꼭 확인할 것
- 설정 파일에는 없지만, Android 13+에서는 알림 권한(`POST_NOTIFICATIONS`)을
  런타임에 별도로 요청해야 포그라운드 서비스 알림이 정상 표시됩니다.
- 접근성 서비스 / 오버레이 권한은 **MainActivity**에서 안내하는 설정
  화면으로 이동해 수동으로 켜야 합니다 (Play 스토어 정책상 자동 활성화
  불가).
- 삼성/샤오미 등 일부 제조사는 배터리 최적화 예외 처리를 해주지 않으면
  접근성 서비스가 백그라운드에서 종료될 수 있습니다.
