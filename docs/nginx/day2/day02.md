# Day 2: nginx.conf 해부 — 디렉티브와 컨텍스트

## 학습 목표
- 어떤 nginx 설정 파일이든 구조를 파악하며 읽을 수 있다
- `nginx -t`로 설정 검증, `nginx -s reload`로 무중단 반영을 할 수 있다

## 핵심 개념
- **디렉티브(directive)**: `이름 값;` 형태의 설정 한 줄. 단순 디렉티브(`;`로 끝)와 블록 디렉티브(`{ }`를 가짐)로 나뉜다.
- **컨텍스트(context)**: 블록 디렉티브가 만드는 영역. 계층 구조를 이룬다:
  ```
  main (파일 최상단)
  ├── events { }        # 커넥션 처리 방식
  └── http { }          # HTTP 서버 전체 설정
      └── server { }    # 가상 호스트 하나
          └── location { }  # URL 경로별 처리
  ```
- **상속 규칙**: 하위 컨텍스트는 상위 설정을 상속하되, 같은 디렉티브를 다시 쓰면 덮어쓴다.
- **include**: `/etc/nginx/conf.d/*.conf` 패턴. 실무에서는 사이트별 설정을 분리한다. (기본 이미지의 `default.conf`가 여기 있다)

## 실습
```bash
# 로컬에 설정 디렉토리 만들고 볼륨 마운트로 운영
mkdir -p ~/nginx-lab/conf.d
docker run -d --name nginx-lab2 -p 8080:80 \
  -v ~/nginx-lab/conf.d:/etc/nginx/conf.d nginx:alpine
```

`~/nginx-lab/conf.d/default.conf` 작성:
```nginx
server {
    listen 80;
    server_name localhost;

    location / {
        root /usr/share/nginx/html;
        index index.html;
    }
}
```

```bash
# 설정 문법 검증 (반영 전 필수 습관!)
docker exec nginx-lab2 nginx -t

# 무중단 리로드
docker exec nginx-lab2 nginx -s reload
```

## 브레이크 실험
1. 세미콜론 하나 빼고 `nginx -t` → 에러 메시지가 몇 번째 줄을 가리키는지 확인
2. `nginx -t` 없이 잘못된 설정으로 `reload` → nginx가 기존 설정을 유지하는 안전장치 관찰
3. 컨테이너를 **재시작**하면? (reload와 달리 잘못된 설정이면 컨테이너가 죽는다 — 실무 사고의 단골 원인)

### 📝 브레이크 실험 기록

**실험 1: 세미콜론 누락**
- 예상: 
  - 잘못된 부분 찾아서 어느 라인에 뭐가 잘못됬는지 확인 할 수 있도록 나타내 준다.

- 실제 결과:
  - 예상한대로 index index.html;에서 ;를 빼고 nginx -t로 태스트 해본 결과  configuration file test filed 라는 안내를 받음.

- 배운 점:
  - 환경설정 파일을 수정한 다음 nginx -t로 오류가 발생한 부분이 없는지 test를 한번 진행해 보고 nginx -s reload를 진행하면 좋음.

**실험 2: 잘못된 설정으로 reload**
- 예상:
  - 잘못된 부분이 있으니 당연히 reload불가

- 실제 결과:
  - 예상한 대로 당연히 reload는 불가 하며 동시에 어디가 잘못됬는지 확인 알려준다.

- 배운 점:
  - 없음

**실험 3: 잘못된 설정으로 restart**
- 예상:
  - restart 불가능

- 실제 결과:
  - restart 불가능

- 배운 점:
  - Reload와는 다르게 환경설정 파일중 어느 부분이 잘못되었는지 알려주지 않는다.

## 저널 질문

**Q1. `reload`와 `restart`의 차이는? 프로덕션에서 왜 reload를 써야 하는가?**

### ✍️ 나의 답변
- 궁극적으로 보면 재시작 하는 기능은 다를게 없다. 하지만 reload의 경우 nginx에서 제공하는 test를 먼저 진행하고 이상이 없으면 restart를 진행하지만 restart의 경우 별도의 test를 진행하지 않는다.

**Q2. 기본 이미지의 nginx.conf에서 `include /etc/nginx/conf.d/*.conf;`는 어느 컨텍스트 안에 있는가? 왜 거기 있어야 하는가?**

### ✍️ 나의 답변
- http context에 위치 해야 한다. 왜일까? http 관련 부분만 설정하기 위해서 http context에 위치한다고 생각하는데. 그렇다면 default.conf에 뭐가 들어가 있는지 본다면 다음과 같다. 
- server Context가 들어 있다. 이 뜻을 보면 결국 계층 nginx.conf의 context 계층구조가 다음과 같이 있기 때문이다.
- main (Global)           # 프로세스 권한, 로그 위치 등
  ├── events              # 비동기 이벤트 처리 방식 설정
  └── http                # 웹 서버/리버스 프록시 설정 시작
      ├── upstream        # 백엔드 서버 그룹 (로드밸런싱 등)
      └── server          # 가상 호스트 설정
      └── location    # URL 경로에 따른 처리 방식 지정
- 결국 *.conf를 하는데 default.conf가 각 server 별로 분리되는 구조로 만들고 nginx.conf에서 해당 conf파일들을 include 하는 구조로 동작한다는 의미다. 

## 오늘의 한 줄 요약
> 이렇게 공부 하는게 맞나 싶다. 뭔가 그냥 대충하는것같아...
