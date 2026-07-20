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
  - docker ps를 해보니까 nginx가 Exited로 변경됨
- 배운 점:
  - 50x.html이 있어서 당 error page를 보여준다고 생각했는데. 그렇지 않았다. 그럼 50x.html은 어제 발생하는건가?를 알아 봤다.
  - 50x.html의 내용을 보면 "너가 지금 보고 있는 페이지는 현재 사용불가능하다. 다음에 다시 시도해주기 바란다. 만약 너가 이 자원의 시스템 관리자면 에러 로그 확인해 봐라."라는 내용이다.
  - 그렇다면 error를 한번 유도해 보고 싶은데. nginx.conf에서 설정을 잘못하고 nginx -s reload하게 되면 50x.html이 발생하는지 확인해 봤다.
  - nginx.conf에서 http부근에 include /etc/nginx/mine.types: -> 123mine.types로 변경 reload했을 때 다음과 같은 error가 발생했다
    - ```shell 
      2026/07/18 03:38:20 [emerg] 65#65: open() "/etc/nginx/123mime.types" failed (2: No such file or directory) in /etc/nginx/nginx.conf:15 nginx: [emerg] open() "/etc/nginx/123mime.types" failed (2: No such file or directory) in /etc/nginx/nginx.conf:15
      ```
  - 지금 상태에서는 nginx 뒤에 backend 서버가 없어서 별도의 설정을 통해 504 error를 강제로 발생 시키게 하는 방법 뿐이 없음
  - failed to connect 새 localhost라고 나오는 이유는 docker exec nginx -s stop 했을 때 docker에서 돌아가는 nginx가 exit된다. 때문에 연결할 수 없는 상대이다.

**실험 2: worker kill**
- 예상: 삭제되면 그냥 삭제만 되고 끝나는 것으로 끝날줄 알았음

- 실제 결과: 새로운 worker process가 생성됨을 확인 함.

- 배운 점:
  - worker process를 kill해도 새로운 worker process가 생성됨. 이는 master process가 worker process가 죽었다는것을 감지해 새로 worker process를 생성하는 것이다. 
  - 그 증거로 master를 멈췄을 때 worker를 kill 하는경우 worker가 생성되면 안되는데. 확인 결과 생성되지 않았고, 다시 master process를 활성화 시켰을 때 worker가 생성됨을 확인 했다.
  - ```shell
    # host 네임스페이스의 PID 확인
    docker top nginx
    
    # 상위(=host) 네임스페이스에서 발사
    docker run --rm --pid=host --privileged alpine kill -STOP <host_pid>
      
    # 확인 → 재생성 안 됨. worker 수가 줄어든 채로 유지됨
    docker top nginx
    ```

**추가실험 1: nginx -s stop**
- 예상 : docker는 살아 있고 nginx는 상태가 멈춘 상태로 유지 되는 것으로 생각함.

- 실제 결과: master process가 모든 worker process를 exit 상태로 만들고, 확인한다음 자신을 exit 시킨다. 이때 docker도 같이 exit상태로 만든다.

**추가실험 2: nginx -s quit**
- 예상 : 예상하지 못함. 내용으로는 그냥 종료 하는 것으로 생각함.

- 실제 결과:
  - nginx -s stop과 가장 큰 차이는 worker가 진행하고 있는 일이 있는지 확인하는 것이다. 만약 진행하고 있는 process가 있다고 하면 해당 process가 종료 될 때까지 기다렸다가 exiting한다.
  - 결국 nginx -s stop과 nginx -s quit의 가장 큰 차이는 worker가 진행하고 있는 process에 대해서 강제 종료 할 것인지, 완료 된 후 종료할 것인지를 선택하는 것이다.

