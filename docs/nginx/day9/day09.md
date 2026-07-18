# Day 9: 보안과 성능 Best Practice

## 학습 목표
- rate limiting으로 백엔드(DB 커넥션 풀)를 보호할 수 있다
- 프로덕션 체크리스트를 자기 언어로 설명할 수 있다

## 핵심 개념

### 보안
- **rate limiting** — 과거 커넥션 고갈 사건의 예방책:
  ```nginx
  # http 컨텍스트
  limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
  # location
  location /api/ {
      limit_req zone=api burst=20 nodelay;   # 순간 폭주 20개까지 허용, 초과는 503
  }
  ```
- **server_tokens off;** — 버전 노출 차단
- **보안 헤더**: `X-Content-Type-Options nosniff`, `X-Frame-Options SAMEORIGIN`, HSTS(HTTPS 안정화 후)
- **client_max_body_size**: 기본 1m — 파일 업로드 기능 있으면 반드시 조정 (413 에러의 원인)
- **default_server로 미지정 Host 차단**: `return 444;` (IP 직접 스캔 봇 차단)

### 성능
- `worker_processes auto;` + `worker_connections 1024;` (최대 동시접속 ≈ 곱)
- `keepalive_timeout 65;` + upstream `keepalive` — 커넥션 재사용
- 버퍼: `proxy_buffers`는 대부분 기본값으로 충분. **측정 없이 튜닝하지 말 것**이 최고의 튜닝 조언.

## 실습
- BOMS `/api/`에 rate limit 적용 → `ab -n 100 -c 20` 부하로 503 발생 확인 → access log에서 limiting 기록 확인
- `curl -I`로 Server 헤더, 보안 헤더 확인
- BOMS 파일 업로드(엑셀 등록자 명단!)에 맞는 `client_max_body_size` 산정

### 📝 측정 기록

**rate limit 부하 테스트 (`ab -n 100 -c 20`)**
- 2xx 개수: / 503 개수:
- burst 값을 바꿨을 때 변화:

**client_max_body_size 산정**
- BOMS 최대 업로드 파일 예상 크기:
- 설정값과 근거:

## 브레이크 실험
1. rate를 극단적으로 낮춰(1r/s) 일반 페이지 로딩이 어떻게 깨지는지 관찰 → CSS/JS 요청까지 막히는 문제 → 정적/API location 분리의 필요성 체감

### 📝 브레이크 실험 기록

**실험 1: 1r/s 극단 제한**
- 브라우저에서 관찰된 현상:

- 배운 점:

## 저널 질문

**Q1. rate limiting을 nginx에서 거는 것과 Spring(인터셉터)에서 거는 것의 차이는? 각각 어떤 공격/부하에 유효한가?**

### ✍️ 나의 답변


**Q2. 과거 MySQL 커넥션 고갈 사건에 오늘 배운 것 중 무엇이 어떻게 방어선이 되는지 계층별로 정리하라. (nginx → Tomcat → HikariCP → MySQL)**

### ✍️ 나의 답변
- nginx 계층:
- Tomcat/HikariCP 계층:
- MySQL 계층:

## 오늘의 한 줄 요약
>
