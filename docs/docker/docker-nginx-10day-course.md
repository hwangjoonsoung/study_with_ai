# Docker + Nginx 10일 학습 코스 (1일 2시간)

> **사용법**
> 1. 매일 해당 Day의 **개념 → 실습 → 생각해볼 질문 → 숙제** 순으로 진행한다.
> 2. 하단 **학습 일지 템플릿**을 복사해 그날의 기록을 남긴다.
> 3. 일지를 Claude에게 붙여넣으면 → 성공/미흡 판정 + 다음 단계 조언을 받는다.
>
> **핵심 원칙**
> - 항상 샌드박스에서 먼저 실험하고, 검증된 것만 BOMS에 반영한다.
> - "됐다"에서 멈추지 말고 "왜 됐는지"까지 간다.
> - 매일 최소 한 번은 **의도적으로 깨뜨린다.** 장애를 만들어본 사람만 장애를 고칠 수 있다.
> - 매일 한 번은 **"Docker가 없었다면?"**을 자문한다. 도구는 그것이 없앤 고통을 알 때만 진짜로 이해된다.

---

## 코스 전체 지도

```
[1주차 — Nginx 기본기 + Docker 함정]
Day 1  샌드박스 구축 · location 매칭
Day 2  proxy_pass 슬래시 · Docker DNS
Day 3  프록시 헤더 · Spring 연동
Day 4  정적 파일 · gzip · 타임아웃 · 업로드
Day 5  로그 · 트러블슈팅 (1주차 종합)

[2주차 — HTTPS + 실전 아키텍처]
Day 6  TLS 기초 · mkcert 로컬 HTTPS
Day 7  HTTP→HTTPS 리다이렉트 · HSTS
Day 8  Let's Encrypt 실전 (certbot)
Day 9  SPA 라우팅 · /api 프록시 분리
Day 10 SSE · WebSocket · rate limit · BOMS 최종 적용
```

| Day | 산출물 | BOMS/Saveface 연결점 |
|-----|--------|---------------------|
| 1 | 동작하는 sandbox compose | 실험 인프라 확보 |
| 2 | 슬래시 4패턴 비교표 | 배포 시 Nginx만 죽는 현상 예방 |
| 3 | 헤더 전달 완성 conf | Spring 로그의 클라이언트 IP 정상화 |
| 4 | 성능 옵션 적용 conf | 파일 업로드 413, 리포트 504 예방 |
| 5 | 커스텀 로그 포맷 + 장애 노트 | 병목이 Nginx인지 앱인지 즉시 판별 |
| 6 | 로컬 443 동작 | 발급 절차와 분리해 TLS 이해 |
| 7 | 리다이렉트 conf | 전 서비스 HTTPS 강제 |
| 8 | 실제 인증서 발급/갱신 | Mac mini 홈서버 운영 자동화 |
| 9 | SPA용 conf 초안 | Thymeleaf→React 전환 의사결정 |
| 10 | BOMS 최종 conf | Saveface SSE 스트리밍 대비 |

---

## 관통 질문 — "왜 Docker인가?"

이 코스는 Nginx 설정법만 배우는 코스가 아니다. 매일의 실습은 동시에 **"Docker가 어떤 문제를 없애주고 있는가"를 체감하는 재료**다. 도구를 "원래 그렇게 쓰는 것"으로 받아들이면 반쪽 지식이 된다 — Docker가 없던 시절의 고통을 알아야, Docker의 각 기능이 왜 그 모양인지 이해되고, 나아가 "이 상황엔 Docker가 과하다"는 판단까지 할 수 있게 된다.

### Docker가 해결하는 문제 4가지 (코스 내내 검증할 가설)

| # | 문제 | Docker 이전의 고통 | Docker의 답 | 체감하는 Day |
|---|------|-------------------|------------|-------------|
| 1 | **환경 재현** | "제 컴퓨터에선 되는데요" — OS/라이브러리/버전 차이로 서버마다 다르게 동작 | 이미지 = 실행 환경 통째로 포장. 어디서 돌려도 같음 | Day 0, 1 |
| 2 | **격리** | 한 서버의 프로그램들이 포트/라이브러리/설정 파일을 공유하며 서로를 오염 | 컨테이너마다 독립된 파일시스템·네트워크·프로세스 공간 | Day 1, 2 |
| 3 | **폐기 가능성(disposability)** | 서버를 오래 쓸수록 "아무도 건드리지 못하는" 눈송이 서버가 됨. 실험이 두려워짐 | 지우고 다시 만들면 그만. 상태는 볼륨에만 | Day 1, 4, 8 |
| 4 | **선언적 구성** | 서버 설치 순서가 담당자 머릿속에만 있음. 복구 = 그 사람 호출 | compose 파일 = 실행 가능한 문서. Git으로 이력 관리 | Day 8, 10 |

이건 아직 **가설**이다. 10일 동안 실습마다 위 표의 어느 칸을 체감했는지 일지에 기록하고, Day 10에 "검증됐는가 / 과장인가"를 자기 언어로 결론 낸다.

### Day 0 — 대조 실험: Docker 없이 해보기 (첫날 시작 전 40분, 강력 추천)

Docker의 가치는 **Docker 없이 같은 일을 해봐야** 보인다. Day 1을 시작하기 전에 딱 한 번, 구식으로 해본다:

1. Mac에 Nginx를 직접 설치한다: `brew install nginx`
2. conf 위치를 찾아 (`/opt/homebrew/etc/nginx/`) 8081 포트로 아무 정적 페이지나 서빙되게 수정
3. `brew services start nginx` 로 기동, 동작 확인
4. 이제 **기록하며 관찰**할 것:
   - conf, 로그, html 루트가 각각 어디 있는가? 이걸 어떻게 알아냈는가? (문서? 검색? — 컨테이너라면 Dockerfile/이미지 문서 한 곳에 있다)
   - 버전은 뭐가 깔렸는가? **1.24를 깔고 싶었다면?** (brew는 특정 버전 고정이 번거롭다 — 이미지 태그 `nginx:1.24-alpine` 한 줄과 대조)
   - Nginx를 **하나 더** 띄우고 싶다면? (포트 충돌, conf 분리… 컨테이너라면 그냥 서비스 하나 추가)
   - 이 Nginx를 **흔적 없이 제거**하려면 뭘 다 지워야 하는가? `brew uninstall` 후에도 남는 것은? (`docker compose down`과 대조)
5. 관찰이 끝나면 `brew services stop nginx && brew uninstall nginx` 로 정리하고, Day 1부터는 컨테이너로만 간다.

이 40분이 코스 전체의 "왜"를 담보한다. 일지 첫 항목으로 **"직접 설치에서 가장 귀찮았던 것 1가지"**를 적어두라 — Day 10 회고 때 다시 본다.

### 매일의 "Docker가 없었다면?" 질문

각 Day의 일지에 그날의 질문에 대한 답을 포함한다 (2~4문장이면 충분):

