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
  - nginx가 멈춘거니 8080으로 접근을 시도해도 nginx에서 8080포트가 없음. 즉 서버가 꺼져있는데 연결을 시도했으니 서버 에러 페이지 인 50x.html을 보여줄 것 같음.
- 실제 결과:
  - curl: (7) Failed to connect to localhost port 8080 after 1 ms: Couldn't connect to server가 발생
- 배운 점:
  - 50x.html이 있어서 당 error page를 보여준다고 생각했는데. 그렇지 않았다. 그럼 50x.html은 어제 발생하는건가?를 알아 봤다.
  - 50x.html의 내용을 보면 "너가 지금 보고 있는 페이지는 현재 사용불가능하다. 다음에 다시 시도해주기 바란다. 만약 너가 이 자원의 시스템 관리자면 에러 로그 확인해 봐라."라는 내용이다.
  - 그렇다면 error를 한번 유도해 보고 싶은데. nginx.conf에서 설정을 잘못하고 nginx -s reload하게 되면 50x.html이 발생하는지 확인해 봤다.
  - nginx.conf에서 http부근에 include /etc/nginx/mine.types: -> 123mine.types로 변경 reload했을 때 다음과 같은 error가 발생했다
    - ```shell 
      2026/07/18 03:38:20 [emerg] 65#65: open() "/etc/nginx/123mime.types" failed (2: No such file or directory) in /etc/nginx/nginx.conf:15 nginx: [emerg] open() "/etc/nginx/123mime.types" failed (2: No such file or directory) in /etc/nginx/nginx.conf:15
      ```
  - 지금 상태에서는 nginx 뒤에 backend 서버가 없어서 별도의 설정을 통해 504 error를 강제로 발생 시키게 하는 방법 뿐이 없음

**실험 2: worker kill**
- 예상: 삭제되면 그냥 삭제만 되고 끝나는 것으로 끝날줄 알았음

- 실제 결과: 새로운 worker process가 생성됨을 확인 함.

- 배운 점: worker process를 kill해도 새로운 worker process가 생성됨. 

## 저널 질문

**Q1. Spring Boot 내장 Tomcat만으로 서비스 중인 지금, nginx를 앞에 두면 구체적으로 무엇이 좋아지는가? (3가지 이상 — Day 10에 다시 보고 수정할 것)**

### ✍️ 나의 답변 (Day 1 시점)
1. 보안 강화 가능 : request가 들어오면 nginx를 한번 거쳐서 backend에 도착하기 때문에 nginx에서 별도의 보안을 체크 하고 backend 서버로 보낼 수 있음

2. 한번의 요청으로 각 서버가 담당하는 요청을 보낼 수 있음: 요청 하나에 대해서 요청을 로깅하는 서버와 실질적으로 그 요청에 대해서 응답하는 서버가 있다고 했을때 nginx가 각 서버에 맞은 요청을 보낼 수 있음.

3. 부하 분산 : 대규모 서버의 경우 응답하는 서버가 여러개인 경우가 만다. 만약 server가 A,B,C,D의 가 있다고 했을 때 nginx가 중간에서 서버 부하 상태를 체크하여 로드밸런싱을 통해 각 서버가 부하를 일률적으로 담당할 수 있도록 할 수 있다.

### ✍️ Day 10 수정본
1.

2.

3.

## 오늘의 한 줄 요약
>
