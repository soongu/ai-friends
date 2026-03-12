# AI 이성친구 챗봇 - API 명세 (SPA)

> 본 문서는 [유즈케이스 시나리오](./app-use-case-scenarios.md)와 [도메인 모델](./domain-model-and-data-schema.md)을 바탕으로 **REST API**와 **SPA 진입점**만 정의합니다. 화면 URL은 설계하지 않으며, Thymeleaf는 초기 HTML 셸만 제공하고 **모든 화면 전환·렌더링은 JavaScript(CSR)로 처리**합니다.

---

## 1. 개요

- **아키텍처**: **SPA**. 단일 HTML 진입점 + Only JavaScript로 화면 구성. 서버는 **REST API만** 제공.
- **Thymeleaf**: 초기 1페이지만 제공. `GET /` → 빈 껍데기 HTML + `id="app"`(또는 root) + 스크립트 로드. 라우팅·이성친구 생성 단계·채팅·프로필은 모두 **클라이언트 라우팅/렌더링**.
- **인증**: 없음. 현재 선택 Soulmate는 **클라이언트 상태(메모리/로컬스토리지)** 또는 필요 시 세션으로만 보관.
- **서버 역할**: REST API (JSON 요청/응답). 화면 URL은 **설계하지 않음** — 브라우저는 항상 동일 진입점에서 JS가 화면을 전환.

---

## 2. SPA 진입점 (서버 측)

| HTTP | URL | 설명 |
|------|-----|------|
| GET | `/` | **단일 진입점**. Thymeleaf로 초기 HTML 반환. (root div + CSS/JS 링크. 실제 콘텐츠 없음 또는 로딩 UI만) |
| GET | `/static/**` | 정적 자원 (JS, CSS, 이미지). Spring `ResourceHandlers` 또는 `static/` 디렉터리 |

- **화면 URL 미설계**: `/create/step1`, `/chat/1`, `/profile/1` 같은 서버 라우트는 **두지 않음**. 주소창 경로를 쓰려면 **클라이언트 라우터**(History API / hash)로만 처리하고, 서버는 그 경로를 인식하지 않음.
- **컨트롤러**: `GET /` 하나만 HTML 반환. 나머지는 모두 `@RestController`로 JSON API.

---

## 3. REST API 목록

모든 데이터 조회·생성·갱신은 아래 API로만 수행. 프론트는 이 API만 호출하고 응답으로 DOM을 갱신한다.

| 구분 | HTTP | URL | 설명 |
|------|------|-----|------|
| 이성친구 생성 | POST | `/api/soulmates` | Soulmate 한 명 생성. Body에 gender, characterImageId, personalityKeywords, hobbies, speechStyles |
| 이성친구 목록 | GET | `/api/soulmates` | (선택) 만든 이성친구 목록. SPA에서 "누구와 대화할지" 선택 시 사용 |
| 이성친구 단건 | GET | `/api/soulmates/{id}` | 프로필·채팅 상단 표시용 상세 정보 (호감도, 레벨, 뱃지 포함) |
| 채팅 전송 | POST | `/api/chat` | 사용자 메시지 전송 → AI 응답 + 호감도·레벨·뱃지 반환 |
| 대화 기록 | GET | `/api/soulmates/{id}/chat/logs` | 해당 Soulmate와의 대화 목록 (시간순). 채팅 화면 진입 시 또는 스크롤 시 |

---

## 4. API 상세

### 4.1 POST /api/soulmates — 이성친구 생성

**Request**