| Day | 질문 |
|-----|------|
| 1 | 오늘 만든 샌드박스를 Docker 없이 만들었다면 뭐가 필요했나? (Nginx 설치 + 백엔드 2개 = 몇 개의 설치와 설정?) 그리고 실험이 끝난 뒤 "완전한 원상복구"는 가능했을까? |
| 2 | 서비스명 DNS(`app-a`)는 Docker가 준 기능이다. 호스트에 직접 깔았다면 Nginx는 백엔드를 뭘로 가리켜야 했고, 백엔드가 늘어나면 뭐가 고통스러워지나? |
| 3 | 오늘 배운 프록시 헤더 문제는 Docker와 무관하게 존재한다. 그럼 "Docker 때문에 생긴 문제"와 "프록시라서 생긴 문제"를 구분하는 기준은 뭔가? (도구 탓과 구조 탓을 가르는 연습) |
| 4 | `client_max_body_size` 실험에서 conf를 수십 번 고쳤다. 운영 서버에 직접 SSH로 들어가 conf를 고치는 방식과 볼륨 마운트+reload 방식 — 실수했을 때의 복구 비용이 어떻게 다른가? |
| 5 | 로그가 컨테이너와 함께 사라지는 건 Docker의 **단점**이다. 폐기 가능성(장점)과 로그 유실(단점)이 같은 뿌리에서 나온다는 게 무슨 뜻인가? Docker가 만들어낸 새로운 문제는 또 뭐가 있나? |
| 6 | 인증서 파일을 컨테이너에 넣는 방법이 여러 개다(이미지에 COPY / 볼륨 마운트). 각각 "이미지 = 불변, 상태 = 볼륨" 원칙에 비추면 어느 쪽이 맞고 왜인가? |
| 7 | 리다이렉트 설정 실수로 서비스가 루프에 빠졌다고 하자. 컨테이너 환경에서의 롤백(이전 conf로 되돌리기)과 직접 설치 환경에서의 롤백은 절차가 어떻게 다른가? Git으로 conf를 관리한다는 것의 의미는? |
| 8 | certbot을 호스트에 설치하지 않고 컨테이너로 실행했다(`docker run --rm certbot/certbot`). "설치 없이 도구를 빌려 쓰고 버린다"는 이 패턴이 열어주는 가능성을 자기 사례로 2개 들어보라. |
| 9 | React 빌드 산출물을 넣는 3가지 방법(COPY/볼륨/별도 컨테이너)을 저울질했다. 이 고민 자체가 "이미지란 무엇인가"에 대한 질문이다 — 이미지에 들어가야 하는 것과 밖에 있어야 하는 것을 가르는 자신의 기준을 서술하라. |
| 10 | 최종 질문: BOMS를 Docker 없이 Mac mini에 직접 운영한다면 뭘 잃는가? 반대로, **Docker가 오히려 과한 경우**는 언제인가? (예: 단일 정적 사이트, 개인 스크립트) — "항상 Docker"도 교조다. 판단 기준을 자기 언어로. |

---

# 1주차 — Nginx 기본기 + Docker 함정

---

## Day 1 — 샌드박스 구축, location 매칭

### 오늘의 개념

**Nginx의 요청 처리 순서**를 먼저 그림으로 잡는다.

```
요청 도착
  → server 블록 선택 (listen 포트 + server_name 매칭)
  → location 블록 선택 (아래 우선순위)
  → 해당 location의 지시어 실행 (proxy_pass / root / return ...)
```

**location 매칭 우선순위** (이게 오늘의 핵심):

1. `= /path` — 정확히 일치 (최우선, 매칭되면 즉시 종료)
2. `^~ /path` — prefix 매칭인데, 매칭되면 정규식 검사를 **생략**
3. `~ regex` / `~* regex` — 정규식 (대소문자 구분 / 무시), **conf에 쓰인 순서대로** 첫 매칭 승리
4. `/path` — 일반 prefix, **가장 긴 것**이 승리

> 흔한 오해: "위에 쓴 location이 이긴다" — 아니다. prefix는 길이순, 정규식만 순서순이다.

**reload vs restart**: Nginx는 `reload` 시 기존 워커가 진행 중인 요청을 다 처리하고 새 워커로 교체된다(graceful). 컨테이너 restart는 연결이 끊긴다. 운영에서는 거의 항상 reload.

### 시간 배분
- 0:00–0:30 샌드박스 compose 작성 및 기동
- 0:30–1:20 location 패턴 실험
- 1:20–1:45 의도적으로 깨뜨리고 로그 읽기
- 1:45–2:00 일지 작성

### 실습

```yaml
# docker-compose.yml
services:
  nginx:
    image: nginx:alpine
    ports: ["8080:80"]
    volumes:
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
  app-a:
    image: traefik/whoami
  app-b:
    image: traefik/whoami
```

```nginx
server {
    listen 80;
    location /a/ { proxy_pass http://app-a:80/; }
    location /b/ { proxy_pass http://app-b:80/; }
}
```

**실험 A — whoami 읽는 법 익히기**
`curl -v localhost:8080/a/` 실행 후 응답에서 `Hostname`(어느 컨테이너가 받았나), `GET /`(백엔드가 받은 경로), `X-Forwarded-*`(아직 없음 — Day 3 예고) 를 확인.

**실험 B — 우선순위 토너먼트**
아래 5개를 한 conf에 넣고, 각각 다른 백엔드나 `return 200 "여기는 X\n";` 로 구분되게 한 뒤 요청을 날려 누가 이기는지 표로 기록:

```nginx
location = /a        { return 200 "exact\n"; }
location ^~ /a/b     { return 200 "prefix-no-regex\n"; }
location ~ ^/a       { return 200 "regex\n"; }
location /a/         { return 200 "prefix\n"; }
location /a/b/c      { return 200 "longest-prefix\n"; }
```

`/a`, `/a/`, `/a/b`, `/a/b/c`, `/a/x` 를 각각 요청해서 결과 기록.

**실험 C — 의도적 파괴**
1. 서비스명을 `app-c`로 오타 → `docker compose up` → `docker logs`에서 `host not found in upstream` 확인. **Nginx는 부팅 시 upstream 이름을 해석하지 못하면 아예 뜨지 않는다.**
2. conf에 문법 오류(세미콜론 삭제) → `nginx -t`가 **몇 번째 줄**을 알려주는지 확인.

**습관화**: 설정 변경 → `docker exec <c> nginx -t` → 통과 시 `docker exec <c> nginx -s reload`. 이 3단 콤보를 오늘 최소 10번 반복.

### 체크포인트
- [ ] `nginx -t` → `reload` 흐름이 손에 익었다
- [ ] 우선순위 토너먼트 표를 실제 결과로 채웠다
- [ ] upstream 오타 시 부팅이 실패하는 로그를 직접 봤다
- [ ] reload와 restart의 차이를 설명할 수 있다

### 생각해볼 질문
1. prefix 매칭이 "긴 것 우선"인 이유는 뭘까? "먼저 쓴 것 우선"이었다면 어떤 운영상 문제가 생겼을까?
2. `^~`는 어떤 상황을 위해 존재할까? (힌트: 정적 파일 location과 정규식 location이 공존할 때)
3. Nginx가 부팅 시 upstream DNS를 못 찾으면 죽어버리는 설계는 장점일까 단점일까? 어떤 철학이 깔려 있을까?

### 숙제 (30분 이내, 다음 날 일지에 결과 포함)
- **H1.** `location ~ \.(jpg|png)$` 와 `location /images/` 가 공존할 때 `/images/cat.jpg` 는 어디로 갈까? **먼저 예측을 적고**, 실험으로 검증하라. 예측이 틀렸다면 왜 틀렸는지 분석하라.
- **H2.** BOMS의 현행 nginx.conf에서 location 블록만 뽑아 우선순위 순서로 재배열해보라. 실제 conf에 쓰인 순서와 매칭 순서가 다른 부분이 있는가?

---

## Day 2 — proxy_pass 슬래시, Docker 네트워킹 함정

### 오늘의 개념

**proxy_pass의 URI 치환 규칙** — Nginx에서 가장 악명 높은 함정:

- proxy_pass에 **URI가 있으면** (`http://app/` ← 마지막 슬래시도 URI다): location에 매칭된 부분이 그 URI로 **치환**된다.
- proxy_pass에 **URI가 없으면** (`http://app`): 클라이언트가 보낸 경로가 **그대로** 전달된다.

**Docker DNS의 동작**:
- compose 네트워크 안에서 서비스명은 내장 DNS(`127.0.0.11`)가 컨테이너 IP로 해석해준다.
- 컨테이너 안의 `localhost`(=127.0.0.1)는 **그 컨테이너 자신**이다. 호스트도, 다른 컨테이너도 아니다.
- Nginx는 기본적으로 **부팅 시점에 한 번만** upstream 이름을 해석하고 캐싱한다. → 백엔드 컨테이너가 재생성되어 IP가 바뀌면 Nginx는 옛 IP를 물고 있다. → `resolver` + 변수 조합이 이걸 런타임 해석으로 바꾼다.

**`depends_on`의 진실**: 컨테이너 **시작 순서**만 보장하지, 안의 프로세스가 **포트를 열었는지**는 모른다. Spring Boot는 뜨는 데 수십 초 걸리므로, Nginx가 먼저 준비돼도 백엔드는 아직일 수 있다. (`condition: service_healthy` + healthcheck가 정석 해법 — 오늘 숙제)

### 시간 배분
- 0:00–0:50 슬래시 4패턴 매트릭스 실험
- 0:50–1:30 Docker DNS / resolver 실험
- 1:30–1:45 depends_on 실험
- 1:45–2:00 일지 작성

