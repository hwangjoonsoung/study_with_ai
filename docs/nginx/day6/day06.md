# Day 6: Docker 환경의 nginx — proxy-net 아키텍처 이해

## 학습 목표
- 지금 운영 중인 shared external network(proxy-net) 구조를 밑바닥부터 직접 재구성할 수 있다
- Docker DNS와 nginx의 upstream resolve 타이밍 문제를 이해한다

## 핵심 개념
- **컨테이너 이름 = DNS**: 같은 네트워크의 컨테이너는 서비스 이름으로 접근. `proxy_pass http://boms-app:8080`이 동작하는 원리.
- **expose vs ports**: 백엔드는 `expose`만(내부 노출), nginx만 `ports`로 호스트에 노출 — 공격 표면 최소화.
- **DNS 캐싱 함정**: nginx는 기본적으로 **시작 시점에** upstream 도메인을 IP로 고정한다. 백엔드 컨테이너가 재생성되어 IP가 바뀌면 nginx는 옛 IP를 계속 찌른다(502). 해결책:
  ```nginx
  resolver 127.0.0.11 valid=10s;   # Docker 내장 DNS
  set $backend http://boms-app:8080;
  proxy_pass $backend;             # 변수 사용 시 런타임 resolve
  ```
- **멀티 프로젝트 리버스 프록시**: nginx 하나 + `server_name`별 분기 → 프로젝트별 compose 파일 + 공유 external network. (현재 홈서버 구조가 바로 이것)

## 실습
- 빈 상태에서 시작: `docker network create proxy-net` → nginx compose + 더미 앱 2개 compose를 각각 띄우고 `server_name`으로 라우팅
- 백엔드 컨테이너 `down` → `up` 후 502 재현 → resolver 방식으로 수정해 해결

## 브레이크 실험
1. 백엔드를 proxy-net에 연결하지 않고 프록시 시도 → 어떤 에러? error log에서 DNS 실패와 connection refused를 구분해보기

### 📝 브레이크 실험 기록

**실험 1: 네트워크 미연결 프록시**
- error log 문구:

- DNS 실패 vs connection refused 구분법:

**실험 2: 백엔드 재생성 후 502 재현 → resolver로 해결**
- 재현 과정:

- 해결 후 확인 방법:

## 저널 질문

**Q1. 지금 홈서버의 nginx 설정을 열어서 각 줄을 주석으로 설명해보라. 설명 못 하는 줄이 남아있는가?**

### ✍️ 나의 답변 (설명 못 한 줄과 조사 결과)


**Q2. 백엔드 컨테이너에 `ports` 대신 `expose`만 쓰는 이유를 보안 관점에서 설명하라.**

### ✍️ 나의 답변


## 오늘의 한 줄 요약
>
