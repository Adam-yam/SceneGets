# SceneGets

SCENE-FLIX(https://adam-yam.github.io/SCENE-FLIX/)의 차트 / 뉴스 / 스케줄 데이터를
갤럭시 홈 화면 위젯 3종으로 보여주는 안드로이드 앱.

## 위젯 구성

| 위젯 | 크기 | 자동 갱신 | 탭 동작 |
|---|---|---|---|
| 차트 | 4x2 | 1시간 | 없음 |
| 뉴스 | 4x2 | 1시간 | 기사 원문으로 이동 |
| 스케줄 | 4x2 | 6시간 | 없음 |

세 위젯 모두 헤더의 새로고침 아이콘으로 즉시 수동 갱신 가능.

## 데이터 소스

`SceneFlixConfig.kt`의 `BASE_URL` 하나로 관리됩니다.

```
https://adam-yam.github.io/SCENE-FLIX/data/
├── charts/chart.json
├── news.json
└── schedule/schedule_{year}_h{1|2}.json
```

## 스케줄 반기 파일 자동 전환

`data/SchedulePeriod.kt`가 오늘 날짜 기준으로 파일명을 **계산**합니다.
연도가 바뀌어도 (2026_h2 → 2027_h1 → 2027_h2 ...) 코드 수정 없이 자동으로 따라갑니다.

- 1~6월 → `h1`, 7~12월 → `h2`
- 현재 반기에 남은 예정 일정이 3개 미만이면 다음 반기 파일도 함께 조회해서 이어붙임
- 다음 반기 파일이 아직 발행 전이라 404가 나는 건 정상 상황으로 처리(에러 아님)

## 실패 처리 원칙

기존 위젯에서 겪었던 "네트워크 실패 시 조용히 null 반환 → 로딩 상태에서 멈춤" 문제를
반복하지 않도록, 모든 네트워크 호출은 `ApiResult`(Success/Error)로 명시적 결과를 반환합니다.

- 갱신 실패 시: 캐시된 마지막 데이터 + "갱신 실패 · N분 전" 문구를 표시 (빈 화면 방지)
- 캐시조차 없는 최초 실행 실패 시에만 완전한 에러 화면 표시

## 프로젝트 구조

```
app/src/main/java/com/adamyam/scenegets/
├── models/          # ChartResponse, NewsResponse, ScheduleResponse 등 데이터 클래스
├── network/         # HTTP 호출 (SceneFlixHttpClient, ChartApi, NewsApi, ScheduleApi)
├── data/            # Repository + DataStore 캐시 + SchedulePeriod 로직
├── work/            # WorkManager 자동/수동 갱신
└── widget/
    ├── common/      # 색상, 포맷팅, 플랫폼 정렬 등 공통 유틸
    ├── chart/       # 차트 위젯 (GlanceAppWidget + Composable UI)
    ├── news/        # 뉴스 위젯
    └── schedule/    # 스케줄 위젯
```

## 빌드 방법 (로컬)

1. Android Studio(Ladybug 이상 권장)에서 이 폴더를 프로젝트로 열기
2. Gradle Sync (인터넷 연결 필요 — google()/mavenCentral() 저장소 접근)
3. `app` 모듈 실행 → 에뮬레이터/실기기(API 26+)에 설치
4. 홈 화면 길게 눌러 "위젯" → SceneGets 차트/뉴스/스케줄 위젯 추가

compileSdk/targetSdk 35에 맞춰 AGP 8.6.1 / Gradle 8.7 wrapper로 구성되어 있습니다.

## 릴리즈 (GitHub Actions 수동 실행)

`.github/workflows/release.yml`이 **workflow_dispatch**(수동 실행) 트리거로 등록되어 있어,
GitHub 저장소의 Actions 탭 → "Manual Release" → "Run workflow"에서 버전을 입력하면:

1. 서명된 release APK를 빌드하고
2. `v{버전}` 태그로 GitHub Release를 생성하면서 APK를 첨부합니다.

### 최초 1회 설정 — 저장소 Secrets 등록

서명 키가 없으면 release APK가 기기에 설치되지 않으므로, 저장소 Settings → Secrets and
variables → Actions에 아래 4개 Secret을 등록해야 합니다. (전달받은 키스토어 파일/비밀번호 사용)

| Secret 이름 | 값 |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | 키스토어 파일을 base64로 인코딩한 문자열 |
| `RELEASE_KEYSTORE_PASSWORD` | 키스토어 비밀번호 |
| `RELEASE_KEY_ALIAS` | 키 별칭 |
| `RELEASE_KEY_PASSWORD` | 키 비밀번호 |

이 키스토어는 앱을 계속 같은 서명으로 릴리즈하기 위한 것이므로 **한 번 등록한 뒤로는 절대
분실/변경하지 마세요** — 잃어버리면 이후 릴리즈로 기존 설치본을 업데이트할 수 없고 삭제 후
재설치해야 합니다. 키스토어 파일 자체는 저장소에 커밋하지 않고 안전한 곳에 따로 보관하세요.

## 다음 단계로 고려할 것

- 위젯 설정 화면(예: 차트 위젯에서 보여줄 곡 개수 조절)
- 앨범 이미지(`albumImageUrl`) 위젯에 표시 (현재는 텍스트 정보만 우선 구현)
- 스케줄 타입별 필터(방송만 보기 등)를 위젯 설정으로 추가

## Share-to-Download

SceneGets can receive Android `ACTION_SEND`/`ACTION_VIEW` links and download supported media URLs into `Download/SceneGets/`.

- Share a video URL from another app and choose SceneGets.
- The app validates the shared URL before passing it to yt-dlp.
- Downloads run in a foreground service with progress notifications.
- Completed files are written to the public Downloads/SceneGets folder.
- The downloader uses `dev.ffmpegkit-maintained:yt-dlp-android:2.0.2` plus its compatibility API.
- The current configuration targets `arm64-v8a`, which matches modern Galaxy devices.

The embedded yt-dlp version is the version shipped by the selected Android library release; update the library dependency when a newer maintained release is required.