### 실습

**실험 A — 슬래시 매트릭스** (`/a/hello?x=1` 요청, whoami로 백엔드 수신 경로 확인)

| # | location | proxy_pass | 예측 | 실제 |
|---|---|---|---|---|
| 1 | `/a/` | `http://app-a:80/` | | |
| 2 | `/a/` | `http://app-a:80` | | |
| 3 | `/a` | `http://app-a:80/` | | |
| 4 | `/a` | `http://app-a:80` | | |
| 5 | `/a/` | `http://app-a:80/sub/` | | |

**반드시 예측 칸을 먼저 채우고 실험하라.** 쿼리스트링(`?x=1`)이 유지되는지도 함께 관찰.

**실험 B — localhost의 정체**
```nginx
location /wrong/ { proxy_pass http://localhost:8080/; }
```
→ 요청하면 무슨 에러? (`502`) error.log에는 뭐라고 찍히나? (`connection refused` — 자기 자신 8080에 아무도 없으니까)

**실험 C — 부팅 실패와 resolver 회피**
1. `docker compose stop app-a` 상태에서 `docker compose restart nginx` → 부팅 실패 재현
2. 회피 패턴 적용 후 같은 상황 재현:
```nginx
resolver 127.0.0.11 valid=10s;
set $up http://app-a:80;
location /a/ { proxy_pass $up; }
```
→ 이번엔 Nginx가 뜨고, `/a/` 요청 시에만 502가 난다. **"전체 장애"가 "부분 장애"로 격리**된 것.
3. `app-a`를 다시 올리면 재기동 없이 복구되는지 확인 (`valid=10s` 덕분).

**실험 D — IP 캐싱 문제 재현**
resolver 없는 원래 conf로 되돌린 뒤 `docker compose up -d --force-recreate app-a` (IP 변경 유도) → 502가 나는지 확인. resolver 패턴에서는 10초 후 자동 복구되는지 대조.

### 체크포인트
- [ ] 슬래시 매트릭스 5칸을 예측→실험으로 채웠고, 틀린 예측의 이유를 안다
- [ ] 컨테이너 안 localhost가 왜 실패하는지 error.log 근거로 설명할 수 있다
- [ ] resolver + 변수 패턴이 "부팅 실패 회피"와 "IP 갱신" 두 문제를 동시에 푸는 걸 확인했다

### 생각해볼 질문
1. Nginx가 upstream IP를 부팅 시 캐싱하는 건 성능상 이점이 있다. 어떤 트레이드오프인가? Kubernetes 같은 환경에서는 왜 이게 더 큰 문제가 될까?
2. 변수 proxy_pass를 쓰면 URI 치환 규칙이 달라진다(치환이 아니라 변수값 그대로). 이게 슬래시 매트릭스와 어떻게 상호작용할까? (직접 실험해봐도 좋음)
3. BOMS 재배포 시 `docker compose up -d` 로 앱 컨테이너가 재생성된다면, 지금 conf는 실험 D의 문제에 노출되어 있나?

### 숙제
- **H1.** `depends_on`에 `condition: service_healthy` + 백엔드 healthcheck를 붙여서, 백엔드가 완전히 뜬 뒤에만 Nginx가 시작되게 compose를 고쳐라. Spring Boot라면 어떤 엔드포인트를 healthcheck로 쓰면 좋을까? (힌트: 이미 아는 Actuator)
- **H2.** 어제 숙제 H1의 예측-검증 결과를 일지에 포함하라.
- **H3.** (선택) `upstream` 블록 문법을 찾아보고, `server app-a:80;` 하나짜리 upstream 블록으로도 같은 구성을 만들어보라. 어떤 상황에서 upstream 블록이 필수가 될까? (Day 10 예고)

---

## Day 3 — 프록시 헤더와 Spring 연동

### 오늘의 개념

프록시가 끼는 순간 백엔드가 잃어버리는 정보 세 가지:

| 잃는 것 | 복구 헤더 | 안 넘기면 생기는 일 |
|---|---|---|
| 클라이언트 IP | `X-Real-IP`, `X-Forwarded-For` | 로그에 전부 프록시 IP(Docker 게이트웨이)만 찍힘 |
| 원래 프로토콜 | `X-Forwarded-Proto` | 리다이렉트가 `http://`로 나감 → Day 7의 무한루프 |
| 원래 Host | `Host` | 가상호스트 라우팅, 절대 URL 생성이 깨짐 |

**`X-Forwarded-For`의 구조**: `클라이언트, 프록시1, 프록시2` 형태로 누적된다. `$proxy_add_x_forwarded_for`는 기존 값 뒤에 `$remote_addr`를 덧붙인다. → **맨 앞 값은 클라이언트가 조작 가능**하다는 보안 함의가 있다 (생각해볼 질문 3).

**Host 변수 3형제**:
- `$host` — 요청 라인 → Host 헤더 순으로 결정, 포트 없음. 대부분 이걸 쓴다.
- `$http_host` — Host 헤더 그대로 (포트 포함될 수 있음)
- `$server_name` — conf에 적힌 이름 고정. 요청과 무관.

**Spring의 짝**: `server.forward-headers-strategy: native` (Tomcat의 RemoteIpValve 사용) 또는 `framework` (Spring의 ForwardedHeaderFilter). 이걸 켜야 `request.getRemoteAddr()`, `getScheme()`, `sendRedirect()`가 프록시 헤더를 반영한다.

### 시간 배분
- 0:00–0:40 whoami로 헤더 유무 비교
- 0:40–1:30 BOMS(또는 로컬 Spring)에 연결해 실측
- 1:30–2:00 일지 작성

### 실습

**실험 A — 헤더 없이 vs 있이** (whoami)

```nginx
# 1차: 헤더 없이
location /a/ { proxy_pass http://app-a:80/; }

# 2차: 헤더 4종 추가
location /a/ {
    proxy_pass http://app-a:80/;
    proxy_set_header Host              $host;
    proxy_set_header X-Real-IP         $remote_addr;
    proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

1차에서 whoami가 보는 `Host`가 `app-a:80`(proxy_pass 대상 주소!)인 것, 2차에서 원래 요청 host로 바뀌는 것 확인.

**실험 B — 조작된 X-Forwarded-For 관찰**
```bash
curl -H "X-Forwarded-For: 1.2.3.4" localhost:8080/a/
```
whoami가 받는 XFF가 `1.2.3.4, <실제IP>` 로 누적되는 것 확인. → 맨 앞만 믿으면 IP 스푸핑에 당한다.

**실험 C — Spring 실측**
간단한 컨트롤러(또는 BOMS 아무 엔드포인트)에서 `request.getRemoteAddr()`와 `request.getScheme()`을 로그로 찍고:
1. `forward-headers-strategy` 없이 → 게이트웨이 IP 확인
2. `native` 켜고 → 실제 클라이언트 IP 확인

### 체크포인트
- [ ] 헤더 유무에 따라 백엔드가 보는 Host/IP가 어떻게 달라지는지 표로 정리했다
- [ ] XFF 누적 구조와 스푸핑 가능성을 직접 관찰했다
- [ ] `forward-headers-strategy` on/off의 차이를 Spring 로그로 실측했다

### 생각해볼 질문
1. `proxy_set_header Host $host;` 를 생략하면 Nginx는 기본으로 뭘 보낼까? (실험 A에서 봤다) 이 기본값이 문제되는 실제 시나리오는?
2. `X-Real-IP` 와 `X-Forwarded-For` 는 역할이 겹친다. 왜 둘 다 존재하고, 언제 뭘 믿어야 할까?
3. BOMS에서 "요청자 IP 기준 rate limit"이나 "IP 화이트리스트"를 만든다면, XFF의 어느 위치 값을 써야 안전한가? Nginx 계층에서 거르는 것과 Spring 계층에서 거르는 것 중 어디가 맞을까?

### 숙제
- **H1.** Nginx의 `real_ip` 모듈(`set_real_ip_from`, `real_ip_header`)을 조사하고, "신뢰할 수 있는 프록시" 개념이 왜 필요한지 3줄로 요약하라.
- **H2.** BOMS 로그에 지금 클라이언트 IP가 제대로 찍히고 있는지 확인하라. 안 찍히고 있다면 오늘 배운 걸로 고치되, **고치기 전/후 로그를 캡처**해 일지에 첨부.
- **H3.** (선택) Cloudflare 같은 CDN이 앞에 하나 더 있다면 헤더 체인이 어떻게 될지 그림으로 그려보라.

---

## Day 4 — 정적 파일 · gzip · 타임아웃 · 업로드

### 오늘의 개념

**Nginx가 정적 파일에 강한 이유**: 이벤트 기반 + `sendfile`(커널이 파일을 소켓으로 직접 복사, 유저스페이스 왕복 없음). 반면 Spring을 거치면 스레드 점유 + JVM 힙 경유. → css/js/이미지는 Nginx가 직접 서빙하는 게 정석.

**타임아웃 3형제** (혼동 주의):
- `proxy_connect_timeout` — 백엔드에 **TCP 연결을 맺는** 시간 (기본 60s, 보통 5s면 충분)
- `proxy_send_timeout` — 백엔드로 **요청을 쓰는** 사이 시간
- `proxy_read_timeout` — 백엔드로부터 **응답을 읽는** 사이 시간 (기본 60s) ← 504의 주범

**캐시 헤더**: `expires 30d`는 `Cache-Control: max-age`를 생성한다. 강캐싱을 걸면 배포 후에도 브라우저가 옛 파일을 쓴다 → 해법은 파일명 해싱(Vite가 `app.a1b2c3.js` 로 자동 처리) + `index.html`만 no-cache. Day 9와 직결.

**413의 정체**: `client_max_body_size` 기본값 **1MB**. 업로드 기능 만들고 "작은 파일은 되는데 큰 건 안 돼요" 소리 나오면 십중팔구 이거다. Spring 쪽 `spring.servlet.multipart.max-file-size`와 **양쪽 다** 맞춰야 한다.

### 시간 배분
- 0:00–0:40 정적 서빙 + 캐시 헤더
- 0:40–1:20 gzip / 업로드 한도 / 타임아웃
- 1:20–1:45 증상 재현 (413, 504)
- 1:45–2:00 일지 작성

### 실습

```nginx
client_max_body_size 20m;
proxy_connect_timeout 5s;
proxy_read_timeout   120s;

