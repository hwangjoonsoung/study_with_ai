# study_with_ai

Docker와 nginx 리버스 프록시를 학습하기 위한 실습용 저장소입니다.

## 구성

- **nginx** (`nginx:alpine`): `8080:80` 포트로 노출되는 리버스 프록시. `nginx.conf`를 읽어 경로 기반으로 백엔드로 요청을 전달합니다.
- **app-a / app-b** (`traefik/whoami`): 요청 정보를 그대로 응답해 주는 백엔드 컨테이너.

경로 라우팅:

| 경로   | 프록시 대상 |
|--------|-------------|
| `/a/`  | `app-b`     |
| `/b/`  | `app-b`     |

## 실행

```bash
docker compose up -d
```

기동 후 브라우저나 curl로 확인합니다.

```bash
curl http://localhost:8080/a/
curl http://localhost:8080/b/
```

종료:

```bash
docker compose down
```

## 문서

- `docs/docker/docker-nginx-10day-course.md` — Docker & nginx 10일 학습 코스
- `docs/network/network-10day-course.md` — 네트워크 10일 학습 코스
