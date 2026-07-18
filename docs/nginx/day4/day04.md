# Day 4: 정적 파일 서빙 — 제대로 하기

## 학습 목표
- gzip 압축, 캐시 헤더(expires), MIME 타입을 설정할 수 있다
- "Tomcat이 아니라 nginx가 정적 파일을 서빙해야 하는 이유"를 수치로 확인한다

## 핵심 개념
- **MIME 타입**: `include mime.types;` — 브라우저가 파일을 어떻게 해석할지 결정
- **gzip**: 텍스트 계열(HTML/CSS/JS/JSON)에만 의미 있음. 이미지엔 역효과.
  ```nginx
  gzip on;
  gzip_types text/css application/javascript application/json;
  gzip_min_length 1024;
  ```
- **캐시 헤더**:
  ```nginx
  location ~* \.(css|js|png|jpg|woff2)$ {
      expires 30d;
      add_header Cache-Control "public, immutable";
  }
  ```
  파일명에 해시가 들어가는 빌드 산출물(React/Vite)은 `immutable` + 장기 캐시가 정석.
- **sendfile on;**: 커널 레벨 파일 전송 — nginx가 정적 파일에 빠른 이유 중 하나.

## 실습
- BOMS의 `static/` 디렉토리(CSS/JS)를 nginx 볼륨으로 마운트해 서빙
- `curl -I`로 `Content-Encoding: gzip`, `Cache-Control` 헤더 확인
- gzip on/off 전후 응답 크기 비교:
  ```bash
  curl -H "Accept-Encoding: gzip" -sw '%{size_download}\n' -o /dev/null URL
  ```

### 📝 측정 기록

| 파일 | gzip off 크기 | gzip on 크기 | 압축률 |
|------|--------------|--------------|--------|
| (예: main.css) | | | |
| | | | |

## 브레이크 실험
1. `expires 30d`를 설정한 뒤 CSS를 수정하면 브라우저에서 무슨 일이 생기는가? → 캐시 무효화(파일명 해싱)가 왜 필요한지 체감

### 📝 브레이크 실험 기록

**실험 1: 장기 캐시 후 CSS 수정**
- 예상:

- 실제 결과 (강력 새로고침 전/후):

- 배운 점:

## 저널 질문

**Q1. Vite 빌드 산출물(`index.html` + 해시 붙은 assets)에서 캐시 정책을 다르게 가져가야 하는 파일은 무엇이고 왜인가? (힌트: index.html은 캐시하면 안 된다)**

### ✍️ 나의 답변


**Q2. gzip을 이미지 파일에 적용하면 왜 역효과인가?**

### ✍️ 나의 답변


## 오늘의 한 줄 요약
>
