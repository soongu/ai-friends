# AI 이성친구 챗봇 - Gemini API 연동 스펙

> 본 문서는 **우리 서버 ↔ Google Gemini API** S2S 연동 시 요청/응답 형식, 페르소나 조합, 에러 처리**를 정의합니다. `GeminiService` 및 RestClient 구현 시 기준으로 사용합니다. (Spring AI 미사용, 직접 HTTP 호출)

---

## 1. 개요

- **역할**: 클라이언트가 보낸 사용자 메시지 + Soulmate 페르소나(성별·성격·취미·말투)를 조합해 Gemini에 요청하고, 응답 텍스트를 추출해 우리 API 응답으로 반환.
- **클라이언트**: 우리 백엔드의 `GeminiService` (RestClient 사용).
- **인증**: API 키. **절대 프론트에 노출하지 않고** 서버에서만 사용.

---

## 2. 엔드포인트 및 공통 설정

### 2.1 URL

```
POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
```

- **{model}**: 사용할 모델명. 예: `gemini-2.0-flash`, `gemini-1.5-flash`, `gemini-1.5-pro` 등.  
  - 전체 URL 예: `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent`
- **베이스 URL**: RestClient 빈 설정 시 `https://generativelanguage.googleapis.com/v1beta` 로 두고, 경로는 `models/gemini-2.0-flash:generateContent` 로 조합해도 됨.

### 2.2 헤더

| 이름 | 값 |
|------|-----|
| Content-Type | application/json |
| x-goog-api-key | {GEMINI_API_KEY} (환경 변수 또는 application.yml에서 주입) |

### 2.3 타임아웃

- 연결·읽기 타임아웃 권장: 각 10~30초. 장시간 대기 시 사용자 경험 저하.

---

## 3. 요청 본문 (Request Body)

### 3.1 Gemini가 기대하는 JSON 구조

- **contents** (필수): 대화 맥락 + 이번 턴 사용자 메시지.
- **systemInstruction** (선택, 모델 지원 시): “너는 ~한 이성친구다” 같은 **페르소나 지시**. 지원 모델에서 사용 권장.
- **generationConfig** (선택): temperature, maxOutputTokens 등.

### 3.2 contents 구조

- **role**: `"user"` 또는 `"model"`. 우리가 보내는 건 사용자 발화이므로 `"user"`.
- **parts**: `[ { "text": "실제 텍스트" } ]` 형태. 텍스트만 보낼 때는 parts 하나에 text만 넣으면 됨.

**단일 턴 (컨텍스트 없이 한 마디만)**

```json
{
  "contents": [
    {
      "role": "user",
      "parts": [{ "text": "안녕, 오늘 날씨 어때?" }]
    }
  ]
}
```

**멀티 턴 (이전 대화 + 이번 사용자 메시지)**

- 대화 기록을 시간순으로 `contents` 배열에 넣는다.  
  예: `[ { "role": "user", "parts": [{ "text": "안녕" }] }, { "role": "model", "parts": [{ "text": "안녕! ..." }] }, { "role": "user", "parts": [{ "text": "오늘 날씨 어때?" }] } ]`
- Gemini는 마지막 사용자 메시지에 대해 답변 생성.
- 맥락 유지 정책(섹션 3.6)에 따라 **몇 턴까지** 넣을지, **토큰 초과 시** 어떻게 자를지 구현 필요.

### 3.3 systemInstruction — 페르소나 및 행동 지침 (우리 앱 핵심)

Soulmate의 **성별, 성격, 취미, 말투**를 한글로 지시문에 넣는다. 우리 서버에서 문자열로 조합.

**조합 예시 (Soulmate 엔티티 기준)**

- gender → "남성" / "여성"
- personalityKeywords → "성격: 친절하고, 유머러스하고, 차분한."
- hobbies → "취미: 영화 감상, 음악."
- speechStyles → "말투: 반말, 캐주얼하게."

**systemInstruction 예시 (페르소나 + 주제 전환 대응 + JSON 응답 지시)**

