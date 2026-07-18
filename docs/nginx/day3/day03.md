# Day 3: server 블록과 location 매칭 — nginx의 심장

## 학습 목표
- 요청이 어떤 server, 어떤 location으로 라우팅되는지 정확히 추적할 수 있다
- `root` vs `alias`, `try_files`를 구분해서 쓸 수 있다

## 핵심 개념
- **server 선택**: `listen`(포트) → `server_name`(Host 헤더) 순으로 매칭. 아무것도 안 맞으면 default_server.
- **location 매칭 우선순위** (반드시 암기할 유일한 규칙):
  1. `= /exact` — 정확히 일치 (최우선)
  2. `^~ /prefix` — 이 prefix가 맞으면 정규식 검사 생략
  3. `~ regex` / `~* regex` — 정규식 (대소문자 구분/무시), **선언 순서대로** 첫 매칭
  4. `/prefix` — 일반 prefix 중 **가장 긴 것**
- **root vs alias**:
  - `location /img/ { root /data; }` → `/img/a.png` = `/data/img/a.png` (경로 이어붙임)
  - `location /img/ { alias /data/; }` → `/img/a.png` = `/data/a.png` (경로 치환)
- **try_files**: `try_files $uri $uri/ /index.html;` — SPA 라우팅의 핵심. 파일 없으면 fallback. (향후 React 마이그레이션에서 반드시 쓴다)

## 실습
location 4종류를 한 서버에 모두 넣고, curl로 어디에 매칭되는지 실험:
```nginx
server {
    listen 80;
    location = / { return 200 "exact root\n"; }
    location ^~ /static/ { return 200 "prefix priority\n"; }
    location ~* \.(jpg|png)$ { return 200 "regex image\n"; }
    location /static/img/ { return 200 "longest prefix\n"; }
    location / { return 200 "fallback\n"; }
}
```
`/`, `/static/a.css`, `/static/img/a.png`, `/hello.jpg`, `/anything` 각각 curl 해보고 **예측 → 확인 → 이유 설명** 순서로 진행.

### 📝 매칭 예측표 (curl 전에 먼저 채울 것!)

| 요청 경로 | 내 예측 | 실제 결과 | 이유 설명 |
|-----------|---------|-----------|-----------|
| `/` | | | |
| `/static/a.css` | | | |
| `/static/img/a.png` | | | |
| `/hello.jpg` | | | |
| `/anything` | | | |

## 브레이크 실험
1. `/static/img/a.png`는 왜 "longest prefix"가 아니라 "prefix priority"에 매칭되는가? `^~`를 일반 prefix로 바꾸면 결과가 어떻게 바뀌는가?
2. `alias` 끝 `/`를 빼면 어떤 일이 생기는가?

### 📝 브레이크 실험 기록

**실험 1: ^~ 제거**
- 예상:

- 실제 결과:

- 배운 점:

**실험 2: alias 끝 슬래시 제거**
- 예상:

- 실제 결과:

- 배운 점:

## 저널 질문

**Q1. BOMS에서 `/css/`, `/js/`, `/api/`, 나머지 페이지 요청을 각각 어떤 location으로 설계하면 좋을지 초안을 그려보라.**

### ✍️ 나의 답변 (설계 초안)
```nginx
# 여기에 직접 작성
```

**Q2. location 매칭 우선순위 4단계를 보지 않고 적어보라.**

### ✍️ 나의 답변
1.
2.
3.
4.

## 오늘의 한 줄 요약
>
