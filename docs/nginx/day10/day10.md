# Day 10: 종합 프로젝트 — BOMS 프로덕션 구성 완성

## 목표
지금까지 배운 전부를 하나의 설정으로 통합한다. 이것이 커리큘럼의 최종 산출물이다.

## 요구사항 체크리스트
- [ ] HTTP(80) → HTTPS(443) 리다이렉트
- [ ] TLS 종료 + 인증서 자동 갱신 경로 확보
- [ ] 정적 자원: gzip + 장기 캐시 (해시 파일명 전제)
- [ ] `/api/` (또는 전체) → Spring Boot 프록시, 필수 헤더 4종 세트
- [ ] Docker DNS resolver로 백엔드 재기동 대응
- [ ] `/api/`에 rate limiting
- [ ] main_ext 로그 포맷 + 로그 볼륨 마운트
- [ ] server_tokens off + 보안 헤더 + client_max_body_size
- [ ] default_server 444 차단
- [ ] (선택, React 마이그레이션 대비) `try_files $uri /index.html;` SPA fallback 구성 초안

## 최종 설정 작성 칸

### 📝 나의 최종 nginx 설정
```nginx
# 여기에 직접 작성 (완성 후 nginxconfig.io 산출물과 diff 비교)
```

### 📝 nginxconfig.io와 diff 비교 결과
- 내 설정에 없던 것과 그 이유:

- 도구 산출물에 없는데 내가 넣은 것과 그 이유:

## 최종 검증 — 스스로에게 장애 주입

**1. 백엔드 kill → 502 → 로그로 진단 → 복구 (목표: 5분 내)**
- 소요 시간:
- 진단 경로 기록:

**2. 만료된/잘못된 인증서 경로 → nginx -t로 사전 차단 확인**
- 결과:

**3. 부하 테스트 중 rate limit 동작 + 백엔드 커넥션 풀(HikariCP actuator) 안정 확인**
- ab 결과 요약:
- actuator에서 본 커넥션 풀 지표:

## 최종 저널

**Q1. Day 1에 적었던 "nginx를 왜 두는가" 답변을 다시 열어 수정하라. 무엇이 달라졌는가?**

### ✍️ 나의 답변 (달라진 점 중심으로)


**Q2. 이번 커리큘럼에서 가장 크게 착각하고 있었던 것 하나는?**

### ✍️ 나의 답변


**Q3. 다음 학습 방향 선택과 이유:**
- [ ] (a) 무중단 배포 (blue-green + upstream)
- [ ] (b) 캐싱 (proxy_cache)
- [ ] (c) 모니터링 (stub_status + Prometheus nginx exporter)

### ✍️ 선택 이유


## 커리큘럼 수료 선언
- 수료일:
- 최종 설정이 적용된 커밋 해시:
