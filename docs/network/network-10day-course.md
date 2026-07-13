# 네트워크 10일 체험 코스 (1일 2시간) — 패킷으로 배우는 네트워크

> **사용법**
> 1. 매일 해당 Day의 **개념 → 실험 → 생각해볼 질문 → 숙제** 순으로 진행한다.
> 2. 하단 **학습 일지 템플릿**을 복사해 기록하고, Claude에게 붙여넣어 판정/조언을 받는다.
> 3. Nginx 코스와 병행 가능 — 각 Day 말미의 **[Nginx 연결]** 표시를 참고.
>
> **핵심 원칙**
> - 모든 개념은 **패킷/소켓을 직접 관측**해서 확인한다. 안 보이면 아직 모르는 것이다.
> - 실험 전에 **예측을 먼저** 적는다. 틀린 예측이 가장 많이 가르쳐준다.
> - 최종 목표는 한 문장이다: **"브라우저에 URL을 치는 순간부터 MySQL 소켓까지, 한 요청의 전 생애를 구간별로 관측하고 설명할 수 있다."**

---

## 준비물 (Day 0, 30분)

```bash
# Mac (호스트)
brew install wireshark          # GUI 포함
brew install mtr                # traceroute 개선판
brew install websocat           # WS 테스트용 (Nginx 코스 공용)

# 확인
tcpdump --version
dig -v
nc -h
curl --version
```

curl 타이밍 분해용 포맷 파일 (여러 Day에서 재사용):

```bash
cat > ~/curl-format.txt <<'EOF'
   namelookup:  %{time_namelookup}s
      connect:  %{time_connect}s
   appconnect:  %{time_appconnect}s
     starttx :  %{time_pretransfer}s
    firstbyte:  %{time_starttransfer}s
        total:  %{time_total}s
EOF
# 사용: curl -w "@$HOME/curl-format.txt" -o /dev/null -s https://example.com
```

Docker 컨테이너 안에서 패킷을 잡을 때는 별도 컨테이너로 네트워크를 공유하는 패턴을 쓴다 (여러 Day에서 사용):

```bash
docker run --rm -it --net container:<대상컨테이너> nicolaka/netshoot tcpdump -i eth0 -nn
```

`netshoot`은 tcpdump/dig/ss/curl/iperf가 전부 든 네트워크 디버깅 전용 이미지다. 외워둘 가치가 있다.

---

## 코스 전체 지도

```
[1주차 — 전송 계층과 이름 해석]
Day 1  TCP 생애주기 — handshake부터 종료까지
Day 2  소켓과 포트 — ss/lsof/nc, TIME_WAIT와 CLOSE_WAIT
Day 3  DNS — 계층 위임부터 Docker 내장 DNS까지
Day 4  NAT — 공유기, Docker 포트매핑, iptables의 정체
Day 5  라우팅과 경로 — traceroute/mtr, 1주차 종합

[2주차 — 보안 계층과 응용 계층, 그리고 실전]
Day 6  TLS 해부 — handshake, 체인, MITM
Day 7  HTTP 시맨틱스 — keepalive, chunked, 캐시 재검증
Day 8  커넥션 풀과 TCP — HikariCP를 패킷으로 다시 보기
Day 9  불안정한 네트워크 — tc로 지연/손실 주입, 재시도와 멱등성
Day 10 종합 — 한 요청의 전 생애 추적 + Tailscale 원리
```

| Day | 산출물 | BOMS/실무 연결점 |
|-----|--------|-----------------|
| 1 | handshake/종료 패킷 캡처 + 주석 | 502의 패킷 레벨 정체(RST) 이해 |
| 2 | TCP 상태 치트시트 | "포트 사용 중" / 커넥션 릭 즉시 진단 |
| 3 | DNS 해석 경로 그림 | Docker 서비스명 해석, 도메인 운영 |
| 4 | NAT 흐름도 (공유기+Docker) | 포트포워딩과 포트매핑이 같은 원리임을 증명 |
| 5 | 경로 추적 리포트 | "느리다"의 위치 특정 |
| 6 | TLS handshake 캡처 + 체인 노트 | Let's Encrypt 운영(Nginx Day 8)의 원리층 |
| 7 | HTTP 헤더 관찰 노트 | SSE(chunked), 캐시(ETag) 원리 |
| 8 | 커넥션 풀 관측 리포트 | 과거 HikariCP 고갈 사건의 완전한 재해석 |
| 9 | 카오스 실험 리포트 | 타임아웃 설정값의 근거 확보, 결제 재시도 설계 |
| 10 | "한 요청의 전 생애" 문서 | 최종 통합 — 면접 단골 질문의 실전판 |

---

# 1주차 — 전송 계층과 이름 해석

---

## Day 1 — TCP 생애주기

### 오늘의 개념

TCP 연결은 태어나고(3-way handshake), 일하고(데이터 전송 + ACK), 죽는다(4-way 종료 또는 RST 급사). 오늘은 이 생애 전체를 눈으로 본다.

**3-way handshake**: `SYN → SYN/ACK → ACK`. 왜 3번인가 — 양쪽 모두 "내 말이 상대에게 닿았음"을 확인해야 하기 때문(시퀀스 번호 동기화). 이게 곧 **연결 수립 = 최소 1 RTT의 비용**이라는 뜻이고, keepalive와 커넥션 풀이 존재하는 근본 이유다.

**정상 종료 vs 급사**:
- `FIN → ACK → FIN → ACK` (4-way) — "할 말 다 했어" 합의 종료
- `RST` — "그런 연결 모르는데?" 일방적 리셋. 닫힌 포트에 접속하거나, 프로세스가 죽은 소켓에 데이터가 오면 발생. **Nginx 502의 패킷 레벨 정체가 대부분 이것.**

**시퀀스/ACK 번호**: 모든 바이트에 번호가 붙는다. ACK는 "여기까지 잘 받았고 다음은 N번을 기다림"이다. 재전송·순서보장·중복제거가 전부 이 번호 하나로 돌아간다.

### 시간 배분
- 0:00–0:20 tcpdump 출력 읽는 법 익히기
- 0:20–1:00 handshake / 정상 종료 관측
- 1:00–1:40 RST 관측 (의도적 파괴)
- 1:40–2:00 일지 작성

### 실험

**실험 A — 첫 캡처**

```bash
# 터미널 1
sudo tcpdump -i lo0 port 8080 -nn -S
# 터미널 2 (Nginx 샌드박스가 8080에 떠 있는 상태)
curl -s localhost:8080/a/ > /dev/null
```

출력에서 찾을 것: `Flags [S]`(SYN), `[S.]`(SYN/ACK), `[.]`(ACK), `[P.]`(PSH+ACK, 데이터), `[F.]`(FIN). **각 줄이 뭔지 주석을 단 캡처를 일지에 첨부**하는 게 오늘의 산출물.

**실험 B — Wireshark로 재관찰**
같은 트래픽을 Wireshark로 잡고:
1. Statistics → Flow Graph 로 시퀀스 다이어그램 자동 생성 확인
2. 아무 패킷 우클릭 → Follow → TCP Stream → **HTTP가 평문 텍스트로 그대로 보이는 것** 확인. (Day 6에서 HTTPS로 같은 걸 하면 아무것도 안 보인다 — 이 대비가 TLS 학습의 축이다)

**실험 C — RST 만들기 (의도적 파괴)**

```bash
# 예측 먼저: 아무도 안 듣는 포트에 접속하면 패킷 레벨에서 무슨 일이?
curl localhost:59999
# tcpdump로 관측: SYN → RST
```

이어서 Nginx 샌드박스에서 `docker stop app-a` 후 `/a/` 요청 → nginx와 app-a 사이 구간을 netshoot으로 잡아 **RST**를 확인하고, 클라이언트가 받는 것은 **502**임을 대조. "커넥션 거부"가 계층을 넘으며 어떻게 번역되는지가 포인트.

**실험 D — handshake 비용 체감**

```bash
# 매번 새 연결
for i in 1 2 3; do curl -s -o /dev/null -w "%{time_connect} %{time_total}\n" http://example.com; done
# 재사용 (한 프로세스에서 두 URL)
curl -s -o /dev/null -w "%{time_total}\n" http://example.com http://example.com
```