gzip on;
gzip_types text/css application/javascript application/json;
gzip_min_length 1024;
gzip_comp_level 5;

location ~* \.(css|js|png|jpg|svg|woff2)$ {
    root /usr/share/nginx/html;
    expires 30d;
    add_header Cache-Control "public";
    access_log off;              # 정적 파일 로그 소음 제거
}
```

**실험 A — gzip 검증**
```bash
# 압축 요청
curl -s -H "Accept-Encoding: gzip" -o /dev/null -w "%{size_download}\n" localhost:8080/big.css
# 비압축 요청
curl -s -o /dev/null -w "%{size_download}\n" localhost:8080/big.css
```
큰 CSS 파일 하나 만들어 넣고 크기 차이 확인. 응답 헤더의 `Content-Encoding: gzip`, `Vary: Accept-Encoding`도 확인.

**실험 B — 413 재현**
`client_max_body_size 1k;` 로 낮추고 `curl -F "f=@bigfile" ...` → 413 확인. error.log 메시지 기록. 이때 **Spring 로그에는 아무것도 안 찍히는 것**도 확인 (요청이 백엔드에 도달조차 안 함 — 이게 트러블슈팅에서 중요한 단서).

**실험 C — 504 재현**
`proxy_read_timeout 3s;` + 백엔드 5초 지연(whoami 대신 `sleep 5` 하는 간단한 서버, 또는 Spring에 `Thread.sleep(5000)` 엔드포인트) → 504 확인. error.log의 `upstream timed out` 기록.

**실험 D — 캐시 함정 재현**
`expires 30d` 상태에서 css 내용을 바꾸고 브라우저 새로고침 → 안 바뀌는 것 확인 (강력 새로고침과 비교). "왜 파일명 해싱이 필요한가"를 몸으로.

### 체크포인트
- [ ] gzip 전후 크기를 수치로 기록했다
- [ ] 413 재현 + "Spring에 로그가 없다"는 단서의 의미를 이해했다
- [ ] 504 재현 + 타임아웃 3형제 중 어느 것이 원인인지 구분할 수 있다
- [ ] 캐시 때문에 배포가 반영 안 되는 상황을 재현했다

### 생각해볼 질문
1. gzip_comp_level을 9로 올리면 뭐가 좋아지고 뭐가 나빠질까? Nginx가 매 요청마다 압축하는 비용을 줄이는 방법은? (힌트: `gzip_static`)
2. 이미지(png/jpg)를 gzip_types에 안 넣는 이유는?
3. BOMS의 학회 데이터 엑셀 업로드를 생각하면 `client_max_body_size`는 얼마가 적절한가? 무한정 키우면 어떤 공격에 노출되나?
4. `proxy_read_timeout`을 매우 길게(600s) 잡는 것으로 긴 리포트 문제를 "해결"하는 것과, 비동기 처리(작업 큐 + 폴링)로 재설계하는 것 — 각각 언제 정당한가?

### 숙제
- **H1.** `proxy_buffering`이 켜져 있을 때(기본값) Nginx가 응답을 어떻게 다루는지 조사하라. 디스크 버퍼링이 일어나는 조건은? (Day 10 SSE의 복선)
- **H2.** BOMS 업로드 한도를 Nginx / Spring 양쪽에서 확인하고 정합성을 맞춰라. 전/후 값을 일지에 기록.
- **H3.** (선택) `sendfile on;` `tcp_nopush on;` 이 뭘 하는지 한 줄씩 정리.

---

## Day 5 — 로그와 트러블슈팅 (1주차 종합)

### 오늘의 개념

**두 개의 시간이 말해주는 것**:
- `$request_time` — Nginx가 요청 첫 바이트를 받고 응답 마지막 바이트를 보낼 때까지 (클라이언트 네트워크 포함)
- `$upstream_response_time` — 백엔드에 연결하고 응답을 다 받을 때까지

| 패턴 | 진단 |
|---|---|
| 둘 다 큼 | 백엔드가 느리다 (앱/DB 조사) |
| request만 크고 upstream은 작음 | 클라이언트 네트워크가 느리거나, 응답이 커서 전송이 오래 걸림 |
| upstream이 비어 있음(`-`) | 백엔드에 아예 안 갔다 (Nginx가 직접 처리: 정적, 413, 리다이렉트 등) |

**상태코드로 범인 좁히기**:
- **502 Bad Gateway** — 백엔드에 연결 실패 / 백엔드가 이상한 응답 (백엔드 죽음, 포트 틀림)
- **504 Gateway Timeout** — 연결은 됐는데 응답이 시간 내 안 옴 (백엔드 느림)
- **503** — Nginx 자체가 거부 (rate limit 등)
- **499** — 클라이언트가 응답 기다리다 먼저 끊음 (Nginx 전용 코드)

### 시간 배분
- 0:00–0:30 커스텀 로그 포맷 적용
- 0:30–1:20 장애 4종 재현 → 로그 대조표 작성
- 1:20–1:45 1주차 종합 퀴즈 (아래)
- 1:45–2:00 일지 작성

### 실습

```nginx
log_format perf '$remote_addr [$time_local] "$request" $status '
                'rt=$request_time urt=$upstream_response_time '
                'up=$upstream_addr bytes=$body_bytes_sent';
