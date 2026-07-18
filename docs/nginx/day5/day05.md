# Day 5: 리버스 프록시 — Spring Boot 연동

## 학습 목표
- nginx → Spring Boot 프록시를 구성하고, 프록시 헤더의 의미를 이해한다
- 502/504 에러가 각각 언제 나는지 구분할 수 있다

## 핵심 개념
- **proxy_pass**:
  ```nginx
  location /api/ {
      proxy_pass http://boms-app:8080;   # URI 없이 — 경로 그대로 전달
      # proxy_pass http://boms-app:8080/; # URI 있으면 — location prefix 치환 (혼동 주의!)
  }
  ```
  끝 `/` 유무가 완전히 다른 동작을 만든다. 반드시 둘 다 실험할 것.
- **프록시 필수 헤더** — 이게 없으면 Spring Boot는 모든 요청이 nginx(127.0.0.1)에서 온 것으로 본다:
  ```nginx
  proxy_set_header Host $host;
  proxy_set_header X-Real-IP $remote_addr;
  proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  proxy_set_header X-Forwarded-Proto $scheme;
  ```
  Spring 쪽: `server.forward-headers-strategy=framework` 설정으로 이 헤더들을 신뢰하게 한다.
- **upstream 블록**: 백엔드를 이름으로 묶기. 로드밸런싱/무중단 배포의 기반.
  ```nginx
  upstream boms {
      server boms-app:8080;
      keepalive 32;   # 백엔드와 커넥션 재사용
  }
  ```
- **에러 구분**: 502 Bad Gateway = 백엔드 연결 실패(죽어있음/포트 틀림), 504 Gateway Timeout = 백엔드가 `proxy_read_timeout`(기본 60s) 안에 응답 못함.

## 실습
- 기존 BOMS 컨테이너 앞에 nginx를 세워 `/` 전체를 프록시
- Spring Boot 로그에서 클라이언트 IP가 어떻게 찍히는지 헤더 설정 전후 비교
- `proxy_pass` 끝 `/` 유무 4가지 조합 실험

### 📝 proxy_pass 조합 실험표

요청: `GET /api/hello` 기준, 백엔드에 도착하는 경로를 기록

| location | proxy_pass | 백엔드 도착 경로 (예측) | 실제 |
|----------|-----------|------------------------|------|
| `/api/` | `http://boms:8080` | | |
| `/api/` | `http://boms:8080/` | | |
| `/api` | `http://boms:8080` | | |
| `/api` | `http://boms:8080/` | | |

## 브레이크 실험
1. BOMS 컨테이너 중지 → 502 확인, error log 읽기
2. Spring Boot에 `Thread.sleep(70000)` 테스트 엔드포인트 → 504 확인 → `proxy_read_timeout` 조정으로 해결

### 📝 브레이크 실험 기록

**실험 1: 백엔드 중지 → 502**
- error log에 찍힌 핵심 문구:

- 배운 점:

**실험 2: 느린 응답 → 504**
- error log에 찍힌 핵심 문구:

- 해결 방법:

## 저널 질문

**Q1. 과거 MySQL 커넥션 고갈 사건 때 Tomcat 로그의 127.0.0.1은 무엇이었나? 이제 그 정체를 nginx 관점에서 설명해보라.**

### ✍️ 나의 답변


**Q2. 502와 504를 각각 한 문장으로 정의하고, 각각 확인해야 할 첫 번째 지점을 적어라.**

### ✍️ 나의 답변
- 502:
- 504:

## 오늘의 한 줄 요약
>
