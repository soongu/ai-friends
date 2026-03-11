# AI 이성친구 챗봇 - S2S 라이브코딩 예제 프로젝트 기획문서

## 1. 프로젝트 개요

### 1.1 컨셉
**커스터마이징 AI 이성친구 챗봇** — 최근 유행하는 AI 연인/친구 앱을 모티브로 한 웹 애플리케이션입니다. 사용자가 이성친구의 성격, 말투, 취미를 설정하면, 그에 맞춘 AI가 대화를 이어갑니다. **게이미피케이션** 요소(레벨, 호감도, 업적)를 넣어 학생들의 몰입도를 높이고, S2S 통신 학습에 집중할 수 있는 흥미로운 주제로 설계합니다.

### 1.2 목적
- **S2S(서버 to 서버) 통신**을 3시간 빈칸채우기 라이브코딩으로 확실히 이해시키기
- 대부분의 로직은 **미리 구현된 상태**로 배포하고, 핵심 S2S 구간만 학생이 직접 채워 넣기
- 인증/인가는 생략해 **HTTP 클라이언트와 외부 API 연동**에만 집중

---

## 2. 기술 스택

| 구분 | 기술 |
|------|------|
| 백엔드 | Spring Boot 4, Java 25 |
| 데이터베이스 | H2 (인메모리, 개발/실습용) |
| 프론트엔드 | Thymeleaf (서버 사이드 렌더링, UI 확실히 꾸밈) |
| 외부 API | Google Gemini API (Spring AI 미사용, 직접 연동) |
| HTTP 클라이언트 | RestClient (동기식), WebClient (소개만) |
| 인증/인가 | **생략** |

---

## 3. 핵심 기능 및 게이미피케이션

### 3.1 사용자 시나리오
1. **이성친구 생성**: 이름, 성격 키워드(친절한, 유머러스한, 차분한 등), 취미, 말투 스타일을 설정
2. **대화**: 설정된 페르소나에 맞춰 AI가 응답. 우리 백엔드가 클라이언트 요청을 받아 **Gemini API**와 S2S 통신
3. **호감도**: 대화 횟수·품질에 따라 호감도 누적
4. **레벨/업적**: 레벨업, 뱃지(첫 대화, 10회 대화, 100회 대화 등)로 성취감 부여

### 3.2 게이미피케이션 요소
| 요소 | 설명 |
|------|------|
| 호감도 | 대화할 때마다 +N, 특정 키워드/이벤트 시 보너스 |
| 레벨 | 호감도 누적에 따른 레벨업 (Lv.1 ~ Lv.10) |
| 업적/뱃지 | "첫 인사", "10번째 대화", "밤샘 대화왕" 등 |
| 대화 기록 | H2에 저장, 이전 대화 컨텍스트로 자연스러운 연속 대화 |

---

## 4. S2S 학습 포인트와 빈칸채우기 전략

### 4.1 S2S 흐름 (학습 목표)
```
[브라우저] → [우리 Spring Boot 서버] → [Google Gemini API 서버] → [우리 서버] → [브라우저]
     ↑                    ↑                        ↑
  Thymeleaf          RestClient              JSON 요청/응답
   (프론트)           (S2S 핵심)              (직접 조합)
```

### 4.2 미리 구현할 부분 (Pre-built)
- 프로젝트 셋업, 의존성, H2 설정
- Thymeleaf 페이지: 이성친구 생성 폼, 채팅 UI, 호감도/레벨 표시
- 도메인 모델: `Soulmate`, `ChatLog`, `UserProfile` (인증 없이 세션/쿠키 또는 단일 사용자 가정)
- 컨트롤러: 페이지 라우팅, 폼 바인딩
- DTO: `GeminiRequest`, `GeminiResponse` (구조만 정의, 실제 호출부는 빈칸)
- `RestClient` 빈 설정 (URL·헤더 틀만, 실제 `exchange` 호출부는 빈칸)
- 에러 처리, 로깅 구조