```
당신은 사용자의 이성친구 캐릭터입니다.
- 성별: 여성
- 성격: 친절하고, 유머러스하고, 차분한.
- 취미: 영화 감상, 음악.
- 말투: 반말을 쓰고, 캐주얼하게 대합니다.

[행동 지침]
- 위 설정에 맞춰 짧고 친근하게 답하세요. 이성친구 말투를 항상 유지하세요.
- 사용자가 대화 도중 갑자기 전혀 다른 주제(예: 음식 이야기하다가 AWS/기술 질문, 일상에서 직장/공부 화제로 전환 등)를 꺼내도 당황하지 마세요.
- 주제가 뜬금없이 바뀌어도 자연스럽게 받아들여 대응하세요.
- 캐릭터를 벗어나지 마세요. "저는 AI라서" 같은 네 번째 벽을 깨는 말은 하지 마세요.

[응답 형식 지침 (필수 규칙)]
당신의 응답은 반드시 아래 JSON 규격을 정확히 준수해야 합니다. 마크다운 코드 블록(```json ... ```)이나 불필요한 텍스트 없이, 순수한 JSON 객체 문자열만 반환하세요.

{
  "aiMessage": "사용자에게 할 대답 (이성친구로서의 친근한 대화)",
  "choices": ["영화 보자", "산책하자", "그냥 수다나 떨자"],
  "affectionDelta": 1
}

- "aiMessage": 화면에 실제 보여질 당신의 텍스트 답변.
- "choices": 가끔(예: 대화가 자연스럽게 물어보는 흐름일 때) 사용자에게 제시할 다지선다 옵션 배열(2~4개). 선택지가 필요 없으면 빈 배열 `[]`.
- "affectionDelta": 방금 사용자의 대화가 당신의 설정된 성격과 취향에 얼마나 잘 맞는지 평가하여 호감도 증감치(-5 ~ +5 정수)를 결정.
```

- **캐릭터 이미지**는 텍스트 생성에는 영향 없으므로, 필요 시 "외모/이미지는 ~한 느낌" 정도만 선택적으로 추가 가능.

#### 갑작스런 주제 전환 대응 (프롬프트 설계 요약)

| 상황 | 지시 요지 |
|------|-----------|
| 뜬금없는 주제 전환 | 당황하지 말고 자연스럽게 받아들인다. "갑자기 그런 이야기하다니 재밌다" 등 반응 가능. |
| 본인이 잘 모르는 내용 | 이성친구 말투 유지하며 "나도 잘 모르는데~", "같이 알아보자" 등으로 응답. |
| 캐릭터 일관성 | 어떤 주제든 말투·성격(친절/유머/차분)은 유지. 주제만 바뀔 뿐 대화 상대는 그대로. |
| 네 번째 벽 | AI/챗봇임을 언급하지 않음. 항상 "이성친구" 한 명으로만 답변. |

#### JSON 출력 형식 및 파싱 (백엔드 구현)

- **Gemini 출력 규칙**: 응답은 순수 JSON 포맷의 문자열로 반환된다.
- **파싱 순서** (Gemini 응답 텍스트 한 번에 처리):
  1. 모델이 마크다운 블록(```json ... ```)을 포함할 수 있으므로 이를 앞뒤로 제거(`trim`, `replace`).
  2. Spring의 `ObjectMapper`를 사용하여 생성된 JSON 문자열을 `GeminiParsedResponse` (또는 임시 DTO)로 바로 매핑.
- **예시**:  
  - 원문: `{"aiMessage": "오늘 날씨 좋다~ 뭐 할까?", "choices": ["영화 보자", "산책하자", "그냥 수다 떨자"], "affectionDelta": 2}` 
  - → aiMessage = `"오늘 날씨 좋다~ 뭐 할까?"`  
  - → choices = `["영화 보자", "산책하자", "그냥 수다 떨자"]`
  - → affectionDelta = `2`

### 3.4 generationConfig (선택)

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| temperature | number | 창의성 (0~2). 낮을수록 고정적 | 0.7 ~ 0.9 |
| maxOutputTokens | integer | 최대 출력 토큰 | 1024 |
| topP | number | 샘플링 | 0.95 |
| topK | integer | 샘플링 | 40 |

- 채팅 답변은 짧게 할 경우 `maxOutputTokens` 512~1024 정도로 제한해도 됨.

### 3.5 맥락(context) 유지 — 기술적 구현

대화 흐름을 유지하려면 **이전 턴을 contents에 포함**해야 하고, **토큰 한도·비용** 때문에 “몇 턴까지·어떻게 자를지”를 정해 두어야 한다.

#### 3.5.1 정책 (권장)

