#!/usr/bin/env bash
# =========================================================
# ai-friends 원클릭 실행 스크립트
# ---------------------------------------------------------
#  1) .env 존재 확인 (없으면 .env.example 에서 복사)
#  2) docker compose build  (Dockerfile 안에서 gradle clean bootJar 실행)
#  3) docker compose up -d
#  4) MySQL healthcheck 통과 대기
#  5) App 기동(8080 응답) 대기
#  6) App 로그를 follow (Ctrl+C 로 종료해도 컨테이너는 계속 구동)
#
# 사용법:
#   chmod +x run.sh
#   ./run.sh              # 빌드 + 실행 + 로그
#   ./run.sh down         # 스택 종료 (볼륨 유지)
#   ./run.sh clean        # 스택 종료 + 볼륨까지 삭제
#   ./run.sh logs         # 앱 로그만 이어서 보기
# =========================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

# ---- 색상 프리픽스 ---------------------------------------
C_RESET='\033[0m'
C_INFO='\033[1;34m'
C_OK='\033[1;32m'
C_WARN='\033[1;33m'
C_ERR='\033[1;31m'

info()  { printf "${C_INFO}[INFO]${C_RESET}  %s\n" "$*"; }
ok()    { printf "${C_OK}[ OK ]${C_RESET}  %s\n" "$*"; }
warn()  { printf "${C_WARN}[WARN]${C_RESET}  %s\n" "$*"; }
err()   { printf "${C_ERR}[ERR ]${C_RESET}  %s\n" "$*" >&2; }

# ---- docker compose 실행 바이너리 탐지 -------------------
if docker compose version >/dev/null 2>&1; then
    DC="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
    DC="docker-compose"
else
    err "docker compose 를 찾을 수 없습니다. Docker Desktop 또는 docker CLI 를 먼저 설치해 주세요."
    exit 1
fi

COMMAND="${1:-up}"

case "${COMMAND}" in
    down)
        info "컨테이너 중지 및 제거 (볼륨은 유지)"
        ${DC} down
        ok "중지 완료"
        exit 0
        ;;
    clean)
        warn "컨테이너 + 볼륨(MySQL 데이터) 까지 모두 삭제합니다."
        ${DC} down -v
        ok "초기화 완료"
        exit 0
        ;;
    logs)
        ${DC} logs -f app
        exit 0
        ;;
    up) ;;
    *)
        err "알 수 없는 명령입니다: ${COMMAND}"
        echo "사용법: ./run.sh [up|down|clean|logs]"
        exit 1
        ;;
esac

# ---- .env 준비 -------------------------------------------
if [[ ! -f ".env" ]]; then
    if [[ -f ".env.example" ]]; then
        warn ".env 가 없어 .env.example 을 복사했습니다. GEMINI_API_KEY 를 채워 주세요."
        cp .env.example .env
    else
        err ".env.example 도 없습니다. 먼저 환경 파일을 준비해 주세요."
        exit 1
    fi
fi

# GEMINI_API_KEY 가 비어 있으면 경고 (Gemini 호출 시 실패)
if ! grep -E '^GEMINI_API_KEY=.+' .env >/dev/null 2>&1; then
    warn "GEMINI_API_KEY 값이 비어 있습니다. AI 채팅 기능이 동작하지 않습니다."
    warn "  → https://aistudio.google.com/app/apikey 에서 발급 후 .env 수정"
fi

# ---- 빌드 ------------------------------------------------
info "1/3  이미지 빌드 (내부에서 gradle clean bootJar 실행)"
${DC} build

# ---- 기동 ------------------------------------------------
info "2/3  컨테이너 기동 (MySQL healthcheck 통과 후 App 실행)"
${DC} up -d

# ---- MySQL healthy 대기 ----------------------------------
info "MySQL healthcheck 대기 중..."
MYSQL_CID="$(${DC} ps -q mysql)"
for i in {1..60}; do
    STATUS="$(docker inspect -f '{{.State.Health.Status}}' "${MYSQL_CID}" 2>/dev/null || echo starting)"
    if [[ "${STATUS}" == "healthy" ]]; then
        ok "MySQL is healthy"
        break
    fi
    printf "  ... (%02d/60) status=%s\n" "$i" "${STATUS}"
    sleep 2
    if [[ "$i" == "60" ]]; then
        err "MySQL 이 60초 내에 healthy 상태가 되지 않았습니다."
        ${DC} logs mysql | tail -50
        exit 1
    fi
done

# ---- App 랜딩 대기 ---------------------------------------
info "3/3  Spring Boot 기동 대기 (http://localhost:8080)"
for i in {1..60}; do
    if curl -fsS -o /dev/null http://localhost:8080/ 2>/dev/null; then
        ok "App is up → http://localhost:8080"
        break
    fi
    printf "  ... (%02d/60) waiting for app\n" "$i"
    sleep 2
    if [[ "$i" == "60" ]]; then
        warn "App 이 8080 응답을 주지 않습니다. 아래 로그를 확인해 주세요."
        break
    fi
done

echo
ok "모든 컨테이너가 올라왔습니다. 아래는 앱 로그입니다. (Ctrl+C 로 로그만 종료, 컨테이너는 계속 구동)"
echo "-------------------------------------------------------------"
${DC} logs -f app
