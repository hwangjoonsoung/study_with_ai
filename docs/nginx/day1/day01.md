# Day 1: 웹서버란 무엇인가 + 첫 실행

## 학습 목표
- 웹서버(nginx)와 WAS(Tomcat)의 역할 차이를 설명할 수 있다
- nginx의 이벤트 기반(event-driven) 아키텍처가 왜 빠른지 이해한다
- Docker로 nginx를 띄우고 기본 페이지를 확인한다

## 핵심 개념
- **웹서버 vs WAS**: nginx는 정적 파일 서빙·프록시·TLS 종료에 특화, Tomcat은 Java 코드 실행에 특화. 둘은 경쟁 관계가 아니라 분업 관계다.
- **프로세스 모델**: master process(설정 읽기, worker 관리) + worker process(실제 요청 처리). Apache의 스레드-퍼-커넥션과 달리 nginx worker 하나가 이벤트 루프로 수천 커넥션을 처리한다.
- **C10K 문제**: nginx가 태어난 배경. 커넥션 1만 개를 어떻게 동시에 감당할 것인가.

## 실습
```bash
# 1. nginx 컨테이너 실행
docker run -d --name nginx-lab -p 8080:80 nginx:alpine

# 2. 확인
curl http://localhost:8080

# 3. 컨테이너 내부 구조 탐험
docker exec -it nginx-lab sh
ls /etc/nginx/            # 설정 파일 위치
cat /etc/nginx/nginx.conf # 메인 설정
ls /usr/share/nginx/html/ # 기본 정적 파일 위치
ps aux                    # master + worker 프로세스 확인
```

## 브레이크 실험
1. `docker exec nginx-lab nginx -s stop` 후 curl → 어떤 에러가 나는가? (Connection refused vs 502의 차이를 미리 체감)
2. worker process를 kill 해보기 → master가 자동으로 재생성하는 것 관찰

### 📝 브레이크 실험 기록

**실험 1: nginx stop 후 curl**
- 예상:

- 실제 결과:

- 배운 점:

**실험 2: worker kill**
- 예상:

- 실제 결과:

- 배운 점:

## 저널 질문

**Q1. Spring Boot 내장 Tomcat만으로 서비스 중인 지금, nginx를 앞에 두면 구체적으로 무엇이 좋아지는가? (3가지 이상 — Day 10에 다시 보고 수정할 것)**

### ✍️ 나의 답변 (Day 1 시점)
1.

2.

3.

### ✍️ Day 10 수정본
1.

2.

3.

## 오늘의 한 줄 요약
>