- nginx -s stop
```shell 
2026/07/20 22:04:05 [notice] 1#1: signal 15 (SIGTERM) received from 41, exiting
2026/07/20 22:04:05 [notice] 31#31: exiting
2026/07/20 22:04:05 [notice] 30#30: exiting
2026/07/20 22:04:05 [notice] 32#32: exiting
2026/07/20 22:04:05 [notice] 33#33: exiting
2026/07/20 22:04:05 [notice] 34#34: exiting
2026/07/20 22:04:05 [notice] 35#35: exiting
2026/07/20 22:04:05 [notice] 37#37: exiting
2026/07/20 22:04:05 [notice] 39#39: exiting
2026/07/20 22:04:05 [notice] 40#40: exiting
2026/07/20 22:04:05 [notice] 36#36: exiting
2026/07/20 22:04:05 [notice] 38#38: exiting
2026/07/20 22:04:05 [notice] 31#31: exit
2026/07/20 22:04:05 [notice] 30#30: exit
2026/07/20 22:04:05 [notice] 35#35: exit
2026/07/20 22:04:05 [notice] 37#37: exit
2026/07/20 22:04:05 [notice] 34#34: exit
2026/07/20 22:04:05 [notice] 39#39: exit
2026/07/20 22:04:05 [notice] 36#36: exit
2026/07/20 22:04:05 [notice] 40#40: exit
2026/07/20 22:04:05 [notice] 38#38: exit
2026/07/20 22:04:05 [notice] 32#32: exit
2026/07/20 22:04:05 [notice] 33#33: exit
2026/07/20 22:04:05 [notice] 1#1: signal 17 (SIGCHLD) received from 39
2026/07/20 22:04:05 [notice] 1#1: worker process 31 exited with code 0
2026/07/20 22:04:05 [notice] 1#1: worker process 39 exited with code 0
2026/07/20 22:04:05 [notice] 1#1: signal 29 (SIGIO) received
2026/07/20 22:04:05 [notice] 1#1: signal 17 (SIGCHLD) received from 36
2026/07/20 22:04:05 [notice] 1#1: worker process 30 exited with code 0
2026/07/20 22:04:05 [notice] 1#1: worker process 36 exited with code 0
2026/07/20 22:04:05 [notice] 1#1: signal 29 (SIGIO) received
2026/07/20 22:04:05 [notice] 1#1: signal 17 (SIGCHLD) received from 32
2026/07/20 22:04:05 [notice] 1#1: worker process 32 exited with code 0
2026/07/20 22:04:05 [notice] 1#1: worker process 37 exited with code 0
2026/07/20 22:04:05 [notice] 1#1: signal 29 (SIGIO) received
2026/07/20 22:04:05 [notice] 1#1: signal 17 (SIGCHLD) received from 38
2026/07/20 22:04:05 [notice] 1#1: worker process 38 exited with code 0
2026/07/20 22:04:05 [notice] 1#1: worker process 40 exited with code 0
2026/07/20 22:04:05 [notice] 1#1: signal 29 (SIGIO) received
2026/07/20 22:04:05 [notice] 1#1: signal 17 (SIGCHLD) received from 33
2026/07/20 22:04:05 [notice] 1#1: worker process 33 exited with code 0
2026/07/20 22:04:05 [notice] 1#1: signal 29 (SIGIO) received
2026/07/20 22:04:05 [notice] 1#1: signal 17 (SIGCHLD) received from 35
2026/07/20 22:04:05 [notice] 1#1: worker process 34 exited with code 0
2026/07/20 22:04:05 [notice] 1#1: worker process 35 exited with code 0
2026/07/20 22:04:05 [notice] 1#1: exit
```