| 항목 | 권장값 | 설명 |
|------|--------|------|
| **최대 턴 수** | 최근 10~20 턴 | 1 턴 = user 1건 + model 1건. 그 이상은 오래된 것부터 제외 |
| **상한 토큰(입력)** | 모델별 입력 한도 내 | 예: gemini-2.0-flash 1M, 일부 모델 32k. contents 전체가 이 한도를 넘지 않도록 |
| **자르기 방식** | **슬라이딩 윈도우** | “가장 오래된 턴부터 제거”. 최근 N 턴만 유지 |

#### 3.5.2 구현 포인트

1. **조회**: 채팅 요청 시 해당 `soulmateId`의 `ChatLog`를 **createdAt 오름차순**으로 조회. `LIMIT`으로 “최근 2N건”(user+model이 한 턴이므로) 또는 “최근 N 턴”만 가져오기.
2. **contents 조합**:  
   - DB에서 가져온 순서대로 `role`(USER → "user", AI → "model"), `parts: [{ "text": message }]` 로 넣고,  
   - 그 뒤에 **이번 사용자 메시지**를 `role: "user"`, `parts: [{ "text": userMessage }]` 로 append.
3. **토큰 제한 대비**:  
   - **옵션 A**: 턴 수로만 제한 (예: 최근 10 턴). 구현 단순.  
   - **옵션 B**: 대략적인 토큰 수 추정(글자 수 / 2 등)으로 상한 넘으면 오래된 턴부터 제거. 더 안전하지만 구현 복잡도 증가.  
   - 초기에는 **옵션 A**(최대 턴 수)만 적용해도 무방.
4. **설정 위치**: `application.yml` 예시  
   - `app.gemini.context.max-turns: 10`  
   - 또는 상수 클래스에 `MAX_CONTEXT_TURNS = 10` 등.

#### 3.5.3 서비스 레이어 역할

| 담당 | 역할 |
|------|------|
| **ChatLogService** | `findRecentBySoulmateId(soulmateId, limit)` — 최근 N건(또는 N 턴×2) 조회. 정렬 순서 보장. |
| **GeminiService** | 조회된 ChatLog 리스트를 **user/model 순서**로 contents 배열로 변환 후, 현재 userMessage 추가. `maxTurns` 초과 분은 제거(가장 오래된 턴부터). |

#### 3.5.4 예시 (최대 10 턴)

- DB에서 최근 20건 조회 (user+model 10쌍).  
- 20건을 시간순으로 contents에 넣고 + 이번 user 메시지 1건.  
- 총 21개 content 블록. Gemini는 마지막 user 메시지에 대해 답변 생성.

### 3.6 우리 서버에서의 조합 순서

1. Soulmate 조회 (gender, personalityKeywords, hobbies, speechStyles).
2. 위 필드로 **systemInstruction** 문자열 생성 (한글 지시문 + 주제 전환 대응 지침 포함).
3. **맥락 유지**: `ChatLogService`로 해당 Soulmate의 **최근 N 턴**(설정값) ChatLog 조회 → **contents**에 user/model 순서로 추가. (3.5 참고)
4. 이번 사용자 메시지를 **contents** 마지막에 `role: "user"`, `parts: [{ "text": userMessage }]` 로 추가.
5. JSON 본문 조합: `systemInstruction`, `contents`, (선택) `generationConfig`.
6. RestClient로 `POST` 요청 전송.

---

## 4. 응답 본문 (Response Body)

### 4.1 성공 시 구조 (요약)

- **candidates**: 배열. 보통 1개. 각 항목에 `content.parts[].text` 가 있음.
- **usageMetadata** (있을 수 있음): promptTokenCount, candidatesTokenCount 등.

**텍스트 추출 경로**

- 답변 텍스트: `candidates[0].content.parts[0].text`
- `candidates`가 비어 있거나 `parts`가 없을 수 있음(안전 필터 등) → 이 경우 대체 메시지 반환 필요.

**예시 (간소화)**

```json
{
  "candidates": [
    {
      "content": {
        "parts": [{ "text": "안녕! 오늘 날씨 좋지? 나도 좋아~" }],
        "role": "model"
      },
      "finishReason": "STOP"
    }
  ],
  "usageMetadata": {
    "promptTokenCount": 50,
    "candidatesTokenCount": 20,
    "totalTokenCount": 70
  }
}
```

