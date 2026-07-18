# Day 7: HTTPS — Let's Encrypt와 TLS 종료

## 학습 목표
- certbot으로 인증서를 발급하고 nginx에 적용할 수 있다
- "TLS 종료(termination)"의 의미와 X-Forwarded-Proto의 역할을 이해한다

## 핵심 개념
- **TLS 종료**: HTTPS 암호화를 nginx에서 풀고, nginx→백엔드는 평문 HTTP. 백엔드는 인증서를 몰라도 된다. 이것이 nginx를 앞에 두는 큰 이유 중 하나.
- **인증서 발급 (HTTP-01 challenge)**: Let's Encrypt가 `http://도메인/.well-known/acme-challenge/`로 접근해 도메인 소유를 확인. 80 포트가 열려 있어야 한다.
- **기본 HTTPS 설정**:
  ```nginx
  server {
      listen 443 ssl http2;
      server_name boms.example.com;
      ssl_certificate     /etc/letsencrypt/live/boms.example.com/fullchain.pem;
      ssl_certificate_key /etc/letsencrypt/live/boms.example.com/privkey.pem;
  }
  server {
      listen 80;
      server_name boms.example.com;
      return 301 https://$host$request_uri;   # HTTP → HTTPS 리다이렉트
  }
  ```
- **갱신**: 인증서 유효기간 90일. `certbot renew`를 cron/타이머로 자동화 + `nginx -s reload`.
- **X-Forwarded-Proto**: 백엔드가 "원래 요청이 https였는지" 아는 유일한 방법. 이게 없으면 Spring Security 리다이렉트가 http로 새는 버그가 생긴다.

## 실습
- 실 도메인이 있다면: certbot(docker 이미지) + webroot 방식으로 발급 → nginx 적용
- 도메인이 없다면: `mkcert`로 로컬 인증서 발급해 443 설정 연습 (원리는 동일)
- `curl -v https://...`로 인증서 체인, TLS 버전 확인

## 브레이크 실험
1. 인증서 경로를 틀리게 쓰고 `nginx -t` → 에러 확인
2. 80 포트의 acme-challenge location을 지우고 갱신 시도 → 왜 실패하는가

### 📝 브레이크 실험 기록

**실험 1: 잘못된 인증서 경로**
- 에러 메시지:

- 배운 점:

**실험 2: acme-challenge 차단 후 갱신**
- 결과:

- 배운 점:

## 저널 질문

**Q1. nginx에서 TLS를 종료하지 않고 Spring Boot가 직접 HTTPS를 받게 하면 어떤 불편이 생기는가? (인증서 갱신, 멀티 프로젝트 관점에서)**

### ✍️ 나의 답변


**Q2. X-Forwarded-Proto가 없을 때 Spring Security에서 생길 수 있는 구체적 버그 시나리오를 하나 적어보라.**

### ✍️ 나의 답변


## 오늘의 한 줄 요약
>