access_log /var/log/nginx/access.log perf;
```

**실험 — 장애 4종 재현 후 표 완성**

| 장애 | 만드는 법 | status | urt | error.log 핵심 문구 |
|---|---|---|---|---|
| 백엔드 다운 | `docker stop app-a` | ? | ? | ? |
| 백엔드 느림 | read_timeout 3s + 5s 지연 | ? | ? | ? |
| location 미스 | 없는 경로 요청 | ? | ? | ? |
| 업로드 초과 | max_body 1k + 큰 파일 | ? | ? | ? |

**1주차 종합 퀴즈** (일지에 답 포함 — 판정 대상)

1. 사용자가 "가끔 파일 업로드가 안 돼요"라고 한다. Spring 로그에는 아무 에러가 없다. 첫 번째로 볼 곳과 그 이유는?
2. 재배포 직후에만 30초쯤 502가 났다가 저절로 낫는다. 원인 후보 두 가지와 각각의 해법은? (Day 2 내용)
3. access.log에 `status=200 rt=8.2 urt=0.1` 인 요청이 반복된다. 무슨 상황인가?
4. `/api/users` 는 프록시로 가야 하는데 어쩐지 정적 파일 404가 난다. location 설정에서 의심할 것은? (Day 1 내용)

### 체크포인트
- [ ] 장애 4종 표를 실측으로 채웠다
- [ ] 502와 504를 "만드는 법"부터 다르게 설명할 수 있다
- [ ] 종합 퀴즈 4문항에 근거를 들어 답했다

### 생각해볼 질문
1. access_log를 JSON 포맷으로 남기면 뭐가 좋아질까? BOMS의 모니터링 스택(Prometheus/Grafana, Uptime Kuma)과 어떻게 연결할 수 있을까?
2. 499가 자주 보인다면 사용자 경험 관점에서 무슨 일이 벌어지고 있는 걸까? 어떤 지표와 함께 봐야 하나?
3. 로그를 컨테이너 안에 두면 컨테이너 재생성 시 사라진다. Docker 로깅 드라이버 vs 볼륨 마운트 — BOMS엔 뭐가 맞을까?

### 숙제
- **H1.** BOMS 운영 nginx에 `perf` 포맷(또는 원하는 변형)을 적용하고, 하루치 로그에서 **가장 느린 요청 5개**를 뽑아 일지에 첨부하라. (`sort -k... | tail` 같은 셸 한 줄이면 된다)
- **H2.** 오늘까지의 내용으로 **"BOMS 장애 대응 치트시트"** 초안(반 페이지)을 만들어라: 증상 → 확인 명령 → 판단 기준. 2주차가 끝나면 이 문서를 완성본으로 업데이트한다.

---

# 2주차 — HTTPS + 실전 아키텍처

---

## Day 6 — TLS 기초, mkcert 로컬 HTTPS

### 오늘의 개념

**TLS 핸드셰이크 (요지만)**: 클라이언트가 서버 인증서를 받아 → 신뢰하는 CA 체인으로 검증 → 키 교환 → 이후 대칭키 암호화 통신. 우리가 Nginx에서 다루는 건 "인증서/키를 어디 두고 어떻게 갱신하나"가 대부분이다.

**파일 3종의 정체**:
- `privkey.pem` — 서버 개인키. **유출되면 끝.** 권한 관리 대상.
- `cert.pem` — 내 서버 인증서 한 장.
- `fullchain.pem` — 내 인증서 + 중간 CA 인증서. **Nginx에는 이걸 준다.** cert만 주면 일부 클라이언트(특히 모바일/구형)가 체인 검증에 실패하는 미묘한 장애가 난다.

**mkcert의 원리**: 로컬에 사설 CA를 만들어 OS/브라우저 신뢰 저장소에 등록 → 그 CA로 서명한 인증서는 **내 기기에서만** 초록 자물쇠. 발급 절차(도메인 검증) 없이 443 블록 자체를 연습할 수 있는 이유.

### 시간 배분
- 0:00–0:30 개념 정리 (위 내용을 자기 말로 노트)
- 0:30–1:30 mkcert 발급 → 443 블록 구성 → 검증
- 1:30–2:00 일지 작성

### 실습

```bash
brew install mkcert
mkcert -install
mkcert localhost 127.0.0.1
# → localhost+1.pem / localhost+1-key.pem 생성
```

```yaml
# compose에 추가
    ports: ["8080:80", "8443:443"]
    volumes:
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
      - ./certs:/etc/nginx/certs:ro