### 4.2 우리 DTO 변환

- **필수 추출**: `candidates[0].content.parts[0].text` → JSON 응답 텍스트 문자열 추출.
- **JSON 파싱**: 추출한 텍스트에서 ```json 같은 마크다운 블록을 제거한 후, `ObjectMapper`를 사용하여 `GeminiParsedResponse` 객체 조각으로 직렬해제(deserialize) 한다.
- **반환**: 서비스는 파싱된 DTO를 컨트롤러로 전달. 우리 `POST /api/chat` 응답에 활용.
- **선택**: usageMetadata → 로깅 또는 우리 API 응답에 포함 가능.

---

## 5. 에러 및 예외 상황

### 5.1 HTTP 상태별 처리

| HTTP | 의미 | 우리 서버 동작 |
|------|------|----------------|
| 200 | 성공 | 위 경로로 텍스트 추출 후 반환 |
| 400 | 잘못된 요청(본문/파라미터) | 로그 남기고, 사용자 메시지 "요청 형식 오류" 등 반환 |
| 401 | API 키 없음/잘못됨 | 로그(키 값 제외), "AI 서비스 설정 오류" |
| 403 | 권한/할당량 초과 등 | 로그, "AI 서비스를 사용할 수 없습니다" |
| 429 | Rate Limit | 로그, "잠시 후 다시 시도해 주세요" |
| 5xx | Gemini 서버 오류 | 로그, "AI가 일시적으로 응답하지 않습니다" |

### 5.2 200이지만 candidates 비어 있음

- **promptFeedback**: 안전 필터 등으로 생성이 차단된 경우. `promptFeedback.blockReason` 등 확인.
- **candidates.length === 0** 이면: 로그 후 사용자에게 "응답을 생성하지 못했습니다" 같은 기본 메시지 반환.

### 5.3 타임아웃

- RestClient 타임아웃 발생 시: 로그 후 "요청 시간이 초과되었습니다" 반환.

### 5.4 우리 API와의 매핑

- `api-and-screen-spec.md`의 `POST /api/chat` 에러 응답과 맞춤:
  - 401/403 → 502 또는 별도 코드 + "AI 서비스 설정 오류"
  - 429 → 429 + "잠시 후 다시 시도해 주세요"
  - 5xx/타임아웃 → 502 + "AI가 일시적으로 응답하지 않습니다"

---

## 6. 요청/응답 가공 체크리스트 (구현 시)

| 단계 | 위치 | 내용 |
|------|------|------|
| 1 | GeminiService | Soulmate + userMessage → **systemInstruction** 문자열 조합 (페르소나 + JSON 형식을 요구하는 명시적 지침 포함) |
| 2 | ChatLogService / GeminiService | **맥락 유지**: 최근 N 턴 ChatLog 조회 후 **contents** 배열 조합 (이전 대화 + 이번 user 메시지), 턴 수 상한 적용 |
| 3 | GeminiService | Map 또는 DTO → **Gemini API 형식 JSON** 직렬화 |
| 4 | GeminiService | RestClient **POST** 호출 (URL, 헤더, body) |
| 5 | GeminiService | 응답 JSON 파싱 → **candidates[0].content.parts[0].text** 추출 |
| 6 | GeminiService | **JSON 문자열** 다듬기(마크다운 제거) 후 `ObjectMapper`로 aiMessage, choices, affectionDelta 객체화 |
| 7 | GeminiService | 4xx/5xx·타임아웃·빈 candidates 처리 후 우리 예외/메시지로 변환 |

---

## 7. 환경 변수

- **GEMINI_API_KEY**: Google AI Studio에서 발급. `application.yml` 또는 환경 변수로 주입.
- `.env.example` 에 `GEMINI_API_KEY=` 템플릿 제공 권장.

---

## 8. 문서 이력

| 버전 | 일자 | 변경 내용 |
|------|------|-----------|
| 0.1 | 2025-03-11 | 최초 작성 (기획·유즈케이스·API 명세 반영) |
| 0.2 | 2025-03-11 | 맥락 유지(슬라이딩 윈도우·최대 턴 수)·갑작스런 주제 전환 대응 프롬프트 설계 추가 |
| 0.3 | 2025-03-11 | 미연시 스타일 다지선다 선택지(프롬프트·출력 형식·파싱·API choices 필드) 추가 |