### 4.3 학생이 채울 빈칸 (Live Coding)
| 순서 | 위치 | 내용 | 학습 포인트 |
|------|------|------|-------------|
| 1 | `GeminiService.callGemini()` | `RestClient`로 Gemini API `POST` 요청 | S2S 요청 전송 |
| 2 | `GeminiService` | 요청 DTO → Gemini JSON (`contents`, `parts`) 조합 | 요청 가공 |
| 3 | `GeminiService` | 응답 JSON 파싱 → 우리 DTO 변환 | 응답 가공 |
| 4 | `AiChatController` | 채팅 요청 시 `GeminiService` 호출 후 Thymeleaf에 결과 전달 | 프록시 API 완성 |
| 5 | (선택) | 4xx/5xx 처리, 타임아웃 설정 | 실패 시나리오 대응 |

---

## 5. 프로젝트 구조 (제안)

```
src/main/java/com/example/aisoulmate/
├── config/
│   └── RestClientConfig.java      # RestClient 빈 (기본 URL, 헤더 - 빈칸: exchange 로직 X)
├── controller/
│   ├── HomeController.java       # 메인, 이성친구 생성 폼
│   ├── ChatController.java       # 채팅 페이지, 대화 기록
│   └── AiChatController.java     # AI 응답 API (빈칸: GeminiService 호출부)
├── service/
│   ├── SoulmateService.java      # 이성친구 CRUD (완성)
│   ├── ChatLogService.java       # 대화 기록 저장 (완성)
│   └── GeminiService.java        # [빈칸] S2S 핵심 - RestClient 호출, JSON 조합/파싱
├── dto/
│   ├── GeminiRequestDto.java     # 우리 앱 → Gemini 요청 (완성)
│   ├── GeminiResponseDto.java    # Gemini → 우리 앱 응답 (완성)
│   └── ChatRequestDto.java       # 클라이언트 채팅 요청 (완성)
├── domain/
│   ├── Soulmate.java
│   ├── ChatLog.java
│   └── UserProfile.java
└── repository/
    ├── SoulmateRepository.java
    └── ChatLogRepository.java

src/main/resources/
├── templates/
│   ├── index.html                # 메인, 이성친구 생성
│   ├── chat.html                 # 채팅 UI (Thymeleaf, 꾸밈)
│   └── profile.html              # 호감도, 레벨, 뱃지
├── static/
│   ├── css/
│   └── js/
└── application.yml
```

---

## 6. Thymeleaf UI 컨셉

### 6.1 페이지 구성
- **메인(index)**: 이성친구 생성 폼 — 이름, 성격 키워드, 취미, 말투 선택
- **채팅(chat)**: 말풍선 형태 채팅 UI, 사용자/AI 구분, 호감도·레벨 상단 표시
- **프로필(profile)**: 레벨 바, 뱃지 목록, 대화 통계

### 6.2 디자인 톤
- 따뜻하고 친근한 색감 (파스텔, 그라데이션)
- 모바일 퍼스트, 반응형
- 게이미피케이션 요소 시각화 (레벨 바, 뱃지 아이콘)

---

## 7. 3시간 라이브코딩 세션과의 연계

| 시간대 | 강의 내용 (lecture-plan-overview-day1.md) | 예제 프로젝트 연동 |
|--------|------------------------------------------|---------------------|
| 0~30분 | S2S 개념, 포트원 과제 연결 | "우리 앱은 사용자 대신 Gemini 서버를 부른다" 흐름 설명 |
| 30~75분 | RestClient 세팅, 외부 API 호출 | `GeminiService` 빈칸 채우기 |
| 75~85분 | 휴식 | - |
| 85~100분 | WebClient 소개 | 개념만, 구현 생략 |
| 100~165분 | Gemini 프록시 라이브코딩 | `AiChatController` + `GeminiService` 완성, 채팅 동작 확인 |
| 165~180분 | 회고 | "포트원 결제도 같은 패턴" 정리 |

---

## 8. 환경 변수 및 사전 준비

- `GEMINI_API_KEY`: Google AI Studio에서 발급, `.env` 또는 `application.yml` 플레이스홀더로 주입
- `.env.example`에 `GEMINI_API_KEY=` 템플릿 제공

---

## 9. 기대 효과

- **흥미 유발**: AI 이성친구 + 게이미피케이션으로 수업 참여도 상승
- **S2S 집중**: 인증/인가 없이 HTTP 클라이언트와 외부 API 연동만 연습
- **빈칸채우기**: 전체를 처음부터 짜지 않고, 핵심 구간만 채워 넣어 3시간 내 완성 가능
- **실무 연결**: 포트원 결제 API 연동 과제와 동일한 "우리 서버 ↔ 외부 API" 패턴 학습