```

```nginx
server {
    listen 443 ssl;
    http2 on;
    server_name localhost;

    ssl_certificate     /etc/nginx/certs/localhost+1.pem;
    ssl_certificate_key /etc/nginx/certs/localhost+1-key.pem;

    ssl_protocols TLSv1.2 TLSv1.3;

    location / { proxy_pass http://app-a:80; }
}
```

**검증 3종**
```bash
curl -v https://localhost:8443/           # 핸드셰이크 로그 읽기
openssl s_client -connect localhost:8443  # 체인/프로토콜 확인
```
+ 브라우저에서 자물쇠 클릭 → 인증서 뷰어로 발급자(mkcert CA) 확인.

**의도적 파괴**
1. cert와 key를 서로 바꿔 지정 → `nginx -t` 가 뭐라고 하나?
2. key 파일 경로를 틀리게 → Nginx가 뜨나? (**안 뜬다** — Day 8 "순서 문제"의 복선)

### 체크포인트
- [ ] fullchain / cert / privkey를 남에게 설명할 수 있다
- [ ] openssl s_client 출력에서 인증서 체인을 읽어봤다
- [ ] "pem 파일이 없으면 Nginx가 아예 안 뜬다"를 직접 확인했다

### 생각해볼 질문
1. TLS 종료(termination)를 Nginx에서 하고 뒤는 http로 통신하는 구성 — 어떤 전제에서 안전한가? Docker 네트워크는 그 전제를 만족하나?
2. `ssl_protocols`에서 TLSv1.0/1.1을 빼는 이유는? 반대로 못 빼는 상황은 어떤 경우일까?
3. mkcert 인증서를 팀 동료 PC에서 열면 경고가 뜬다. 왜인가? 이게 공인 CA 체계의 본질과 어떻게 연결되나?

### 숙제
- **H1.** `openssl x509 -in <fullchain.pem> -text -noout` 으로 인증서 내용을 열어 Subject / Issuer / SAN / 유효기간을 찾아 기록하라.
- **H2.** Let's Encrypt 인증서의 유효기간은 90일이다. 왜 짧게 설계했을지 자기 논리로 3줄 써보고, 내일 검색으로 답을 맞춰보라.

---

## Day 7 — HTTP → HTTPS 자동 리다이렉트, HSTS

### 오늘의 개념

**표준 패턴**: 80 전용 server 블록 + `return 301`. `rewrite ^ https://...`는 정규식 평가를 거치는 구식 스타일이다.

**301 vs 302 vs 308**:
- 301 — 영구 이동. 브라우저가 **캐싱**한다 (한번 301 받으면 다음부턴 서버에 안 물어보고 바로 https로 감).
- 302 — 임시. 캐싱 안 함. 테스트 단계에서 유용.
- 308 — 301 + **메서드 보존** (POST가 GET으로 안 바뀜). API에 POST 리다이렉트가 걸릴 수 있으면 308 고려.

**HSTS의 본질**: 첫 http 접속 한 번은 여전히 평문이다(중간자 공격 창구). HSTS는 "다음부터 max-age 동안 무조건 https로만 접속해"라고 브라우저에 각인 → 그 한 번마저 없애려는 게 `preload` 리스트. **브라우저에 각인되므로 서버 설정을 되돌려도 클라이언트는 계속 https를 강제한다** → 되돌리기 어려움의 정체.

### 시간 배분
- 0:00–0:40 리다이렉트 블록 구성 + 301 캐싱 체감
- 0:40–1:25 무한 리다이렉트 재현과 해결
- 1:25–1:45 HSTS 실험
- 1:45–2:00 일지 작성

### 실습

```nginx
server {
    listen 80;
    server_name localhost;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;        # Day 8에서 실제로 쓴다
    }

    location / {
        return 301 https://$host$request_uri;
    }
}
```

**실험 A — 리다이렉트 관찰**
```bash
curl -v http://localhost:8080/some/path?q=1     # 301 + Location 헤더 확인
curl -vL http://localhost:8080/some/path?q=1    # 따라가기 (-L). 쿼리 유지되나?
```
브라우저에서도 접속 → 개발자도구 Network에서 301 확인 → **"disable cache" 없이** 다시 접속하면 `(from disk cache)` 301이 나오는 것 확인. 301 캐싱을 눈으로.

**실험 B — 무한 리다이렉트 만들기 (오늘의 하이라이트)**
Spring 뒤에 두고:
1. `X-Forwarded-Proto`를 **안 넘기는** 상태에서 Spring이 `sendRedirect("/somewhere")` 또는 Spring Security의 https 요구 설정 → Spring은 자기가 http로 통신 중이라 믿음 → `http://`로 리다이렉트 → Nginx가 다시 301 https → 루프.
2. `curl -vL --max-redirs 5` 로 루프 확인 (브라우저는 `ERR_TOO_MANY_REDIRECTS`).
3. 헤더 추가 + `forward-headers-strategy: native` 로 해결. **전/후 curl 출력을 일지에 첨부.**

**실험 C — HSTS**
```nginx
add_header Strict-Transport-Security "max-age=300" always;   # 일부러 5분짜리
```
1. https로 한 번 접속 (HSTS 각인)
2. 이후 브라우저 주소창에 `http://...` 입력 → 개발자도구에서 **307 Internal Redirect** 확인 (서버에 요청이 아예 안 감!)
3. 5분 뒤 다시 http 접속 → 이번엔 서버 301 (각인 만료)
→ "서버가 아니라 브라우저가 강제한다"를 체감. `always` 플래그가 왜 필요한지도 조사(에러 응답에도 헤더를 붙이려면).

### 체크포인트
- [ ] 301 캐싱을 브라우저에서 확인했다
- [ ] 무한 리다이렉트를 만들고 → 원인을 설명하고 → 고쳤다
- [ ] HSTS의 "307 internal redirect"를 봤고, 되돌리기 어려운 이유를 브라우저 각인으로 설명할 수 있다

### 생각해볼 질문
1. 301 캐싱은 편리하지만, 리다이렉트 정책을 바꿔야 할 때 문제가 된다. 실서비스에서 안전하게 전환하려면 어떤 순서(302→301)로 가야 할까?
2. `.well-known` 예외를 301보다 **위에** 두는 게 나은가, 아래도 상관없나? (힌트: Day 1 location 우선순위 — prefix 길이)
3. HSTS `includeSubDomains`를 켜면 어떤 것까지 영향을 받나? BOMS처럼 서브도메인 여러 개를 운영할 가능성이 있다면 신중해야 하는 이유는?
4. `preload` 리스트에 올라가면 사실상 영구적이다. 홈서버에 preload는 적절한가?

### 숙제
- **H1.** 오늘 만든 무한 리다이렉트의 **요청 흐름 시퀀스 다이어그램**을 손으로 그려라 (클라이언트 ↔ Nginx ↔ Spring, 각 화살표에 프로토콜과 상태코드). 그릴 수 있으면 이해한 것이다.
- **H2.** 어제 숙제 H2(Let's Encrypt 90일의 이유)의 자기 답과 검색 답을 비교해 일지에 기록.
- **H3.** (선택) `return 301`과 `return 308`을 바꿔가며 POST 요청(`curl -X POST -L`)이 어떻게 달라지는지 실험.

---

## Day 8 — Let's Encrypt 실전 (certbot)

### 오늘의 개념

**ACME 프로토콜의 아이디어**: "이 도메인이 네 것임을 증명해봐."
- **HTTP-01** — LE 서버가 `http://도메인/.well-known/acme-challenge/토큰` 을 가져가본다. → 80 포트가 외부에서 열려 있어야 한다.
- **DNS-01** — 도메인의 TXT 레코드에 토큰을 넣는다. → 80 포트가 필요 없다. 와일드카드(`*.example.com`) 가능. CGNAT 등으로 포트를 못 여는 홈서버의 탈출구.

**닭-달걀 문제 (오늘의 최대 함정)**:
443 블록은 pem 파일을 참조한다 → pem이 없으면 `nginx -t` 실패 → Nginx가 안 뜬다 → 챌린지를 받을 80도 안 뜬다 → 발급이 안 된다 → pem이 없다 → ...

**정석 순서**:
1. **80 블록만** 있는 conf로 기동 (`.well-known` location 포함)
2. certbot webroot 방식으로 발급
3. pem 생성 확인 후 **443 블록 추가** → reload

**갱신의 3요소**: ① `certbot renew`가 주기적으로 돌아야 하고 ② 갱신 성공 시 ③ **Nginx가 reload** 되어야 한다 (Nginx는 pem을 부팅/reload 시점에만 읽는다 — 파일만 바뀌면 옛 인증서로 계속 서빙).

### 시간 배분
- 0:00–0:30 발급 순서 설계 + 도메인/포트포워딩 점검
- 0:30–1:20 발급 실행
- 1:20–1:45 갱신 자동화 구성 + dry-run
- 1:45–2:00 일지 작성

### 실습

**사전 점검 (Mac mini 홈서버)**
- [ ] 도메인 A레코드가 집 공인 IP를 가리키는가 (`dig +short 도메인`)
- [ ] 공유기 80/443 포트포워딩이 Mac mini로 향하는가
- [ ] ISP가 80 포트를 막지 않았는가 (`curl -I http://도메인` 을 외부망/폰 LTE로)
- [ ] CGNAT이면 HTTP-01은 불가 → DNS-01로 우회 (DNS 제공자 API 플러그인)

**발급**
```bash
mkdir -p certbot/conf certbot/www

# 1단계: 80 블록만으로 nginx 기동 (443 블록은 주석)

# 2단계: 발급
docker run --rm \
  -v ./certbot/conf:/etc/letsencrypt \
  -v ./certbot/www:/var/www/certbot \
  certbot/certbot certonly --webroot -w /var/www/certbot \
  -d yourdomain.example.com --email you@example.com --agree-tos --no-eff-email

# 3단계: 443 블록 주석 해제 → nginx -t → reload
```

compose의 nginx에 인증서 볼륨 추가:
```yaml
      - ./certbot/conf:/etc/letsencrypt:ro
      - ./certbot/www:/var/www/certbot:ro
```

**갱신 자동화** (compose에 certbot 서비스로 상주시키는 패턴)
```yaml
  certbot:
    image: certbot/certbot
    volumes:
      - ./certbot/conf:/etc/letsencrypt
      - ./certbot/www:/var/www/certbot
    entrypoint: >
      /bin/sh -c 'trap exit TERM;
      while :; do certbot renew --webroot -w /var/www/certbot; sleep 12h & wait $${!}; done'
```
+ 갱신 후 reload: 간단하게는 호스트 cron/launchd로 `certbot renew --deploy-hook "docker exec nginx nginx -s reload"` 방식도 가능. **둘 중 하나를 선택하고 이유를 일지에**.

**검증**
```bash
docker run --rm -v ./certbot/conf:/etc/letsencrypt certbot/certbot renew --dry-run
openssl s_client -connect 도메인:443 | openssl x509 -noout -dates   # 유효기간
```

### 체크포인트
- [ ] 닭-달걀 문제를 순서 설계로 회피했다 (또는 일부러 밟아보고 빠져나왔다)
- [ ] 실 도메인 발급 성공 + 브라우저 자물쇠 확인
- [ ] `renew --dry-run` 통과
- [ ] "갱신 후 reload"가 자동으로 도는 구조를 만들었고, 방식 선택의 이유를 적었다

### 생각해볼 질문
1. webroot 방식 대신 `--nginx` 플러그인(conf 자동 수정)도 있다. Docker 환경에서 왜 webroot가 더 선호될까?
2. 인증서 갱신이 조용히 실패하면 90일 뒤에야 알게 된다. **만료 7일 전에 알림**을 받으려면 어떻게 구성하겠는가? (힌트: 이미 아는 Uptime Kuma에 인증서 만료 모니터링 기능이 있다)
3. DNS-01로 와일드카드 인증서를 받으면 서브도메인 추가 때마다 재발급이 필요 없다. 반면 어떤 리스크가 생기나? (개인키 하나가 커버하는 범위)
4. Tailscale로만 접근하는 내부 서비스라면 공인 인증서가 필요한가? 대안은?

### 숙제
- **H1.** 오늘 만든 구성으로 **"인증서 운영 런북"** 반 페이지: 발급 절차 / 갱신 확인 명령 / 만료 임박 시 대응 / 완전 만료 시 복구 절차.
- **H2.** Uptime Kuma에 인증서 만료 모니터를 추가하고 스크린샷을 일지에.
- **H3.** (선택) `certbot certificates` 출력에서 어떤 정보를 읽을 수 있는지 정리.

---

## Day 9 — SPA 라우팅과 /api 프록시 분리

### 오늘의 개념

**SPA 404 문제의 본질**: React Router의 `/members/42` 는 **브라우저 안에서만** 존재하는 경로다. 새로고침하면 브라우저가 서버에 `GET /members/42` 를 실제로 보내고, Nginx 입장에서 그런 파일은 없다 → 404. `try_files $uri /index.html` 은 "파일이 있으면 주고, 없으면 index.html을 줘서 라우터가 처리하게 해라"는 뜻.

**같은 오리진의 힘**: 오리진 = 스킴+호스트+포트. Nginx가 `/`(SPA)와 `/api/`(백엔드)를 한 오리진으로 묶으면:
- CORS 설정이 **통째로 불필요**해진다 (cross-origin 요청 자체가 없으니까)
- 세션 쿠키가 아무 추가 설정 없이 동작한다 (`SameSite=Lax` 기본값으로 충분)
- CSRF는 여전히 신경 써야 한다 (쿠키 기반 인증의 숙명 — Spring Security CSRF를 이미 아는 게 여기서 힘이 된다)

**JWT vs 세션 쿠키, 오리진 관점 정리**:

| | 동일 오리진 (Nginx 통합) | 교차 오리진 (별도 도메인) |
|---|---|---|
| 세션 쿠키 | 그냥 됨. 추천 | SameSite=None; Secure + CORS credentials 지옥 |
| JWT (localStorage) | 가능하지만 XSS에 노출 | 가능, 그러나 굳이? |

→ **Nginx로 묶는 순간 세션 쿠키가 최선이 되는 경우가 많다.** BOMS의 인증 고민에 대한 유력한 답.

**개발/운영 경로 일치**: Vite dev 서버(5173)는 운영에 없다. dev에서는 `server.proxy`로 `/api` → 8080 을 흉내 내고, 운영에서는 Nginx가 같은 일을 한다. **`/api` prefix를 양쪽에서 동일하게** 유지하는 게 "dev에선 됐는데 운영에서 안 돼요"를 막는 핵심.

### 시간 배분
- 0:00–0:30 Vite 빌드 → Nginx 서빙
- 0:30–1:00 404 재현 → try_files 해결
- 1:00–1:30 /api 프록시 + 세션 쿠키 확인
- 1:30–1:45 캐시 전략 (Day 4 연결)
- 1:45–2:00 일지 작성

### 실습

**구성**
```bash
npm create vite@latest spa-test -- --template react-ts
cd spa-test && npm i && npm run build   # → dist/
```

```nginx
server {
    listen 443 ssl;
    # ... ssl 설정 (Day 6~8)

    # API — 우선 매칭되도록
    location /api/ {
        proxy_pass http://boms:8080;      # 끝 슬래시 없음 = 경로 그대로 전달 (Day 2!)
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 해시된 정적 자원 — 강캐싱
    location /assets/ {
        root /usr/share/nginx/html;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # SPA 라우팅
    location / {
        root /usr/share/nginx/html;
        try_files $uri /index.html;
        add_header Cache-Control "no-cache";   # index.html은 항상 재검증
    }
}
```

**실험 A — 404 재현이 먼저**
try_files **없이** React Router 경로 하나 만들어 새로고침 → 404 직접 보기 → try_files 추가 → 해결. (해결부터 하면 배우는 게 없다)

**실험 B — Day 2 복습이 실전으로**
`proxy_pass http://boms:8080;` (슬래시 없음) 인 이유를 슬래시 매트릭스로 설명해보라. 만약 `http://boms:8080/;` 로 쓰면 백엔드가 받는 경로는? Spring 컨트롤러 매핑에 어떤 영향?

**실험 C — 세션 쿠키 관찰**
로그인 요청 후 개발자도구 → Application → Cookies에서 `JSESSIONID` 확인: `HttpOnly`? `Secure`? `SameSite`? 이후 `/api/` 호출에 쿠키가 자동 동봉되는 것 확인. **CORS 에러가 한 번도 안 나는 것** 자체가 관찰 포인트.

**실험 D — Vite dev proxy**
```ts
// vite.config.ts
server: { proxy: { '/api': 'http://localhost:8080' } }
```
dev(5173)에서도 같은 `/api` 코드가 동작하는 것 확인 → "경로 일치" 원칙 체감.

### 체크포인트
- [ ] SPA 새로고침 404를 재현→해결했다
- [ ] `/api/` proxy_pass의 슬래시 선택을 Day 2 지식으로 설명할 수 있다
- [ ] 세션 쿠키의 HttpOnly/Secure/SameSite를 확인했고, CORS 설정이 왜 필요 없는지 설명할 수 있다
- [ ] index.html은 no-cache, 해시 자원은 immutable — 이 조합의 이유를 설명할 수 있다

### 생각해볼 질문
1. `try_files $uri $uri/ /index.html` 처럼 `$uri/` 를 끼우는 변형도 흔하다. 뭐가 달라지나? 언제 필요한가?
2. index.html을 강캐싱하면 무슨 일이 벌어지나? (Day 4 실험 D + 해시 자원의 관계로 설명)
3. 모노레포에서 `task_management_front/` 빌드 산출물을 Nginx 컨테이너에 넣는 방법이 여러 개다: ① Nginx 이미지에 COPY (멀티스테이지 빌드) ② 볼륨 마운트 ③ 별도 프론트 컨테이너. BOMS의 GitHub Actions 배포 파이프라인과 각각 어떻게 맞물리는지 장단점을 따져보라.
4. Thymeleaf 페이지와 React SPA가 **한동안 공존**해야 한다면(점진적 전환) Nginx location을 어떻게 설계하겠는가? 예: `/legacy/`는 Spring 렌더링, `/app/`은 SPA.

### 숙제
- **H1.** 질문 3에 대한 결론을 내리고, BOMS deploy.yml에 어떤 스텝이 추가/변경되어야 하는지 목록을 만들어라. (다음 BOMS 작업의 실제 입력물이 된다)
- **H2.** 질문 4의 점진적 전환 시나리오로 location 설계 초안을 conf로 작성해보라 (동작 안 시켜도 됨, 설계만).
- **H3.** (선택) `error_page 404 /index.html;` 로도 SPA 라우팅을 흉내 낼 수 있다. try_files와 뭐가 다른가? (상태코드 관점)

---

## Day 10 — SSE · WebSocket · rate limit · BOMS 최종 적용

### 오늘의 개념

**SSE와 버퍼링의 충돌**: Nginx는 기본으로 백엔드 응답을 버퍼에 모았다가 내보낸다(`proxy_buffering on`) — 느린 클라이언트로부터 백엔드를 빨리 해방시키는 좋은 설계다. 그런데 SSE는 "조금씩 계속 흘려보내는" 응답이라 버퍼링과 정면충돌한다: 버퍼가 찰 때까지 클라이언트에 아무것도 안 간다. → Saveface의 Claude 스트리밍 응답이 "한참 조용하다가 한 번에 쏟아지는" 증상의 원인이 될 수 있다.
- `proxy_buffering off` — Nginx 설정으로 끄기
- `X-Accel-Buffering: no` — **백엔드가 응답 헤더로** 끄기 (Spring에서 SSE 응답에만 선택적으로 붙일 수 있어 더 정교)

**WebSocket과 프록시**: WS는 http로 시작해 `Upgrade` 핸드셰이크로 프로토콜을 전환한다. 문제는 ① Nginx가 백엔드와 기본 HTTP/1.0으로 통신(Upgrade는 1.1 필요) ② `Upgrade`/`Connection`은 hop-by-hop 헤더라 프록시가 전달 안 함. → 두 줄이 필수인 이유:
```nginx
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```
+ WS는 오래 사는 연결이므로 `proxy_read_timeout`을 길게 (기본 60초면 유휴 연결이 끊긴다).

**rate limit의 leaky bucket**: `rate=10r/s`는 "100ms당 1개"로 균등하게 검사한다. 순간 burst를 허용하려면 `burst=20`(대기열), 대기열을 지연 없이 통과시키려면 `nodelay`. 초과분은 503 (`limit_req_status 429;`로 바꾸는 게 API에는 더 정직하다).

### 시간 배분
- 0:00–0:40 SSE 버퍼링 재현 → 해결
- 0:40–1:05 WebSocket 프록시
- 1:05–1:25 rate limit 실험
- 1:25–2:00 **BOMS 최종 conf 작성 + 코스 총정리**

### 실습

**실험 A — SSE (재현이 먼저!)**
Spring에 SSE 엔드포인트 (`SseEmitter` 또는 Flux, 1초 간격 5개 이벤트):
1. **기본 설정(buffering on)으로 먼저** `curl -N https://.../api/stream` → 5초 침묵 후 한꺼번에 쏟아지는 것 확인
2. 해결 적용:
```nginx
location /api/stream/ {
    proxy_pass http://boms:8080;
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 3600s;
    proxy_set_header Connection "";
}
```
3. 다시 curl → 1초마다 흘러나오는 것 확인
4. (심화) buffering을 다시 켜고 Spring 응답에 `X-Accel-Buffering: no` 헤더만 추가 → 같은 효과 확인. **어느 쪽이 BOMS/Saveface에 맞는 설계인지 결론 내리기.**

**실험 B — WebSocket**
`websocat` 또는 브라우저 콘솔 `new WebSocket(...)` 으로:
1. Upgrade 헤더 **없이** 연결 시도 → 실패 양상 기록 (400? 즉시 종료?)
2. 두 줄 추가 → 성공
3. 61초 이상 유휴 방치 → 끊기나? → read_timeout 조정 or ping/pong 논의

**실험 C — rate limit**
```nginx
limit_req_zone $binary_remote_addr zone=api:10m rate=5r/s;
location /api/ {
    limit_req zone=api burst=10 nodelay;
    limit_req_status 429;
    ...
}
```
```bash
for i in $(seq 1 30); do curl -s -o /dev/null -w "%{http_code} " https://.../api/ping; done
```
→ 200과 429의 분포 확인. `burst`와 `nodelay`를 빼가며 분포가 어떻게 변하는지 3회 비교.

**최종 과제 — BOMS conf 완성**
지금까지의 전부를 반영한 BOMS nginx.conf를 작성한다. 구성 요소 체크리스트:
- [ ] 80 → 443 리다이렉트 + `.well-known` 예외 (Day 7,8)
- [ ] TLS + fullchain + 프로토콜 제한 (Day 6,8)
- [ ] 프록시 헤더 4종 + Spring forward-headers (Day 3)
- [ ] client_max_body_size / 타임아웃 (Day 4)
- [ ] perf 로그 포맷 (Day 5)
- [ ] SPA try_files + /api 분리 + 캐시 전략 (Day 9) — 전환 전이라면 설계 주석으로
- [ ] SSE location (Day 10) — Saveface 대비 주석 포함
- [ ] resolver + 변수 proxy_pass 또는 healthcheck 기반 기동 순서 (Day 2)

그리고 **모든 줄에 "왜 있는지" 주석**을 단다. 설명 못 하는 줄이 남아 있으면 그게 마지막 학습 주제다.

### 체크포인트
- [ ] SSE "한꺼번에 쏟아짐"을 재현하고 두 가지 방법으로 해결했다
- [ ] WS 실패→성공 양상을 기록했다
- [ ] rate limit 파라미터 3회 비교표를 만들었다
- [ ] 전 줄 주석 달린 BOMS 최종 conf가 나왔다

### 생각해볼 질문
1. `proxy_buffering off`를 **전역으로** 꺼버리면 편할 텐데, 왜 SSE location에만 국한하는 게 좋은가? (느린 클라이언트와 백엔드 스레드 점유 관점)
2. rate limit 키를 `$binary_remote_addr`로 잡았다. Day 3의 XFF 논의와 연결하면, CDN/프록시 뒤에서는 이 키가 왜 위험한가?
3. SSE 연결이 수백 개로 늘면 Nginx와 Spring(스레드 모델) 중 어디가 먼저 한계에 오나? Saveface를 WebFlux로 갈지 MVC+SseEmitter로 갈지에 어떤 시사점이 있나?
4. 이 코스에서 배운 것 중 **Kubernetes Ingress로 넘어가면 그대로 통하는 개념**과 **Nginx 고유라서 버려야 하는 지식**을 나눠보라.

### 숙제 (코스 마무리)
- **H1.** Day 5에서 초안 잡은 **"BOMS 장애 대응 치트시트"를 완성본으로 업데이트** (HTTPS/SPA/SSE 장애 유형 추가).
- **H2.** 10일 전체를 돌아보며 **"내가 3번 이상 틀렸거나 헷갈린 것 Top 3"** 를 뽑고, 각각에 대해 미래의 나에게 남기는 한 문단 경고문을 써라.
- **H3.** 최종 conf를 BOMS에 실제 반영하고, 반영 전/후 접속 테스트 결과를 일지에. (반영은 트래픽 적은 시간대에)
- **H4. (관통 질문 최종 결산)** 코스 첫머리의 "Docker가 해결하는 문제 4가지" 가설표로 돌아가, 각 항목에 대해 **검증됨 / 부분 검증 / 과장** 판정과 근거(어느 Day의 어떤 경험)를 적어라. 그리고 Day 0에서 기록한 "직접 설치에서 가장 귀찮았던 것"을 다시 읽고, 지금의 자신이 그때의 자신에게 해줄 설명을 한 문단으로 써라. 마지막으로 **"나는 언제 Docker를 쓰지 않을 것인가"**에 대한 자기 기준 3줄 — 이 3줄이 있어야 도구를 아는 것이지 도구를 믿는 것이 아니다.

---

# 학습 일지 템플릿

> 매일 이 블록을 복사해 채우고, Claude에게 붙여넣어 피드백을 요청한다.

```markdown
## Day N — [주제]
- 날짜:
- 실제 투입 시간:

### 오늘 한 것
-

### 예측 vs 실제 (예측 실험이 있는 날)
| 실험 | 내 예측 | 실제 결과 | 틀렸다면 왜? |
|---|---|---|---|

### 동작한 것 (성공)
-

### 막혔던 것 / 에러
- 증상:
- 에러 로그 (원문 붙여넣기):
- 시도한 것 (순서대로):
- 해결 여부와 근거:

### 새로 알게 된 것
-

### 생각해볼 질문에 대한 내 답
1.
2.

### 오늘의 "Docker가 없었다면?" 답변 (2~4문장)
> 관통 질문 표에서 오늘 Day의 질문에 답한다. 오늘 체감한 "Docker가 해결하는 문제 4가지" 중 해당 칸 번호도 표시.

### 숙제 결과
- H1:
- H2:

### 스스로 설명해보기 (한 줄로)
> 오늘 배운 걸 동료에게 설명한다면?

### 체크포인트
- [ ]

### 남은 의문
-
```

---

# Claude의 판정 기준 (미리 공개)

일지를 받으면 다음 관점으로 피드백한다. 어떤 걸 보는지 알고 있으면 기록의 질이 올라간다.

| 항목 | 판정 포인트 |
|---|---|
| **예측 습관** | 실험 전에 예측을 적었는가. 틀린 예측을 분석했는가 (틀린 예측이 하나도 없으면 오히려 의심 — 쉬운 것만 한 것) |
| **재현 깊이** | 성공만 했는가, **의도적으로 깨뜨려보기도** 했는가 |
| **인과 설명** | "됐다"가 아니라 **왜 됐는지**를 자기 말로 설명하는가 |
| **로그 활용** | 추측으로 고쳤는가, **로그 원문을 근거로** 고쳤는가 |
| **질문 응답** | 생각해볼 질문에 자기 논리로 답했는가 (정답 여부보다 논증 과정) |
| **숙제 수행** | 숙제가 다음 날 일지에 이어지는가 (연속성) |
| **BOMS 연결** | 샌드박스 결과를 실제 프로젝트에 적용할 판단이 서는가 |
| **왜 Docker인가** | 그날의 관통 질문에 "장점 찬양"이 아니라 **비교와 트레이드오프**로 답했는가. Docker의 단점·한계를 언급한 답이 오히려 높은 점수 |
| **의문 남기기** | "남은 의문"이 비어 있으면 대개 표면만 훑은 것 |

**성공 판정**: 체크포인트 완료 + 예측/실제 표 성실 + 질문에 자기 논리 답변 + 관통 질문에 트레이드오프 기반 답변 + 남은 의문이 구체적.
**보완 신호**: 에러 없이 술술 끝났는데 배운 게 얇다 / 해결은 했는데 원인을 모른다 / 복붙만 하고 값을 바꿔보지 않았다 / 생각해볼 질문을 건너뛰었다 / "Docker는 편하다"류의 결론만 있고 근거 비교가 없다.
**중단 신호**: 같은 유형의 막힘이 3일 연속이면 진도를 멈추고 해당 주제만 하루 더 (코스는 지도일 뿐, 이해가 우선).