- nginx -s quit log
```shell
2026/07/20 22:13:50 [notice] 22#22: gracefully shutting down
2026/07/20 22:13:50 [notice] 23#23: gracefully shutting down
2026/07/20 22:13:50 [notice] 24#24: gracefully shutting down
2026/07/20 22:13:50 [notice] 25#25: gracefully shutting down
2026/07/20 22:13:50 [notice] 22#22: exiting
2026/07/20 22:13:50 [notice] 24#24: exiting
2026/07/20 22:13:50 [notice] 26#26: gracefully shutting down
2026/07/20 22:13:50 [notice] 25#25: exiting
2026/07/20 22:13:50 [notice] 23#23: exiting
2026/07/20 22:13:50 [notice] 26#26: exiting
2026/07/20 22:13:50 [notice] 28#28: gracefully shutting down
2026/07/20 22:13:50 [notice] 29#29: gracefully shutting down
2026/07/20 22:13:50 [notice] 28#28: exiting
2026/07/20 22:13:50 [notice] 29#29: exiting
2026/07/20 22:13:50 [notice] 30#30: gracefully shutting down
2026/07/20 22:13:50 [notice] 30#30: exiting
2026/07/20 22:13:50 [notice] 32#32: gracefully shutting down
2026/07/20 22:13:50 [notice] 32#32: exiting
2026/07/20 22:13:50 [notice] 22#22: exit
2026/07/20 22:13:50 [notice] 27#27: gracefully shutting down
2026/07/20 22:13:50 [notice] 25#25: exit
2026/07/20 22:13:50 [notice] 23#23: exit
2026/07/20 22:13:50 [notice] 24#24: exit
2026/07/20 22:13:50 [notice] 27#27: exiting
2026/07/20 22:13:50 [notice] 26#26: exit
2026/07/20 22:13:50 [notice] 29#29: exit
2026/07/20 22:13:50 [notice] 28#28: exit
2026/07/20 22:13:50 [notice] 30#30: exit
2026/07/20 22:13:50 [notice] 32#32: exit
2026/07/20 22:13:50 [notice] 27#27: exit
2026/07/20 22:13:50 [notice] 31#31: gracefully shutting down
2026/07/20 22:13:50 [notice] 31#31: exiting
2026/07/20 22:13:50 [notice] 31#31: exit
2026/07/20 22:13:50 [notice] 1#1: signal 17 (SIGCHLD) received from 22
2026/07/20 22:13:50 [notice] 1#1: worker process 22 exited with code 0
2026/07/20 22:13:50 [notice] 1#1: worker process 28 exited with code 0
2026/07/20 22:13:50 [notice] 1#1: signal 29 (SIGIO) received
2026/07/20 22:13:50 [notice] 1#1: signal 17 (SIGCHLD) received from 26
2026/07/20 22:13:50 [notice] 1#1: worker process 25 exited with code 0
2026/07/20 22:13:50 [notice] 1#1: worker process 26 exited with code 0
2026/07/20 22:13:50 [notice] 1#1: worker process 31 exited with code 0
2026/07/20 22:13:50 [notice] 1#1: worker process 32 exited with code 0
2026/07/20 22:13:50 [notice] 1#1: signal 29 (SIGIO) received
2026/07/20 22:13:50 [notice] 1#1: signal 17 (SIGCHLD) received from 29
2026/07/20 22:13:50 [notice] 1#1: worker process 29 exited with code 0
2026/07/20 22:13:50 [notice] 1#1: signal 29 (SIGIO) received
2026/07/20 22:13:50 [notice] 1#1: signal 17 (SIGCHLD) received from 27
2026/07/20 22:13:50 [notice] 1#1: worker process 23 exited with code 0
2026/07/20 22:13:50 [notice] 1#1: worker process 27 exited with code 0
2026/07/20 22:13:50 [notice] 1#1: worker process 30 exited with code 0
2026/07/20 22:13:50 [notice] 1#1: signal 29 (SIGIO) received
2026/07/20 22:13:50 [notice] 1#1: signal 17 (SIGCHLD) received from 30
2026/07/20 22:13:50 [notice] 1#1: signal 17 (SIGCHLD) received from 24
2026/07/20 22:13:50 [notice] 1#1: worker process 24 exited with code 0
2026/07/20 22:13:50 [notice] 1#1: exit
```

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
> nginx의 시작, -s로 nginx의 상태를 변경할 수 있음을 알았다.그리고 기본 정적 파일의 위치를 어디서 확인할 수 있는지, 400번대, 500번대 에러가 발생하면 어떤 html파일을 띄우는지 확인할 수 있었다.

## 추가적으로 배운점.
1. 500 error이 발생하면 50x.html을 띄워 주는데 서빙되는 조건은 500, 502, 503, 504 error가 발생했을 때 50x.html을 띄워준다. 현 상태에서는 500대 error를 발생시키기 어려운 상황이라, 50x.html을 확인하기 까다롭다. nginx.conf 내용을 수정하는 검증방법은 잘못된 것이다. 환경설정이후 nginx를 다시 reload하거나 restart 해야 하는데 해당 과정에서 발생한 error는 server 환경 자체의 에러 이기 때문에 500번대 에러를 발생하지 않는다.

## 🔍 리뷰 피드백

