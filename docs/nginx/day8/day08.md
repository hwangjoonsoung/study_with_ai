# Day 8: 로깅과 디버깅 — 장애 진단 워크플로우

## 학습 목표
- access log / error log를 읽고 문제를 좁혀갈 수 있다
- 커스텀 log_format으로 응답시간·업스트림 상태를 기록할 수 있다

## 핵심 개념
- **access log**: 모든 요청 기록. 기본 combined 포맷에 운영 필수 변수를 추가:
  ```nginx
  log_format main_ext '$remote_addr - [$time_local] "$request" '
                      '$status $body_bytes_sent '
                      'rt=$request_time urt=$upstream_response_time '
                      'us=$upstream_status';
  ```
  - `$request_time`: 클라이언트 기준 전체 시간
  - `$upstream_response_time`: 백엔드가 쓴 시간 → **느린 게 nginx인지 Spring인지 이걸로 가른다**
- **error log 레벨**: debug > info > notice > warn > error > crit. 평소 warn, 디버깅 시 debug.
- **진단 워크플로우** (외우기):
  1. 증상 확인 (상태코드? 타임아웃? 느림?)
  2. access log에서 해당 요청 찾기 → `us=`(업스트림 상태) 확인
  3. 502/504면 error log에서 connect() failed / timed out 확인
  4. 백엔드 로그와 대조

## 실습
- main_ext 포맷 적용 후 BOMS에 요청 → rt와 urt 차이 관찰
- `docker logs -f nginx` 대신 로그 볼륨 마운트 + `tail -f` 습관화
- 상태코드별 집계 원라이너:
  ```bash
  awk '{print $9}' access.log | sort | uniq -c | sort -rn
  ```

## 브레이크 실험
1. Day 5의 502/504를 재현하고, **로그만 보고** 원인을 지목하는 훈련 (컨테이너 상태를 먼저 보지 말 것)

### 📝 브레이크 실험 기록

**블라인드 진단 훈련**
- 증상:

- access log에서 본 것 (rt / urt / us):

- error log에서 본 것:

- 최종 진단과 근거:

- 진단에 걸린 시간:

## 저널 질문

**Q1. 기존 모니터링 크론 스크립트에 nginx 로그 기반 알림(5xx 급증 감지)을 추가한다면 어떤 지표를 잡겠는가?**

### ✍️ 나의 답변


**Q2. rt=2.001 urt=0.003 인 요청이 발견됐다. 어디가 느린 것인가? 다음으로 무엇을 확인하겠는가?**

### ✍️ 나의 답변


## 오늘의 한 줄 요약
>