- **Content-Type**: `application/json`
- **Body**:
```json
{
  "gender": "FEMALE",
  "characterImageId": "char_02",
  "characterImageUrl": null,
  "name": null,
  "personalityKeywords": ["친절한", "유머러스한"],
  "hobbies": ["영화", "음악"],
  "speechStyles": ["반말", "캐주얼"]
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| gender | String | O | 예: `MALE`, `FEMALE` |
| characterImageId | String | O | 캐릭터 선택지 코드 |
| characterImageUrl | String | X | 없으면 서버에서 ID로 매핑 |
| name | String | X | 표시 이름 |
| personalityKeywords | String[] | O | 1개 이상 |
| hobbies | String[] | O | 1개 이상 |
| speechStyles | String[] | O | 1개 이상 |

**Response (201 Created)**

- **Location**: ` /api/soulmates/{id}`
- **Body**: 생성된 Soulmate 리소스 (아래 4.2 응답과 동일 구조 또는 `{ "id": 1 }` 등 최소 정보).

**에러**: 400 (검증 실패 시 필드별 메시지).

---

### 4.2 GET /api/soulmates/{id} — 이성친구 단건 (프로필 정보 포함)

**Response (200 OK)**

```json
{
  "id": 1,
  "gender": "FEMALE",
  "characterImageId": "char_02",
  "characterImageUrl": "/static/images/char_02.png",
  "name": null,
  "personalityKeywords": ["친절한", "유머러스한"],
  "hobbies": ["영화", "음악"],
  "speechStyles": ["반말", "캐주얼"],
  "affectionScore": 50,
  "level": 2,
  "badges": ["FIRST_CHAT", "CHAT_10"],
  "createdAt": "2025-03-11T10:00:00"
}
```

- 채팅 상단·프로필 화면에서 이 API로 호감도·레벨·뱃지를 갱신하면 됨.

**에러**: 404 (없는 id).

---

### 4.3 GET /api/soulmates — 이성친구 목록 (선택)

**Response (200 OK)**

```json
{
  "soulmates": [
    { "id": 1, "characterImageUrl": "...", "name": null, "level": 2, "affectionScore": 50 }
  ]
}
```

- SPA에서 "이성친구 선택" 또는 "만든 목록"이 필요할 때만 구현.

---

### 4.4 POST /api/chat — AI 채팅 (S2S 핵심)

**Request**

- **Content-Type**: `application/json`
- **Body**:
```json
{
  "soulmateId": 1,
  "userMessage": "안녕, 오늘 날씨 어때?"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| soulmateId | Long | O | 대화할 Soulmate ID |
| userMessage | String | O | 사용자 발화 내용 |

**Response (200 OK)**

```json
{
  "userMessage": "안녕, 오늘 날씨 어때?",
  "aiMessage": "안녕! 오늘 날씨 좋다~ 뭐 할까?",
  "choices": ["영화 보자", "산책하자", "그냥 수다 떨자"],
  "soulmateId": 1,
  "affectionScore": 60,
  "level": 2,
  "newBadges": ["FIRST_CHAT"]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| userMessage | String | 에코 (선택) |
| aiMessage | String | AI 응답 텍스트 |
| choices | String[] | **미연시 스타일 선택지**. AI가 이번 턴에서 제시한 다지선다 옵션. 없으면 `[]`. 클라이언트는 버튼 등으로 노출 후, 사용자가 고르면 **그 문자열을 그대로 다음 요청의 userMessage로 전송** |
| soulmateId | Long | 동일 |
| affectionScore | Integer | 갱신된 호감도 (AI 응답에 따라 동적으로 증감) |
| level | Integer | 갱신된 레벨 |
| newBadges | String[] | 이번 턴에 새로 획득한 뱃지 (없으면 `[]`) |

**에러**

| HTTP | 상황 | Body 예시 |
|------|------|-----------|
| 404 | Soulmate 없음 | `{"error":"NOT_FOUND","message":"이성친구를 찾을 수 없습니다"}` |
| 400 | userMessage 비어 있음 | `{"error":"BAD_REQUEST","message":"메시지를 입력해 주세요"}` |
| 502 | Gemini API 실패 | `{"error":"AI_UNAVAILABLE","message":"AI가 일시적으로 응답하지 않습니다"}` |
| 429 | Rate Limit | `{"error":"RATE_LIMIT","message":"잠시 후 다시 시도해 주세요"}` |

---

### 4.5 GET /api/soulmates/{id}/chat/logs — 대화 기록

**Query (선택)**  
- `page`, `size`: 페이징. 기본값 예: 0, 50.  
- 또는 `limit`, `before`(커서) 등.

**Response (200 OK)**

```json
{
  "logs": [
    { "speaker": "USER", "message": "안녕", "createdAt": "2025-03-11T10:01:00" },
    { "speaker": "AI", "message": "안녕! ...", "createdAt": "2025-03-11T10:01:02" }
  ]
}
```

- 채팅 화면 진입 시 이 API로 과거 대화를 불러와 JS로 말풍선 렌더링.

---

## 5. 클라이언트(SPA) 측 정리

- **라우팅**: 서버 URL은 `/` 하나만. 화면 구분(메인 / 생성 step1·2·3 / 채팅 / 프로필)은 **전부 JS**에서 상태 또는 History API(hash/ path)로 처리.
- **이성친구 생성**: Step1(성별) → Step2(캐릭터) → Step3(성격·취미·말투)는 **클라이언트 상태**로만 유지. 최종 "생성" 버튼 시 `POST /api/soulmates` 한 번만 호출.
- **채팅**: `POST /api/chat` 호출 후 응답의 `aiMessage`, `affectionScore`, `level`, `newBadges`로 DOM 갱신. **choices**가 있으면 미연시처럼 버튼으로 노출하고, 사용자가 누르면 그 문자열을 **다음 요청의 userMessage**로 보내면 됨. 목록은 `GET /api/soulmates/{id}/chat/logs`로 로드.
- **프로필**: `GET /api/soulmates/{id}`로 호감도·레벨·뱃지 표시.

---

## 6. 서버 측 구현 요약

| 담당 | HTTP | URL | 비고 |
|------|------|-----|------|
| 단일 HTML | GET | `/` | Thymeleaf 1페이지만. 나머지 없음 |
| SoulmateController (또는 ApiController) | POST | `/api/soulmates` | 생성 |
| | GET | `/api/soulmates` | 목록 (선택) |
| | GET | `/api/soulmates/{id}` | 단건(프로필) |
| | GET | `/api/soulmates/{id}/chat/logs` | 대화 기록 |
| AiChatController | POST | `/api/chat` | AI 채팅 (S2S) |

- **뷰(Thymeleaf)**: `index.html` 하나. 별도 `chat.html`, `profile.html` 등 **화면용 URL·템플릿 없음**.

---

## 7. 문서 이력

| 버전 | 일자 | 변경 내용 |
|------|------|-----------|
| 0.1 | 2025-03-11 | 최초 작성 (유즈케이스·도메인 반영) |
| 0.2 | 2025-03-11 | SPA로 전환. 화면 URL 제거, REST API만 명세, Thymeleaf는 진입점 HTML만 |
| 0.3 | 2025-03-11 | POST /api/chat 응답에 choices(미연시 스타일 선택지) 필드 추가 |
| 0.4 | 2025-03-12 | AI 기반 동적 호감도 증감 로직 반영 및 Gemini 연동 JSON 응답 파싱 명시 |