`curl -v` 출력의 `Re-using existing connection` 문구도 확인.

### 체크포인트
- [ ] SYN/SYN-ACK/ACK/FIN/RST를 tcpdump 출력에서 즉시 식별할 수 있다
- [ ] Follow TCP Stream으로 HTTP 평문을 봤다
- [ ] RST와 FIN의 차이를 "누가 왜 보내는지"로 설명할 수 있다
- [ ] handshake 재사용 유무의 시간 차이를 수치로 기록했다

### 생각해볼 질문
1. handshake가 2-way면 무슨 문제가 생기나? (옛 중복 SYN이 도착하는 시나리오를 상상해보라)
2. UDP는 이 모든 게 없다. 그런데 DNS와 HTTP/3(QUIC)는 UDP를 쓴다. 무엇을 포기하고 무엇을 얻은 건가?
3. `localhost` 대상 tcpdump에서는 재전송이나 손실을 거의 볼 수 없다. 왜인가? (Day 9에서 인위적으로 만들게 된다)

### 숙제
- **H1.** `sudo tcpdump -i any port 3306 -nn` 을 켜둔 채 BOMS(또는 로컬 Spring)를 기동해보라. HikariCP가 풀을 채우는 순간 handshake가 **몇 개** 보이는가? pool size 설정값과 일치하는가? (Day 8의 복선)
- **H2.** 오늘 캡처에서 시퀀스 번호를 따라가며 "클라이언트가 보낸 총 바이트"를 계산해보고, 실제 요청 크기와 맞는지 검증하라.

**[Nginx 연결]** Nginx Day 5의 502를 패킷으로 다시 보면 오늘의 RST다.

---

## Day 2 — 소켓과 포트, TCP 상태 머신

### 오늘의 개념

**소켓 = (프로토콜, 로컬IP:포트, 원격IP:포트)의 5-tuple.** 서버가 80 포트 "하나"로 수만 클라이언트를 받을 수 있는 이유 — 연결은 원격 주소까지 포함한 조합으로 구분되기 때문이다.

**TCP 상태 머신에서 실무에 중요한 3가지**:
- `LISTEN` — 서버가 대기 중. `ss -tlnp`로 보는 것.
- `TIME_WAIT` — **먼저 끊은 쪽**이 잠시(수십 초) 머무는 상태. 늦게 도착하는 패킷이 다음 연결을 오염시키지 않게 하는 안전장치. 많아 보여도 대부분 정상.
- `CLOSE_WAIT` — 상대가 FIN을 보냈는데 **내가 close()를 안 한** 상태. **이게 쌓이면 100% 애플리케이션 버그(리소스 릭)다.** 커넥션 릭 진단의 핵심 시그널.

**도구 3종**:
```bash
ss -tlnp              # 리스닝 소켓 + 프로세스 (netstat의 후계자)
ss -tnp state established '( dport = :3306 )'   # 조건 필터
lsof -i :8080         # 이 포트를 잡은 프로세스 (Mac에서 특히 유용)
nc -l 9999            # 즉석 서버 / nc host port 는 즉석 클라이언트
```

### 시간 배분
- 0:00–0:30 도구 3종 손에 익히기
- 0:30–1:00 nc로 HTTP를 손으로 쳐보기
- 1:00–1:40 TIME_WAIT / CLOSE_WAIT 재현
- 1:40–2:00 일지 작성

### 실험

**실험 A — HTTP를 손으로**

```bash
# 터미널 1: 즉석 서버
nc -l 9999
# 터미널 2
curl localhost:9999/hello
```

터미널 1에 찍히는 요청 원문을 관찰 → **HTTP는 그냥 텍스트다.** 이제 반대로: 터미널 1에서 직접 응답을 타이핑해보라.

```
HTTP/1.1 200 OK
Content-Length: 3

hi
```

curl이 정상 응답으로 받아들이는가? `Content-Length`를 5로 틀리게 주면? (curl이 기다리다가 이상해지는 것 — Day 7 chunked의 복선)

**실험 B — 5-tuple 확인**
브라우저 탭 여러 개로 같은 사이트를 열고 `ss -tn dst :443` → 로컬 포트가 전부 다른 것 확인. "클라이언트 포트는 임시 포트(ephemeral)"의 실물.

**실험 C — CLOSE_WAIT 만들기 (오늘의 하이라이트)**

close()를 안 하는 서버를 하나 만든다:

```python
# leak_server.py — 받고 close 안 함
import socket
s = socket.socket(); s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
s.bind(("0.0.0.0", 9999)); s.listen()
conns = []
while True:
    c, _ = s.accept()
    conns.append(c)          # 절대 close하지 않는다
```

```bash
python3 leak_server.py &
curl --max-time 2 localhost:9999   # curl이 타임아웃으로 먼저 끊음(FIN)
ss -tn state close-wait            # 서버 쪽에 CLOSE_WAIT 잔류 확인
```

curl을 10번 반복 → CLOSE_WAIT 10개 누적 확인. **"CLOSE_WAIT 누적 = 상대는 끊었는데 내 코드가 close를 안 함"** 을 몸에 새긴다.

**실험 D — TIME_WAIT 관찰**
`curl localhost:8080` 직후 `ss -tn state time-wait` → 먼저 끊은 쪽(보통 클라이언트)에 TIME_WAIT. 왜 서버가 아니라 클라이언트 쪽에 있는지 생각해보기.

### 체크포인트
- [ ] ss/lsof/nc를 문서 없이 쓸 수 있다
- [ ] nc로 HTTP 응답을 손으로 타이핑해서 curl을 속여봤다
- [ ] CLOSE_WAIT를 의도적으로 만들었고, 원인을 코드 줄 단위로 지목할 수 있다
- [ ] TIME_WAIT가 왜 정상이고 언제 문제인지 구분할 수 있다

### 생각해볼 질문
1. TIME_WAIT가 대량으로 쌓여 임시 포트가 고갈되는 건 어떤 워크로드에서 발생하나? (힌트: 짧은 연결을 초당 수천 개 만드는 쪽은 누구인가 — 서버? 클라이언트? 프록시?) Nginx→백엔드 구간에 keepalive를 거는 것과 어떻게 연결되나?
2. `SO_REUSEADDR`가 없으면 서버 재시작 직후 "Address already in use"가 나는 이유를 TIME_WAIT로 설명해보라.
3. 과거 겪었던 MySQL `Threads_connected` 증가 사건 — 그때 서버에서 `ss`를 쳤다면 어느 상태의 소켓이 보였을 것 같은가? (Day 8에서 실제로 검증한다)

### 숙제
- **H1.** **TCP 상태 치트시트**를 만들어라: 상태명 / 의미 / 어느 쪽에 생기나 / 많으면 정상인가 버그인가 / 확인 명령. LISTEN, ESTABLISHED, TIME_WAIT, CLOSE_WAIT, FIN_WAIT_2, SYN_SENT 6개면 충분.
- **H2.** BOMS 운영 서버에서 `ss -s` (요약 통계)를 찍어 상태별 분포를 기록하라. 이상 신호가 있는가?
- **H3.** (선택) `nc`로 SMTP나 Redis 같은 다른 텍스트 프로토콜에 접속해 명령을 손으로 쳐보라. "프로토콜 = 약속된 텍스트 대화"라는 감각이 강해진다.

---

## Day 3 — DNS

### 오늘의 개념

**DNS는 계층적 위임 시스템**이다: 루트(.) → TLD(.com) → 권한 서버(example.com) 순으로 "그건 쟤한테 물어봐"를 반복한다. 우리가 평소 빠르다고 느끼는 건 중간의 **재귀 리졸버**(ISP나 8.8.8.8)가 캐싱해주기 때문.