### 2026-07-20 리뷰 (판정: BLOCK)
- [verified] (완성도) "오늘의 한 줄 요약"이 비어 있다. 이것만 채우면 게이트는 통과다. → (2026-07-21 확인) 210~211줄에 요약이 채워졌고 브레이크 실험·저널도 모두 기록됨.
- [reopen] (기술 정확성) 실험 1의 "50x.html이 언제 뜨는지"를 확인하려고 `include 123mime.types`로 reload를 시도했는데, 이건 검증 방법 자체가 틀렸다. `[emerg]`는 **설정 파싱 단계에서 난 에러**라 nginx는 새 설정을 적용하지 않고 기존 프로세스를 그대로 유지한다 — HTTP 응답을 만드는 단계에 도달조차 못 하므로 어떤 에러 페이지도 나올 수 없다. 50x.html이 실제로 서빙되는 조건이 무엇인지(어느 디렉티브가 그 파일을 5xx 응답에 연결하는지) 기본 `default.conf`를 다시 열어 확인하고 정리할 것. → (2026-07-21) "500/502/503/504 일 때 50x.html" 이라는 조건과 "conf 수정으로 검증한 건 잘못된 방법" 이라는 인식은 정확히 잡혔다. 다만 항목이 물은 **어느 디렉티브가** 그 연결을 만드는지가 아직 안 적혔다.
- [reopen] (승격) 실험 1의 핵심 원칙이 아직 안 나왔다. `Failed to connect`(curl exit 7)와 502/504는 **다른 레이어**에서 발생한다. 에러 페이지는 HTTP 응답 바디인데, HTTP 응답을 받으려면 먼저 무엇이 성립해야 하는가? 이 한 문장을 배운 점에 적을 것. → (2026-07-21) "왜 연결이 안 됐는가"(컨테이너 exit)는 설명됐지만, "에러 페이지는 HTTP 응답 바디이고 그걸 받으려면 TCP 연결이 먼저 성립해야 한다"는 레이어 구분 문장 자체는 아직 없다.
- [verified] (승격) 실험 1에서 놓친 관찰이 하나 더 있다. `nginx -s stop` 직후 `docker ps -a`로 nginx-lab **컨테이너 자체의 상태**는 어땠는가? 컨테이너 안에서 nginx master가 PID 1이라는 사실(`docker exec nginx-lab2 ps -o pid,args`로 확인 가능)과 연결하면, 왜 8080이 애초에 connection refused였는지가 설명된다. → (2026-07-21 확인) 40줄 `docker ps` 관찰 + 50줄 인과 서술 + 추가실험 1("master 가 자신을 exit 시키고 docker 도 같이 exit")로 반영됨. 로그의 `1#1` 이 master 라는 것도 증거로 남았다.
- [reopen] (승격) 실험 2가 "kill해도 재생성된다"는 현상 서술에서 멈춰 있다. 누가(어느 프로세스가), 어떤 신호로 자식의 죽음을 감지하고, 어떤 시스템 콜로 새 worker를 만드는가? 그리고 이 구조가 Day 2의 `nginx -s reload`(무중단 반영)와 어떻게 같은 메커니즘인가? → (2026-07-21) master 를 SIGSTOP 으로 멈춰 대조군을 만든 건 훌륭한 검증이다. 다만 "어떤 신호로 감지하는지", "어떤 시스템 콜로 만드는지", "reload 와 같은 메커니즘인지" 세 가지 중 두 가지가 아직 비어 있다.
- [hold] (기술 정확성) 저널 A2("요청 하나에 대해 로깅 서버와 응답 서버로 각각 보낼 수 있다")는 부정확하다. 리버스 프록시의 기본 동작은 요청 1건을 **하나의** upstream으로만 보내는 것이고, nginx의 로깅은 별도 서버로 분기하는 게 아니라 자기 자신의 `access_log`다. 요청을 복제해 다른 서버로도 보내는 건 별도 모듈이 필요한 특수 기능이다. 무엇을 말하려던 것이었는지(경로별 분기인지, 요청 복제인지) 구분해서 다시 쓸 것.
- [hold] (기술 정확성) 저널 A3의 "nginx가 서버 부하 상태를 체크하여" 부분이 틀렸다. 오픈소스 nginx의 기본 분배 방식은 무엇이고, 그것이 백엔드의 실제 부하(CPU/메모리)를 참조하는가? `least_conn`이 보는 값은 정확히 무엇인가? 문서(https://docs.nginx.com/nginx/admin-guide/load-balancer/http-load-balancer/)로 확인 후 수정할 것.
- [hold] (승격) 저널 답변 3개가 모두 "프록시로서의 nginx"에만 몰려 있다. Day 1 핵심 개념에 나온 프로세스 모델/C10K 관점에서, 내장 Tomcat이 직접 클라이언트를 상대할 때 불리해지는 지점(느린 클라이언트, 정적 파일, TLS 핸드셰이크)은 어디인가? Day 10 수정본에서 이 축을 추가할 것.
- [verified] (추가 실험) `nginx -s stop`과 `nginx -s quit`을 각각 실행하고 `docker logs`를 비교할 것. 두 신호의 차이(즉시 종료 vs graceful)를 문서(https://nginx.org/en/docs/beginners_guide.html)에서 먼저 예상한 뒤 확인하면 예측-검증 사이클이 된다. → (2026-07-21 확인) 추가실험 1·2로 수행됐고 양쪽 로그 원문까지 남겼다. quit 로그에만 `gracefully shutting down` 이 찍히는 차이를 스스로 잡아냈다.

