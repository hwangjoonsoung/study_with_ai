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

- 실제 결과:

- 배운 점:

**실험 2: 잘못된 설정으로 reload**
- 예상:

- 실제 결과:

- 배운 점:

**실험 3: 잘못된 설정으로 restart**
- 예상:

- 실제 결과:

- 배운 점:

## 저널 질문

**Q1. `reload`와 `restart`의 차이는? 프로덕션에서 왜 reload를 써야 하는가?**

### ✍️ 나의 답변


**Q2. 기본 이미지의 nginx.conf에서 `include /etc/nginx/conf.d/*.conf;`는 어느 컨텍스트 안에 있는가? 왜 거기 있어야 하는가?**

### ✍️ 나의 답변


## 오늘의 한 줄 요약
>