**레코드 타입 최소 세트**: `A`(IPv4), `AAAA`(IPv6), `CNAME`(별명 — 다른 이름으로 가서 다시 찾아), `TXT`(자유 텍스트 — Let's Encrypt DNS-01이 쓰는 곳), `NS`(위임), `MX`(메일).

**TTL**: 캐시 유효시간. 도메인을 옮길 때 "전파가 느리다"는 말의 정체는 전파가 아니라 **각지의 캐시가 만료되길 기다리는 것**이다. 이사 전 TTL을 미리 줄여두는 게 실무 요령.

**Docker의 127.0.0.11**: 컨테이너 안 `/etc/resolv.conf`에 박혀 있는 내장 DNS. compose 서비스명을 컨테이너 IP로 해석해준다. Nginx Day 2에서 배운 `resolver 127.0.0.11`의 정체가 오늘 완성된다.

### 시간 배분
- 0:00–0:40 dig 기본기 + 위임 추적
- 0:40–1:10 캐시/TTL 실험
- 1:10–1:40 Docker DNS 해부
- 1:40–2:00 일지 작성

### 실험

**실험 A — 위임 경로 전체 보기**

```bash
dig +trace naver.com
```

출력을 위에서부터 읽으며 **루트 → .com TLD → 권한 서버 → A 레코드** 4단계를 구분해 그림으로 그린다(오늘의 산출물). 이어서:

```bash
dig naver.com            # 재귀 리졸버 경유 (평소의 방식)
dig @8.8.8.8 naver.com   # 리졸버 지정
dig +short 본인도메인     # 홈서버 A레코드 확인
```

**실험 B — TTL과 캐시**

```bash
dig naver.com | grep -A1 "ANSWER SECTION"
# 몇 초 후 다시 → TTL 숫자가 줄어드는 것 확인 (캐시에서 응답 중이라는 증거)
```

같은 질의를 tcpdump로 관측 (`sudo tcpdump -i any port 53 -nn`): 첫 질의는 패킷이 나가고, 캐시 히트면 안 나가는 것 확인. **DNS가 UDP인 것**도 여기서 눈으로 확인된다.

**실험 C — Docker DNS 해부**

```bash
docker exec <nginx컨테이너> cat /etc/resolv.conf     # nameserver 127.0.0.11
docker run --rm --net <compose네트워크> nicolaka/netshoot dig app-a
docker run --rm --net <compose네트워크> nicolaka/netshoot dig +short app-a
```

`app-a`의 IP가 나오는 것 확인 → `docker network inspect`의 IP와 대조. 이어서 `docker compose up -d --force-recreate app-a` 후 다시 dig → **IP가 바뀌는 것** 확인. Nginx가 부팅 시 캐싱한 IP가 왜 죽은 주소가 되는지(Nginx Day 2 실험 D)의 원리 완성.

**실험 D — /etc/hosts 우선순위**
`/etc/hosts`에 `127.0.0.1 fake.example.com` 추가 → `curl fake.example.com:8080` 이 로컬로 가는 것 확인 → **이름 해석은 DNS만이 아니다** (hosts → DNS 순서). 로컬 개발에서 운영 도메인을 흉내 낼 때 쓰는 실전 기법.

### 체크포인트
- [ ] +trace 출력을 4단계 위임으로 그림 그렸다
- [ ] TTL 감소와 "질의 패킷이 안 나가는 캐시 히트"를 관측했다
- [ ] Docker 내장 DNS에 직접 질의해봤고, 컨테이너 재생성 시 IP 변경을 확인했다
- [ ] hosts 파일 우선순위를 활용할 수 있다

### 생각해볼 질문
1. DNS가 UDP를 기본으로 쓰는 이유는? 어떤 경우에 TCP로 전환되나? (힌트: 응답 크기, 그리고 존 전송)
2. CNAME 체인이 길면 뭐가 나빠지나? apex 도메인(example.com 자체)에 CNAME을 못 거는 제약은 왜 있을까?
3. 홈서버 도메인의 TTL을 60초로 하는 것과 24시간으로 하는 것 — 각각 언제 유리한가? 집 IP가 유동 IP라면?
4. DNS 질의는 평문 UDP다. 카페 와이파이에서 무엇이 노출되나? DoH(DNS over HTTPS)가 해결하는 것과 해결하지 못하는 것은?

### 숙제
- **H1.** 본인 도메인의 전체 레코드 상태를 정리하라 (`dig 도메인 ANY` 는 요즘 잘 안 통하니 A/AAAA/CNAME/TXT/MX/NS를 각각 질의). Let's Encrypt를 DNS-01로 전환한다면 어떤 레코드를 추가하게 되는지도 조사.
- **H2.** `dig +trace`를 안 쓰고, `dig @a.root-servers.net com NS` 부터 시작해 **수동으로 위임을 3단계 따라가** 본인 도메인의 A레코드까지 도달해보라. 위임의 실체가 손에 붙는다.

**[Nginx 연결]** Nginx Day 2의 resolver 패턴, Day 8의 DNS-01 챌린지가 오늘 내용의 응용이다.

---

## Day 4 — NAT: 공유기, Docker 포트매핑, iptables

### 오늘의 개념

**NAT의 존재 이유**: IPv4 주소 고갈 → 사설 대역(10.x, 172.16-31.x, 192.168.x)을 집/회사 안에서 쓰고, 공유기가 나갈 때 공인 IP로 **바꿔치기**한다.

- **SNAT(source NAT)** — 나가는 패킷의 출발지를 바꿈. 공유기가 하는 일. 응답이 돌아오면 기억해둔 매핑표로 다시 사설 IP로 되돌린다. → **"안에서 밖은 되는데 밖에서 안은 안 되는"** 비대칭의 원인.
- **DNAT(destination NAT)** — 들어오는 패킷의 목적지를 바꿈. **포트포워딩의 정체**이자, **Docker `-p 8080:80`의 정체**. 즉 공유기 포트포워딩과 Docker 포트매핑은 **같은 기술**이다. 오늘 이걸 증명한다.

**Docker 브리지 네트워크**: 컨테이너마다 가상 랜선(veth pair)이 있고, 호스트의 가상 스위치(docker0 또는 br-xxxx)에 꽂혀 있다. 컨테이너 → 외부는 SNAT(MASQUERADE), 외부 → 컨테이너는 DNAT. **호스트가 곧 컨테이너들의 공유기다.**

### 시간 배분
- 0:00–0:30 사설/공인 IP 확인, NAT 비대칭 체감
- 0:30–1:10 Docker 네트워크 해부
- 1:10–1:40 DNAT 증명 (리눅스 환경에서 iptables 관찰)
- 1:40–2:00 NAT 흐름도 그리기 + 일지

### 실험

**실험 A — 나의 두 얼굴**

```bash
ipconfig getifaddr en0    # 사설 IP (192.168.x.x)
curl -s ifconfig.me       # 공인 IP
```

둘이 다르다 = 중간에 NAT가 있다. 폰을 LTE로 바꿔 `curl ifconfig.me` → 완전히 다른 공인 IP. 이어서 **비대칭 확인**: 집 밖(폰 LTE)에서 공유기 공인IP:8080으로 접속 → 포트포워딩 없으면 실패, 있으면 성공. 그 차이가 DNAT 규칙 한 줄이다.

**실험 B — Docker 네트워크 해부도**

```bash
docker network ls
docker network inspect <compose네트워크>    # 서브넷/게이트웨이/컨테이너IP
ifconfig | grep -A3 bridge                  # Mac은 Docker Desktop VM이라 구조가 다름 (아래 참고)
```

> **Mac 주의**: Docker Desktop은 리눅스 VM 안에서 돌아서 호스트에서 docker0/veth가 안 보인다. 브리지/iptables 실험은 Mac mini에 리눅스 VM(lima/colima)을 쓰거나, netshoot 컨테이너 안에서 관찰한다. 이 제약 자체가 "Docker Desktop의 구조"를 배우는 소재다.

```bash
# 컨테이너 관점의 네트워크
docker run --rm --net <compose네트워크> nicolaka/netshoot ip addr
docker run --rm --net <compose네트워크> nicolaka/netshoot ip route   # default via <게이트웨이>
docker run --rm --net <compose네트워크> nicolaka/netshoot traceroute -n 8.8.8.8  # 첫 홉 = 브리지 게이트웨이
```

**실험 C — 포트매핑 = DNAT 증명** (리눅스 환경 확보 시)

```bash
sudo iptables -t nat -L -n | grep -A5 DOCKER
# → "tcp dpt:8080 to:172.x.x.x:80" 류의 DNAT 규칙 발견
```

이 한 줄이 `ports: ["8080:80"]`의 실체다. 공유기 관리 페이지의 포트포워딩 설정과 나란히 놓고 **같은 문법**임을 확인 — 오늘의 최대 수확.

**실험 D — 컨테이너 간 통신은 NAT를 안 탄다**
netshoot으로 nginx→app-a 트래픽을 잡아 출발지 IP 확인 → 컨테이너 사설 IP 그대로 (같은 브리지 안이라 스위칭만 일어남). 반면 컨테이너→외부(8.8.8.8)는 SNAT. **"어디서부터 NAT인가"** 경계를 그릴 수 있게 된다.

### 체크포인트
- [ ] SNAT/DNAT를 각각 "누가 언제 뭘 바꾸는지"로 설명할 수 있다
- [ ] 공유기 포트포워딩과 Docker 포트매핑이 같은 원리(DNAT)임을 규칙 레벨로 확인했다
- [ ] 컨테이너의 default gateway가 브리지임을 확인했다
- [ ] NAT 흐름도(외부→공유기→Mac mini→Docker브리지→컨테이너)를 그렸다

### 생각해볼 질문
1. NAT 뒤의 두 기기가 서로 직접 연결하려면(P2P) 왜 어려운가? 게임/화상통화는 이걸 어떻게 뚫나? (STUN/hole punching — Day 10 Tailscale의 복선)
2. CGNAT(통신사가 한 번 더 NAT)이면 포트포워딩이 왜 원천 불가능한가? 그때의 우회 수단은? (이미 아는 Tailscale이 답의 하나)
3. `network_mode: host`를 쓰면 오늘 본 구조에서 뭐가 사라지나? 언제 쓰고 싶어지고, 무엇을 잃나?
4. BOMS로 들어오는 요청 하나는 오늘 기준으로 DNAT를 몇 번 통과하나? (공유기 1번 + Docker 1번 = 2번을 흐름도에서 확인)

### 숙제
- **H1.** 오늘 그린 NAT 흐름도에 **각 구간의 IP:포트가 어떻게 변환되는지** 구체적 숫자를 채워 완성하라 (예: `1.2.3.4:54321 → 공인IP:443 → 192.168.0.10:443 → 172.18.0.3:8080`). 이 그림이 Day 10 최종 문서의 뼈대가 된다.
- **H2.** colima 또는 lima로 리눅스 환경을 하나 만들어 실험 C를 실제로 수행하라 (Mac에서 못 했다면). 겸사겸사 "Docker Desktop vs colima" 차이를 3줄 정리.

**[Nginx 연결]** Nginx Day 8의 "80 포트가 외부에서 열려 있어야 한다"가 오늘 그린 흐름도의 특정 구간 문제였음이 명확해진다.

---

## Day 5 — 라우팅과 경로 추적 (1주차 종합)

### 오늘의 개념

**라우팅 = 각 장비가 "이 목적지는 어느 문으로"를 정한 표(라우팅 테이블)의 연쇄.** 내 기기도 갖고 있다: `netstat -rn`의 default gateway가 "모르면 공유기로"라는 뜻.

**traceroute의 원리가 오늘의 백미**: IP 헤더의 TTL(Time To Live)은 홉마다 1씩 줄고, 0이 되면 그 라우터가 "시간 초과(ICMP Time Exceeded)"를 돌려보낸다. traceroute는 **TTL을 1, 2, 3...으로 일부러 짧게 보내서** 경로상 라우터들이 자백하게 만드는 해킹적 발상이다.

**RTT 읽는 법**: 홉별 RTT가 계단식으로 뛰는 구간 = 물리적 거리나 혼잡. 단, 중간 라우터의 응답이 느린 건 사용자 트래픽과 무관할 수 있다(라우터는 ICMP 응답을 후순위 처리) — **마지막 홉까지의 일관된 상승만이 진짜 신호**라는 게 오독 방지 포인트.

### 시간 배분
- 0:00–0:40 라우팅 테이블 / traceroute / mtr
- 0:40–1:10 경로 비교 실험
- 1:10–1:45 **1주차 종합 시나리오 훈련**
- 1:45–2:00 일지 작성

### 실험

**실험 A — 내 라우팅 테이블**

```bash
netstat -rn | head -20     # default → 공유기 IP 확인
route get 8.8.8.8          # 특정 목적지가 어느 인터페이스로 나가는지
```

**실험 B — 경로 추적**

```bash
traceroute -n 8.8.8.8          # 첫 홉 = 공유기, 둘째쯤 = 통신사
mtr -n google.com              # traceroute + ping 연속 실행 (손실률까지)
traceroute -n naver.com
traceroute -n aws.amazon.com   # 국내 vs 해외 홉 수/RTT 비교
```

기록할 것: 홉 수, 국내/해외 RTT 차이, `* * *`(응답 안 하는 라우터)의 존재. 폰 테더링으로 같은 목적지를 추적해 **경로가 완전히 다른 것**도 확인.

**실험 C — 1주차 종합 시나리오 훈련 (오늘의 핵심)**

각 시나리오에 대해 ① 첫 확인 명령 ② 예상 결과별 다음 분기 를 적는다. 실제로 재현 가능한 것은 재현까지.

1. "BOMS가 안 열려요" (외부 사용자) — 어디서부터? (dig → 공인IP ping/traceroute → 포트포워딩 → 컨테이너 ss 순의 **바깥→안 이분탐색**을 스스로 구성할 수 있는가)
2. 배포 직후 30초만 502 — 1주차 지식으로 패킷 레벨 설명 (RST + Docker DNS 캐싱)
3. `curl -w`로 찍으니 namelookup만 3초 — 범인 후보와 검증 방법
4. 서버에서 CLOSE_WAIT 400개 발견 — 무엇을 의미하고 어느 코드를 의심하나
5. 집에서는 되는데 회사에서 안 되는 사이트 — 후보 3가지 (DNS 차이 / 방화벽 / 경로 문제)를 구분할 실험 설계

### 체크포인트
- [ ] traceroute의 TTL 원리를 남에게 설명할 수 있다
- [ ] mtr 출력에서 "진짜 문제 구간"과 "ICMP 후순위 노이즈"를 구분할 수 있다
- [ ] 종합 시나리오 5개에 진단 트리를 그렸다

### 생각해볼 질문
1. 같은 목적지인데 갈 때마다 경로가 다를 수 있다. 인터넷 라우팅(BGP)이 "최단 거리"가 아니라 "정책"으로 결정된다는 게 무슨 뜻일까?
2. `* * *` 홉이 있어도 통신은 정상인 경우가 많다. traceroute 결과에서 "실패"라고 판단해도 되는 신호는 정확히 뭔가?
3. ping(ICMP)은 되는데 특정 포트만 안 열리는 경우 — 네트워크 문제인가 방화벽 문제인가? 이걸 구분하는 명령 조합은? (`nc -zv`가 힌트)

### 숙제
- **H1.** **1주차 진단 치트시트**를 완성하라: 증상 → 명령 → 판단 분기. Day 2의 TCP 상태 치트시트와 오늘 시나리오 훈련을 통합한 한 페이지. (Nginx 코스의 장애 치트시트와 나란히 두면 인프라 온콜 문서의 원형이 된다)
- **H2.** BOMS 도메인에 대해 외부망(폰 LTE)에서 mtr을 5분 돌려 손실률/RTT 리포트를 만들어라. 집 인터넷의 베이스라인 확보 — 나중에 "느려요" 소리 들을 때 비교 기준이 된다.

---

# 2주차 — 보안 계층과 응용 계층, 그리고 실전

---

## Day 6 — TLS 해부

### 오늘의 개념

**TLS handshake의 뼈대** (TLS 1.3 기준, 개념 위주):
1. ClientHello — "나 이런 암호 스위트 지원해, 그리고 (1.3부터) 내 키 재료 미리 줄게"
2. ServerHello + **Certificate** — "이걸로 하자, 그리고 이게 내 신분증(인증서 체인)"
3. 클라이언트가 체인 검증(신뢰하는 루트 CA까지 연결되나, 도메인 맞나, 유효기간 맞나) → 세션 키 확립 → 이후 대칭키 암호화

**핵심 통찰 2개**:
- 비대칭 암호(인증서)는 **신원 확인과 키 교환에만** 쓰고, 실제 데이터는 **대칭키**로 암호화한다 (비대칭은 느리니까).
- TLS 1.3은 handshake가 1-RTT (1.2는 2-RTT) — Day 1의 "RTT = 비용" 관점이 여기서 재등장.

**SNI**: 한 IP에 여러 도메인이 있을 때, 클라이언트가 ClientHello에 "나 이 도메인 찾아왔어"를 **평문으로** 적는다. → HTTPS여도 방문 도메인은 노출된다는 프라이버시 함의 + Nginx가 도메인별 인증서를 고를 수 있는 이유.

**MITM과 mitmproxy**: 중간자가 가짜 인증서를 내밀면 클라이언트 검증에서 실패한다 — **단, 중간자의 CA를 신뢰 저장소에 심으면 성립한다.** mkcert(Nginx Day 6)와 mitmproxy는 이 원리를 합법적으로 쓰는 도구다.

### 시간 배분
- 0:00–0:40 handshake 관측
- 0:40–1:10 인증서 체인 해부
- 1:10–1:45 mitmproxy 체험
- 1:45–2:00 일지 작성

### 실험

**실험 A — 암호화의 실체 확인 (Day 1과 수미상관)**
Wireshark를 켜고 `curl https://example.com` → Follow TCP Stream → **Day 1의 평문 HTTP와 달리 아무것도 읽을 수 없는 것** 확인. ClientHello 패킷을 열어 **SNI 필드에 도메인이 평문으로** 있는 것도 확인 — "HTTPS가 숨기는 것과 못 숨기는 것"의 경계.

**실험 B — handshake 단계별 관찰**

```bash
curl -v https://본인도메인 2>&1 | grep -E "TLS|SSL|certificate"
openssl s_client -connect 본인도메인:443 -servername 본인도메인 < /dev/null
```

s_client 출력에서 찾을 것: 인증서 체인(`s:` `i:` 줄들 — subject와 issuer가 사슬로 이어지는 것), 프로토콜 버전, cipher. 이어서:

```bash
openssl s_client ... 2>/dev/null | openssl x509 -noout -subject -issuer -dates
```

**실험 C — 검증 실패 3종 세트 (의도적 파괴)**

```bash
curl https://expired.badssl.com/        # 만료
curl https://wrong.host.badssl.com/     # 도메인 불일치
curl https://self-signed.badssl.com/    # 자가서명
curl -k https://self-signed.badssl.com/ # -k = 검증 생략 (뭘 포기하는 건지 인지)
```

badssl.com은 일부러 깨진 인증서를 모아둔 훈련장이다. 각 에러 메시지가 검증의 어느 단계 실패인지 매핑하라.

**실험 D — mitmproxy로 나를 도청하기**

```bash
brew install mitmproxy
mitmproxy   # 기본 8080 프록시
# 다른 터미널
curl -x http://localhost:8080 https://example.com          # 실패 (인증서 검증)
curl -x http://localhost:8080 --cacert ~/.mitmproxy/mitmproxy-ca-cert.pem https://example.com  # 성공 + mitmproxy 화면에 평문
```

**같은 요청이 CA 신뢰 여부 하나로 도청 가능/불가능이 갈리는 것**이 오늘의 결론이다. 회사 보안 프록시, 인증서 피닝이 왜 존재하는지가 여기서 전부 설명된다.

### 체크포인트
- [ ] 평문 HTTP와 TLS 트래픽을 Wireshark에서 대조했다
- [ ] SNI가 평문임을 패킷에서 확인했다
- [ ] 체인 검증 실패 3종의 에러를 구분할 수 있다
- [ ] mitmproxy로 "CA 신뢰 = 도청 가능"을 재현했다

### 생각해볼 질문
1. `-k`(검증 생략)를 스크립트에 박아두면 정확히 어떤 공격에 노출되나? "내부망이니까 괜찮다"는 언제 성립하고 언제 무너지나?
2. 인증서 피닝은 mitmproxy조차 막는다. 대신 무엇이 어려워지나? (인증서 갱신 관점 — Let's Encrypt 90일과 충돌하는 지점)
3. TLS 종료를 Nginx에서 하고 내부는 평문(Nginx Day 6 질문의 재방문) — 오늘 배운 걸로 그 전제 조건을 더 정밀하게 서술해보라. "Docker 브리지 안 트래픽을 도청하려면 공격자는 뭘 확보해야 하는가?"
4. ECH(Encrypted ClientHello)가 표준화되면 SNI 노출 문제가 해소된다. 그러면 누가 곤란해질까? (기업 방화벽, 국가 검열의 작동 방식과 연결)

### 숙제
- **H1.** 본인 도메인 인증서의 **체인 전체**(리프 → 중간 CA → 루트)를 그림으로 그리고, 각 단계에서 "누가 누구를 보증하는지" 화살표에 적어라.
- **H2.** Nginx Day 6~8에서 다룬 fullchain.pem의 "fullchain"이 정확히 오늘 그린 그림의 어디까지인지 확인하라 (루트는 포함되나? 왜?).

**[Nginx 연결]** Nginx 코스 Day 6~8의 운영 절차에 대한 원리층이 오늘이다. 둘을 붙이면 "발급-검증-갱신"의 전체 그림이 완성된다.

---

## Day 7 — HTTP 시맨틱스: keepalive, chunked, 캐시 재검증

### 오늘의 개념

**Connection: keep-alive** — HTTP/1.1의 기본값. 한 TCP 연결로 여러 요청을 처리해 handshake 비용(Day 1)을 절약한다. 단 **한 번에 하나씩**(요청→응답→요청)이라 앞 요청이 느리면 뒤가 다 막힌다 = **HoL(head-of-line) blocking**. HTTP/2는 한 연결에 스트림을 다중화해 이걸 응용 계층에서 풀었다(TCP 계층 HoL은 남는다 → QUIC의 존재 이유).

**응답의 끝을 아는 두 가지 방법**:
- `Content-Length: N` — 미리 크기를 안다
- `Transfer-Encoding: chunked` — 크기를 모른 채 조각조각 보낸다 (각 조각 앞에 16진수 크기, `0\r\n\r\n`으로 종료). **SSE가 이 위에서 동작한다** — Nginx Day 10의 스트리밍이 전송 계층에서 어떻게 생겼는지가 오늘 보인다.

**캐시 재검증**: `Cache-Control: no-cache`는 "캐시 쓰지 마"가 아니라 **"쓰기 전에 물어봐"**다. 물어보는 수단이 `ETag`/`If-None-Match` (또는 `Last-Modified`/`If-Modified-Since`)이고, 안 바뀌었으면 서버는 본문 없이 **304**만 준다. Nginx Day 9에서 index.html에 no-cache를 준 게 정확히 이 메커니즘을 믿은 것이다.

### 시간 배분
- 0:00–0:35 keepalive 관측
- 0:35–1:10 chunked 해부
- 1:10–1:40 ETag/304 실험
- 1:40–2:00 일지 작성

### 실험

**실험 A — keepalive를 패킷으로**

```bash
sudo tcpdump -i lo0 port 8080 -nn &
curl -s localhost:8080/a/ localhost:8080/b/ -o /dev/null -o /dev/null
```

SYN이 **한 번만** 나오는 것 확인. 이어서 `curl -H "Connection: close" ...` 두 번 → SYN 두 번. 같은 작업의 패킷 수를 비교표로.

**실험 B — chunked를 육안으로**

Nginx 샌드박스나 Spring SSE 엔드포인트에 대해:

```bash
curl -v --raw http://localhost:8080/api/stream
```

`--raw`는 curl이 chunked를 해석하지 않고 원문을 보여준다 → **16진수 크기 + 데이터 + \r\n** 구조를 직접 관찰. `nc localhost 8080` 으로 요청을 손으로 치고 응답 원문을 봐도 좋다(Day 2 기술 재사용). 그리고 답해보라: SSE 스트림에는 Content-Length가 있는가, 없는가? 왜 없을 수밖에 없는가?

**실험 C — ETag와 304**

```bash
# 1차 요청: ETag 받기
curl -sI http://localhost:8080/index.html | grep -i etag
# 2차 요청: 조건부
curl -sI -H 'If-None-Match: "<받은값>"' http://localhost:8080/index.html
# → HTTP/1.1 304 Not Modified, 본문 없음
```

파일을 한 글자 수정 → 같은 조건부 요청 → 200 + 새 ETag. **"no-cache인데도 네트워크 비용이 거의 없는 이유"**가 이 왕복이다. 브라우저 개발자도구 Network에서 `304`와 `(from disk cache)`의 차이도 구분해서 기록.

**실험 D — HTTP/2 맛보기**

```bash
curl -v --http1.1 https://www.google.com -o /dev/null 2>&1 | grep "HTTP/"
curl -v --http2   https://www.google.com -o /dev/null 2>&1 | grep "HTTP/"
```

Wireshark로 h2 트래픽을 보면 TLS 안이라 안 보인다 — 대신 브라우저 개발자도구 Network 탭의 Protocol 열(h2/h3 표시)로 실서비스들의 채택 현황을 관찰. Nginx 샌드박스의 `http2 on`(Nginx Day 6) 유무로 프로토콜이 바뀌는 것도 확인.

### 체크포인트
- [ ] keepalive 유무의 SYN 개수 차이를 패킷으로 확인했다
- [ ] chunked 인코딩의 와이어 포맷을 원문으로 봤고, SSE와의 관계를 설명할 수 있다
- [ ] 304 재검증 왕복을 curl로 재현했다
- [ ] no-cache / no-store / immutable 세 지시어의 차이를 설명할 수 있다

### 생각해볼 질문
1. HTTP/1.1 시절 브라우저가 도메인당 연결을 6개씩 열었던 이유를 HoL로 설명해보라. HTTP/2에서는 왜 1개로 줄었나?
2. 프록시(Nginx)가 chunked 응답을 버퍼링하면(Nginx Day 10) 클라이언트 입장에서 chunked의 장점이 어떻게 소멸하나?
3. ETag를 파일 내용 해시로 만들면 안전하지만, 다중 서버 환경에서 서버마다 ETag가 달라지는 문제가 생길 수 있다(예: inode 기반). 무슨 일이 벌어지고 어떻게 방지하나?
4. 학회 시스템의 참가자 명단 API처럼 자주 조회되고 가끔 바뀌는 데이터 — Cache-Control을 어떻게 설계하겠는가? (private/public, max-age, 재검증의 조합)

### 숙제
- **H1.** BOMS의 주요 응답들(HTML, API JSON, 정적 자원)의 현재 캐시 헤더를 `curl -sI`로 전수 조사해 표로 만들고, 개선안을 적어라.
- **H2.** `nc -l 9999`로 chunked 응답을 **손으로 타이핑**해서 curl에게 전달해보라 (16진수 크기 계산 포함). 성공하면 chunked는 완전히 이해한 것이다.

**[Nginx 연결]** Nginx Day 9의 캐시 전략, Day 10의 SSE가 오늘의 원리 위에 서 있다.

---

## Day 8 — 커넥션 풀과 TCP: HikariCP를 패킷으로 다시 보기

### 오늘의 개념

오늘은 새 개념보다 **과거 사건의 재수사**다. 겪었던 HikariCP 커넥션 고갈 / MySQL `Threads_connected` 문제를 1주차 도구로 완전히 해부한다.

**커넥션 풀의 본질**: DB 연결 = TCP handshake + MySQL 인증 + (TLS면 그것까지). 비싸니까 미리 만들어 재사용한다. 즉 **풀의 커넥션 수 = ESTABLISHED 소켓 수**여야 정상이다. 이 등식이 깨지는 지점이 전부 장애다:

| 관측 | 의미 |
|---|---|
| ESTABLISHED > pool max | 풀 밖에서 연결을 만드는 코드가 있다 (릭) |
| 앱은 있다는데 MySQL엔 없다 | `wait_timeout`으로 서버가 먼저 끊음 → 앱이 죽은 커넥션을 집어 에러 |
| CLOSE_WAIT 누적 (앱 쪽) | MySQL이 FIN 보냈는데 앱이 close 안 함 |
| 앱 재시작 후에만 정상 | 릭이 서서히 쌓이는 패턴의 전형 |

**`wait_timeout` 정렬 문제의 패킷 레벨**: MySQL이 유휴 커넥션에 FIN을 보낸다 → 풀은 모른 채 그 소켓으로 쿼리를 보낸다 → RST가 돌아온다 → "Connection reset" 예외. HikariCP의 `maxLifetime`을 `wait_timeout`보다 **짧게** 잡는 이유가 이 시퀀스 하나로 설명된다.

### 시간 배분
- 0:00–0:30 풀 기동 관측
- 0:30–1:10 릭 재현
- 1:10–1:45 wait_timeout 사건 재현
- 1:45–2:00 리포트 작성

### 실험

로컬 Spring + MySQL(Docker) 환경 기준. BOMS 로컬 프로파일이면 그대로 사용.

**실험 A — 풀 기동의 순간**

```bash
sudo tcpdump -i any port 3306 -nn &
# Spring 기동
```

`minimumIdle`(또는 pool size)만큼 handshake가 **연달아** 보이는 것 확인 (Day 1 숙제의 회수). 기동 후:

```bash
ss -tnp state established '( dport = :3306 )' | wc -l   # 앱 쪽에서 센 수
docker exec mysql mysql -e "SHOW STATUS LIKE 'Threads_connected'"  # DB 쪽에서 센 수
```

**양쪽 숫자가 일치하는 것**이 건강한 상태의 기준선.

**실험 B — 릭 재현 (과거 사건 재연)**

커넥션을 반납하지 않는 코드를 일부러 작성:

```java
// DataSource에서 직접 꺼내고 close하지 않음 (try-with-resources 제거)
Connection c = dataSource.getConnection();
c.prepareStatement("SELECT 1").executeQuery();
// return — close 없음
```

호출을 반복하면서 관찰:
1. `ss` 숫자가 pool max를 향해 증가
2. HikariCP `leak-detection-threshold`(과거에 배운 그 설정) 로그 발동 확인
3. 풀 고갈 시점에 새 요청이 **어떤 예외**를 몇 초 만에 받는지 (`connectionTimeout`의 실체)

**과거에는 증상(Threads_connected 증가)만 봤다면, 오늘은 소켓 수 → 릭 로그 → 고갈 예외의 전체 인과 사슬을 관측하는 것**이 목표다.

**실험 C — wait_timeout 사건 재연**

```sql
SET GLOBAL wait_timeout = 15;   -- 실험용으로 극단적으로 줄임
```

HikariCP `maxLifetime`을 그보다 길게(예: 60초) 잘못 설정 → 15초 이상 유휴 후 쿼리 → tcpdump에서 **MySQL발 FIN, 이후 앱의 전송에 RST** 시퀀스 포착 → 애플리케이션 예외 메시지와 대조. 그다음 `maxLifetime`을 10초로 정렬 → 같은 시나리오에서 풀이 **선제적으로 교체**해 에러가 안 나는 것 확인 (tcpdump에는 주기적 FIN/재연결이 보인다).

### 체크포인트
- [ ] 풀 크기 = ESTABLISHED 수 = Threads_connected 의 삼자 대조를 해봤다
- [ ] 릭의 인과 사슬(소켓 증가 → leak 로그 → 고갈 예외)을 전부 관측했다
- [ ] wait_timeout 불일치가 만드는 FIN→RST 시퀀스를 패킷으로 포착했다
- [ ] maxLifetime < wait_timeout 규칙을 "왜"까지 설명할 수 있다

### 생각해볼 질문
1. pool size를 무작정 키우면 왜 오히려 나빠지나? (MySQL 쪽 커넥션당 메모리, 그리고 컨텍스트 스위칭 — "적정 풀 크기 = 코어 수 기반" 논의를 찾아보라)
2. Nginx→백엔드 구간에도 같은 문제가 있다: 기본적으로 요청마다 새 연결이다. `upstream` 블록의 `keepalive` 지시어가 이 그림에서 뭘 바꾸나? (Day 2 질문 1의 회수)
3. 마이크로서비스 A→B→DB 체인에서 타임아웃을 각각 어떻게 배치해야 하나? "바깥이 안쪽보다 길어야 한다/짧아야 한다" — 어느 쪽이고 왜인가?
4. 학회 결제 시스템에서 PG사 API 호출용 HTTP 클라이언트에도 커넥션 풀이 있다. DB 풀과 같은 원리로 점검한다면 무엇을 보겠는가?

### 숙제
- **H1.** 오늘 실험 전체를 **"HikariCP 사건 재수사 리포트"**로 정리하라: 과거 증상 → 당시 조치 → 오늘 밝혀진 패킷 레벨 원리 → 재발 방지 체크리스트. (과거에 만든 MySQL 참고 문서의 개정판이 된다)
- **H2.** BOMS 운영 환경의 `wait_timeout` / `maxLifetime` / `leak-detection-threshold` 현재값을 확인하고 정합성을 판정하라.

---

## Day 9 — 불안정한 네트워크: tc 카오스 실험, 재시도와 멱등성

### 오늘의 개념

**네트워크는 반드시 실패한다.** 지금까지는 로컬(사실상 완벽한 망)에서 실험했다. 오늘은 `tc`(traffic control)로 지연·손실·대역폭 제한을 **주입**해서, 설정해둔 타임아웃과 재시도가 실제로 뭘 하는지 검증한다.

**tc netem 기본기** (컨테이너 안에서):
```bash
tc qdisc add dev eth0 root netem delay 200ms          # 지연
tc qdisc change dev eth0 root netem delay 200ms 50ms  # 지연 + 지터
tc qdisc change dev eth0 root netem loss 5%           # 패킷 손실
tc qdisc del dev eth0 root                             # 원복
```

**타임아웃의 진짜 질문 — "그래서 재시도해도 되는가?"**: 타임아웃은 "실패"가 아니라 **"결과를 모름"**이다. 요청이 서버에 도달해서 처리됐는데 응답만 늦었을 수도 있다. 여기서:
- **멱등(idempotent)** — 여러 번 해도 결과가 같음: GET, PUT, DELETE (설계상). 재시도 안전.
- **비멱등** — POST(생성, 결제). 무턱대고 재시도하면 **이중 결제/이중 등록**.
- 해법: **idempotency key** — 클라이언트가 요청마다 고유 키를 붙이고, 서버는 같은 키의 재요청에 저장해둔 첫 응답을 돌려준다. 학회 결제 시스템에 직결되는 설계다.

**TCP 재전송 관측**: 손실을 주입하면 드디어 Day 1에서 못 봤던 재전송(retransmission)을 볼 수 있다. Wireshark가 검은 배경으로 표시해준다.

### 시간 배분
- 0:00–0:40 tc로 지연/손실 주입 + 재전송 관측
- 0:40–1:15 타임아웃 검증 실험
- 1:15–1:45 멱등성 사고 실험 + 결제 시나리오 설계
- 1:45–2:00 일지 작성

### 실험

**실험 A — 지연 주입과 체감**

```bash
# app-a 컨테이너에 주입 (netshoot을 NET_ADMIN 권한으로)
docker run --rm --net container:<app-a> --cap-add NET_ADMIN nicolaka/netshoot \
  tc qdisc add dev eth0 root netem delay 300ms
# 확인
curl -w "@$HOME/curl-format.txt" -o /dev/null -s http://localhost:8080/a/
```

`time_connect`부터 밀리는 것 확인 (handshake도 RTT를 타니까 — Day 1 회수). Nginx의 `proxy_connect_timeout 5s`(Nginx Day 4)가 지연 5초 주입 시 실제로 발동하는지 검증.

**실험 B — 손실 주입과 재전송 관측**

```bash
tc qdisc change dev eth0 root netem loss 20%
```

Wireshark를 켠 채 큰 응답(수 MB 파일)을 받으면서: **재전송(검정), Dup ACK, 그리고 전송 속도가 뚝 떨어지는 것**(혼잡 제어가 물러서는 모습) 관찰. "TCP가 손실을 혼잡 신호로 해석한다"는 교과서 문장의 실물이다.

**실험 C — 타임아웃 매트릭스 검증**

지연을 단계별로 올리며 (100ms → 3s → 10s), 각 계층의 타임아웃이 **어느 순서로** 발동하는지 기록:

| 지연 | curl(기본) | Nginx proxy_read_timeout | Spring(RestClient 등) | 실제 발동한 것 |
|---|---|---|---|---|

"바깥 계층 타임아웃 > 안쪽 계층 타임아웃" 원칙(Day 8 질문 3)을 실측으로 검증하는 것이 목적.

**실험 D — 이중 결제 사고 실험 (설계 훈련)**

간단한 POST 엔드포인트(호출마다 DB에 row 삽입)를 만들고:
1. 응답 직전에 `Thread.sleep(5000)` → 클라이언트는 3초 타임아웃 + 재시도 1회
2. DB를 확인 → **row가 2개** = 이중 처리 재현
3. idempotency key 컬럼(unique) + "이미 있으면 기존 결과 반환" 로직 추가 → 같은 시나리오에서 row 1개 확인

**"타임아웃 = 결과 불명"을 코드와 DB로 증명하는 것**이 오늘의 백미다.

### 체크포인트
- [ ] tc로 지연/손실을 주입하고 원복할 수 있다
- [ ] TCP 재전송과 속도 저하(혼잡 제어)를 관측했다
- [ ] 타임아웃 매트릭스를 실측으로 채웠다
- [ ] 이중 처리를 재현하고 idempotency key로 막았다

### 생각해볼 질문
1. 재시도에 지수 백오프(exponential backoff)와 지터(jitter)를 넣으라는 이유는? 수천 클라이언트가 동시에 실패하고 **동시에** 재시도하면 무슨 일이 생기나? (thundering herd)
2. "재시도는 어느 계층에서 해야 하나" — 클라이언트 JS, Nginx(`proxy_next_upstream`), Spring, 그 각각에서 재시도하면 곱셈으로 폭발한다. 어디에 두고 어디서 빼겠는가?
3. PG사 결제 API가 타임아웃됐다. 재시도 전에 할 수 있는 더 안전한 행동은? (힌트: 대부분의 PG는 조회 API를 제공한다 — "모름"을 "앎"으로 바꾸고 나서 움직이기)
4. tc로 만든 5% 손실 환경에서 SSE(장수명 연결)와 폴링(짧은 요청 반복) 중 어느 쪽이 더 강건했나/할 것 같은가? Saveface 설계에 주는 시사점은?

### 숙제
- **H1.** 학회 결제 흐름(사용자 → BOMS → PG)의 **타임아웃/재시도/멱등성 설계 문서** 1페이지를 작성하라: 각 구간의 타임아웃 값과 근거, 재시도 정책, idempotency key를 어디서 발급하고 어디에 저장하는지.
- **H2.** BOMS의 외부 API 호출부(있다면)에서 현재 타임아웃 설정을 전수 조사하라. "설정 안 함(=무한 대기 또는 라이브러리 기본값)"인 곳이 있다면 그게 오늘 실험 기준으로 무슨 리스크인지 적어라.

---

## Day 10 — 종합: 한 요청의 전 생애 + Tailscale 원리

### 오늘의 개념

**오늘은 새 지식이 아니라 통합이다.** 목표 산출물: **"BOMS로의 한 요청, 전 생애 추적 문서"** — 외부 사용자의 브라우저에서 MySQL 소켓까지, 모든 구간을 도구로 관측한 증거와 함께.

전체 사슬:
```
브라우저 → [DNS 조회] → [TCP handshake] → [TLS handshake/SNI]
→ 공유기 DNAT → Mac mini → Docker DNAT → Nginx (프록시 헤더, location)
→ Docker 브리지 → Spring (스레드) → HikariCP → MySQL 소켓
→ 응답: chunked/Content-Length → Nginx 버퍼 → NAT 역변환 → 브라우저 렌더
```

각 화살표마다 이 코스에서 배운 관측 도구가 하나씩 대응된다. **화살표마다 "관측 명령 + 캡처/출력"을 붙일 수 있으면 코스 완주다.**

**Tailscale — NAT traversal의 실전판**: Day 4 질문 1의 회수. 두 NAT 뒤 기기가 직접 연결하는 원리 — ① 각자 STUN 서버에 물어 "바깥에서 본 내 주소"를 알아내고 ② 조정 서버(coordination server)로 서로의 주소를 교환한 뒤 ③ **동시에 서로에게 패킷을 쏴서** 양쪽 NAT에 매핑을 뚫는다(hole punching). 실패하면(대칭형 NAT, CGNAT) DERP 릴레이 서버 경유로 폴백. WireGuard가 그 위의 암호화 터널이다.

### 시간 배분
- 0:00–1:10 전 생애 추적 문서 작성 (구간별 관측 재수행)
- 1:10–1:40 Tailscale 해부
- 1:40–2:00 코스 총정리 회고

### 실험

**실험 A — 전 생애 추적 (최종 과제)**

외부망(폰 테더링)에서 BOMS에 실제 요청을 하나 보내면서, 각 구간을 관측해 문서를 채운다:

| # | 구간 | 관측 도구/명령 | 확인할 것 | 증거(캡처/출력) |
|---|---|---|---|---|
| 1 | DNS | `dig`, tcpdump port 53 | A레코드, TTL, 캐시 여부 | |
| 2 | TCP | tcpdump | SYN/SYN-ACK, RTT | |
| 3 | TLS | Wireshark, s_client | SNI, 체인, 버전 | |
| 4 | 공유기 NAT | 공유기 설정 + 흐름도 | DNAT 규칙 | |
| 5 | Docker NAT | iptables(colima) 또는 흐름도 | DNAT 규칙 | |
| 6 | Nginx | access.log(perf 포맷) | rt vs urt, 헤더 | |
| 7 | 브리지 내부 | netshoot tcpdump | 사설 IP 직통(NAT 없음) | |
| 8 | Spring | 앱 로그 | 클라이언트 IP(XFF 반영) | |
| 9 | DB | ss port 3306 | 풀 소켓 재사용 | |
| 10 | 응답 | curl --raw / -w | chunked or CL, 단계별 시간 | |

전부 이미 해본 것들의 재조합이다. **한 번의 요청으로 10구간을 동시에 잡는 오케스트레이션** 자체가 실력이다.

**실험 B — Tailscale 해부**

```bash
tailscale status                    # 각 피어가 direct인지 relay(DERP)인지
tailscale ping <다른기기>            # 경로와 RTT
tailscale netcheck                  # 내 NAT 유형, 가까운 DERP, UDP 가능 여부
```

MacBook(외부망) ↔ Mac mini 간이 **direct**로 뚫려 있는지 확인하고, direct라면 tcpdump로 **WireGuard UDP 패킷(기본 41641 근처)**이 오가는 것을 관측. `netcheck`의 NAT 유형 출력(예: hard/easy)을 Day 4의 hole punching 논의와 연결해 해석.

**실험 C — 코스 총정리 회고**

1. **"내가 3번 이상 틀렸거나 헷갈린 것 Top 3"** + 미래의 나에게 남기는 경고문 한 문단씩
2. Day 5의 진단 치트시트를 2주차 내용(TLS/HTTP/풀/카오스)까지 반영해 **최종판**으로 업데이트
3. 이 코스 이후의 다음 갈래 중 하나를 고르고 이유를 적기: ① eBPF 기반 관측(bpftrace) ② HTTP/3·QUIC 심화 ③ Kubernetes 네트워킹(CNI, Service, Ingress) ④ BGP/대규모 라우팅

### 체크포인트
- [ ] 전 생애 추적 문서 10구간에 전부 증거가 붙었다
- [ ] Tailscale의 direct/relay를 확인하고 원리로 설명할 수 있다
- [ ] "URL을 치면 무슨 일이 일어나나요"에 관측 증거 기반으로 10분 이상 말할 수 있다

### 생각해볼 질문
1. 전 생애 사슬에서 **장애가 났을 때 이분탐색의 첫 절단점**을 어디로 잡는 게 효율적인가? (바깥→안? 안→바깥? Nginx 로그부터?) 자신의 진단 순서를 하나의 원칙으로 정식화해보라.
2. Tailscale이 있는데 굳이 공인 도메인 + 포트포워딩 + Let's Encrypt를 유지하는 이유는 뭔가? 각각 어떤 사용자를 위한 경로인가? (외부 공개 서비스 vs 관리자 접근의 분리)
3. 이 코스에서 배운 것 중 IPv6가 보편화되면 사라지는 문제와 그대로 남는 문제를 나눠보라. (NAT는? TLS는? 커넥션 풀은?)

### 숙제 (코스 마무리)
- **H1.** 전 생애 추적 문서를 팀 위키/개인 노트에 정식 문서로 올려라. 6개월 뒤의 나(또는 신규 팀원)가 BOMS 인프라를 이해하는 온보딩 문서가 되게.
- **H2.** Nginx 코스의 장애 치트시트와 네트워크 진단 치트시트를 **하나의 온콜 문서**로 통합하라. 증상별 → 계층별 → 명령별 3중 색인이 있으면 최상.

---

# 학습 일지 템플릿

```markdown
## Day N — [주제]
- 날짜:
- 실제 투입 시간:

### 오늘 한 것
-

### 예측 vs 실제
| 실험 | 내 예측 | 실제 결과 | 틀렸다면 왜? |
|---|---|---|---|

### 캡처/출력 증거 (원문 붙여넣기, 주석 포함)
```
(tcpdump/ss/dig 출력 + 각 줄 의미 주석)
```

### 막혔던 것 / 에러
- 증상:
- 출력 원문:
- 시도한 것 (순서대로):
- 해결 여부와 근거:

### 새로 알게 된 것
-

### 생각해볼 질문에 대한 내 답
1.
2.

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

| 항목 | 판정 포인트 |
|---|---|
| **관측 증거** | 주장마다 캡처/출력 원문이 붙어 있는가. "확인했다"는 말만 있고 증거가 없으면 미확인으로 간주 |
| **캡처 주석** | tcpdump 출력에 자기 말로 주석을 달았는가 (복붙만 있으면 읽지 않은 것) |
| **예측 습관** | 실험 전 예측을 적었는가. 전부 맞았다면 오히려 쉬운 것만 한 신호 |
| **인과 사슬** | 증상→관측→원리→해법이 한 줄로 이어지는가 |
| **과거 연결** | HikariCP/MySQL/Nginx 등 기존 경험과 연결 지었는가 (Day 8이 대표 시험대) |
| **질문 응답** | 생각해볼 질문에 자기 논리로 답했는가 (정답보다 논증) |
| **숙제 연속성** | 숙제가 다음 날 일지로 이어지는가 |
| **남은 의문** | 비어 있으면 대개 표면만 훑은 것 |

**성공 판정**: 체크포인트 완료 + 주석 달린 증거 + 질문에 자기 논리 + 구체적인 남은 의문.
**보완 신호**: 증거 없는 "됐다" / 캡처는 있는데 해석이 없다 / 도구를 돌렸지만 출력을 읽지 않았다.
**중단 신호**: 같은 유형의 막힘 3일 연속이면 진도를 멈추고 그 주제만 하루 더. 특히 Day 1~2(tcpdump/ss)가 손에 안 붙으면 이후 전부가 흔들리므로, 여기서는 서두르지 않는다.

---

# 부록 — 읽을거리와 다음 단계

- **High Performance Browser Networking** (hpbn.co, 무료) — Day 1/6/7과 병행하면 밀도 최고. TCP·TLS·HTTP/2 챕터 우선.
- **Julia Evans zines** (wizardzines.com) — tcpdump/DNS/네트워킹 편. 각 Day 시작 전 워밍업 10분용.
- **Cloudflare Learning Center** — DNS/TLS/BGP 해설 글. 질문에 답 맞춰볼 때 참조.
- **Kurose & Ross, 컴퓨터 네트워킹: 하향식 접근** — 코스 후 이론 보강용. 3장(전송)·2장(응용)만이라도.
- **badssl.com** — TLS 검증 실패 훈련장 (Day 6).
- **nicolaka/netshoot** — 네트워크 디버깅 컨테이너. 실무에서도 그대로 쓴다.

**코스 이후 갈래** (Day 10 실험 C에서 선택):
1. **eBPF/bpftrace** — 커널 레벨 관측. tcpdump의 다음 세대.
2. **HTTP/3·QUIC** — UDP 위에 전송 계층을 다시 만든 이유.
3. **Kubernetes 네트워킹** — 이 코스의 개념(브리지/NAT/DNS/프록시)이 CNI/Service/Ingress로 어떻게 확장되는지. 기존 인프라 학습 아크의 자연스러운 다음 장.
4. **관측 가능성 통합** — perf 로그 + Prometheus + 오늘의 도구들을 Grafana 대시보드로. BOMS 모니터링 3계층 모델의 완성.
