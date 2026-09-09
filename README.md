<div align=center>
<img width="1254" height="1254" alt="icon" src="https://github.com/user-attachments/assets/96d5e99f-abf5-4241-8a2f-a31cc816e22a" />
</div>

# SceneGets

RESCENE 팬을 위한 안드로이드 앱. 차트 · 뉴스 · 스케줄 정보를 한 곳에서 확인하고, 홈 화면 위젯으로도 받아볼 수 있습니다.

- 패키지명: `com.adamyam.scenegets`
- 최소 지원: Android 8.0 (API 26) / 타겟: Android 15 (API 35)

## 주요 기능

- **홈 / 차트 / 뉴스 / 스케줄 / 설정** 5개 탭 구성의 인앱 화면
- 차트 탭: 멜론, 유튜브 뮤직, 스포티파이, 지니, 플로, 벅스, 바이브 등 플랫폼별 차트 확인
- 스케줄 탭: 활동 일정 확인 및 일정별 알림 설정 (지정 시간 전 알림)
- 당겨서 새로고침 (pull-to-refresh)
- 라이트 / 다크 / 시스템 설정 테마 지원
- 홈 화면 위젯 3종 (차트 / 뉴스 / 스케줄) — 위젯별 배경 투명도 · 테마 개별 설정 가능
- 백그라운드 자동 동기화 (WorkManager), 배터리 최적화 예외 안내
- 이미지 캐시 및 다운샘플링으로 데이터 사용량 절감

## 기술 스택

- Kotlin, Android WebView (앱 UI는 단일 HTML/JS 자산을 WebView로 로드)
- Jetpack Glance (홈 화면 위젯 UI)
- WorkManager, DataStore Preferences
- OkHttp, kotlinx.serialization (JSON)

## 프로젝트 구조

```
app/src/main/
├── assets/scenegets_app_design.html   # 인앱 UI (탭 화면 전체)
├── java/com/adamyam/scenegets/
│   ├── MainActivity.kt                # WebView 호스팅 + JS 브릿지(SceneGetsBridge)
│   ├── data/                          # 리포지토리, 캐시, 위젯 상태
│   ├── network/                       # API 클라이언트, 엔드포인트 설정
│   ├── notify/                        # 일정 알림
│   ├── work/                          # 백그라운드 동기화 워커
│   └── widget/                        # 차트/뉴스/스케줄 위젯 + 설정 화면
└── res/                                # 아이콘, 위젯 미리보기, 리소스
```

네이티브(Kotlin)와 웹(HTML/JS)은 `SceneGetsBridge`라는 JavaScript 인터페이스로 통신합니다. 차트·뉴스·스케줄 원본 데이터는 아래 엔드포인트에서 가져옵니다.

```
https://adam-yam.github.io/SCENE-FLIX/data/
```

## 라이선스

비공식 팬 프로젝트입니다. RESCENE 및 관련 소속사와는 무관합니다.
