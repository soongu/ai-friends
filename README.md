# ai-friends

Spring AI 강의의 기반 프로젝트입니다. Day 1 기준으로는 RestClient 로 Gemini 를 수동 호출하는
미연시 스타일 챗봇 데모이며, 이후 Day 별로 Spring AI 기반으로 확장됩니다.

로컬에서는 **IDE 로 직접 실행(H2)** 과 **docker compose 로 실행(MySQL)** 두 가지 방법을
지원합니다.

---

## 1. 요구 사항

| 항목 | 버전 | 비고 |
|------|------|------|
| JDK | 25 | IDE 실행 시에만 필요. Docker 실행에는 불필요. |
| Docker Desktop | 최신 | `docker compose` v2 명령어 사용 |
| Google Gemini API Key | — | https://aistudio.google.com/app/apikey |

---

## 2. 환경 변수(.env) 설정

프로젝트 루트에 `.env` 파일을 두고 값을 채웁니다. 템플릿은 `.env.example` 에 있습니다.

```bash
cp .env.example .env
```

`.env` 파일은 `.gitignore` 에 포함되어 있어 커밋되지 않습니다. 절대 API 키를 커밋하지 마세요.

### 2-1. 필수 키

| 변수 | 설명 | 예시 |
|------|------|------|
| `GEMINI_API_KEY` | Google AI Studio 에서 발급받은 Gemini API 키 | `AIza...` |
| `GEMINI_MODEL` | 사용할 Gemini 모델 | `gemini-2.5-flash-lite` |

### 2-2. 로컬(IDE) 실행용

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `DB_URL` | IDE 실행 시 사용할 JDBC URL (H2 인메모리) | `jdbc:h2:mem:aifriends;DB_CLOSE_DELAY=-1` |

### 2-3. docker compose 실행용 (MySQL)

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 비밀번호 | `root1234` |
| `MYSQL_DATABASE` | 생성할 데이터베이스 이름 | `aifriends` |
| `MYSQL_USER` | 앱이 사용할 계정 | `aifriends` |
| `MYSQL_PASSWORD` | 앱 계정 비밀번호 | `aifriends1234` |

> docker compose 로 실행하면 `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` 는 compose 파일에서
> 자동으로 `jdbc:mysql://mysql:3306/...` 형태로 주입됩니다. `.env` 의 `DB_URL`(H2) 은
> 덮어씌워지므로 그대로 두어도 됩니다.

### 2-4. `.env` 가 읽히는 방식

- **docker compose**: `docker compose` 가 같은 디렉터리의 `.env` 를 자동 로드해서
  `${변수}` 문법으로 치환합니다.
- **IDE 로컬 실행**: [`DotenvInitializer`](src/main/java/kr/spartaclub/aifriends/config/DotenvInitializer.java)
  가 애플리케이션 구동 전에 `.env` 를 읽어 Spring `Environment` 에 등록합니다.

---

## 3. 실행 방법

### 방법 A. 원클릭 스크립트 (권장)

빌드부터 MySQL 헬스체크 통과 → 앱 랜딩 확인 → 로그 tail 까지 한 번에 수행합니다.

```bash
chmod +x run.sh        # 최초 1회
./run.sh               # 빌드 + 실행 + 로그
```

부가 명령:

```bash
./run.sh down          # 컨테이너만 내리기 (MySQL 데이터는 유지)
./run.sh clean         # 컨테이너 + 볼륨까지 모두 삭제 (DB 초기화)
./run.sh logs          # 실행 중인 앱의 로그만 이어서 보기
```

접속:
- 앱: http://localhost:8080
- MySQL: `localhost:3309` (컨테이너 내부 포트 3306 → 호스트 3309 로 노출)
  - DBeaver 등에서 접속 시 `MYSQL_USER` / `MYSQL_PASSWORD` 로 로그인

### 방법 B. docker compose 직접 호출

```bash
docker compose up -d --build
docker compose logs -f app
docker compose down            # 내리기
docker compose down -v         # 내리면서 MySQL 볼륨까지 삭제
```

Dockerfile 안에서 `./gradlew clean bootJar` 가 실행되므로 호스트에 JDK 가 없어도 됩니다.

### 방법 C. IDE 로 직접 실행 (H2)

Spring 기본 프로파일이 `local` 이므로 IDE 에서 그냥 Run 하면 H2 인메모리로 동작합니다.
`.env` 의 `GEMINI_API_KEY` 만 채우면 됩니다.

---

## 4. 동작 확인 체크리스트

- `./run.sh` 실행 후 `[ OK ] MySQL is healthy` → `[ OK ] App is up` 로그가 보여야 정상.
- http://localhost:8080 접속이 되면 성공.
- MySQL 에 직접 붙어 스키마가 생성됐는지 확인:
  ```bash
  docker exec -it ai-friends-mysql mysql -uaifriends -paifriends1234 aifriends -e "show tables;"
  ```

---

## 5. 트러블슈팅

| 증상 | 원인 / 해결 |
|------|-------------|
| `./run.sh` 이 즉시 실패하며 "docker compose 를 찾을 수 없습니다" | Docker Desktop 이 실행 중이 아니거나 구버전. Docker Desktop 최신 버전 설치. |
| MySQL healthcheck 가 60초 내 통과하지 못함 | 3309 포트가 이미 사용 중일 수 있음. `lsof -i :3309` 로 확인 후 종료하거나, `docker-compose.yml` 의 포트 매핑(`"3309:3306"`)을 다른 값으로 변경. |
| 앱이 8080 응답을 안 줌 | `docker compose logs app` 로 예외 확인. 대부분 `GEMINI_API_KEY` 미설정 또는 DB 접속 실패. |
| 스키마/데이터를 깨끗이 초기화하고 싶음 | `./run.sh clean` 실행 후 다시 `./run.sh`. |

---

## 6. 참고

- Spring Boot Gradle Plugin: https://docs.spring.io/spring-boot/4.0.3/gradle-plugin
- Spring AI Reference (1.1.x): https://docs.spring.io/spring-ai/reference/1.1/
- Gemini API: https://ai.google.dev/gemini-api/docs
