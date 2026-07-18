# Nginx 완전 입문 커리큘럼 (10일)

> **대상**: nginx를 처음 접하는 Java/Spring Boot 개발자
> **환경**: Mac mini 홈서버 + Docker + Spring Boot(BOMS)
> **방식**: 매일 [개념 → 실습 → 브레이크 실험 → 저널] 사이클

## 사용법
1. `dayXX.md`를 하루에 하나씩 진행한다.
2. 각 파일의 **브레이크 실험 기록**과 **저널 답변** 칸을 반드시 직접 채운다. 채우지 않으면 다음 날로 넘어가지 않는다.
3. 설정 변경 시 5단계 습관: **수정 → `nginx -t` → `reload` → curl 검증 → 로그 확인**
4. 모든 실험 설정은 이 디렉토리와 함께 git으로 버전 관리한다.

## 전체 로드맵

| Phase | 파일 | 주제 | 도달점 |
|-------|------|------|--------|
| 1. 기초 개념 | day01–03 | 웹서버 개념, 설정 구조, location 매칭 | nginx.conf를 읽을 수 있다 |
| 2. 실전 핵심 | day04–07 | 정적 서빙, 리버스 프록시, Docker, HTTPS | BOMS 앞단에 nginx를 세울 수 있다 |
| 3. 운영 | day08–10 | 로깅/디버깅, 보안/성능, 종합 프로젝트 | 프로덕션 구성을 스스로 설계·진단할 수 있다 |

## 관련 자료

### 필수 (커리큘럼과 병행)
| 자료 | 용도 | 링크 |
|------|------|------|
| nginx 공식 Beginner's Guide | Day 1–2 개념 원문 | https://nginx.org/en/docs/beginners_guide.html |
| nginx 공식 Admin Guide | Day 4–9 각 주제별 레퍼런스 | https://docs.nginx.com/nginx/admin-guide/ |
| 디렉티브 알파벳 사전 | 모르는 디렉티브 만날 때마다 | https://nginx.org/en/docs/dirindex.html |
| How nginx processes a request | Day 3 필독 | https://nginx.org/en/docs/http/request_processing.html |

### 강력 추천
- **DigitalOcean**: "Understanding Nginx Server and Location Block Selection Algorithms" 검색
- **Mozilla SSL Configuration Generator**: https://ssl-config.mozilla.org — Day 7 TLS 설정 생성 후 각 줄 이해
- **nginxconfig.io**: Day 10 완성본과 diff 비교 검증용

### 심화 (커리큘럼 이후)
- 『NGINX Cookbook』 (O'Reilly, F5 무료 배포판)
- nginx 공식 블로그 Performance Tuning 시리즈
- proxy_cache, stub_status, OpenResty(Lua)

## 학습 원칙
- AI에게 설정을 "생성"시키지 말고 "리뷰"시켜라. 초안은 반드시 직접 쓴다.
- 측정 없이 튜닝하지 않는다.
- 일부러 망가뜨리고 로그로 복구하는 것이 이 커리큘럼의 핵심 훈련이다.
