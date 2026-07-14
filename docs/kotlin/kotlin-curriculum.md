# Kotlin 학습 커리큘럼 (Java/Spring 개발자용)

> 대상: Java + Spring Boot 실무 경험자
> 목표: Kotlin + Spring Boot로 신규 프로젝트(예: Saveface/Tactful) 백엔드를 설계·구현할 수 있는 수준
> 총 기간: 약 8주 (평일 1~1.5시간 기준, 조절 가능)

---

## 전체 로드맵

| 단계 | 기간 | 핵심 주제 | 완료 기준 |
|------|------|-----------|-----------|
| 입문 | 1주 | 문법, null safety, 클래스 | Kotlin Koans 완주 |
| 초급 | 2주 | 함수형 스타일, 컬렉션, Java 상호운용 | Java 코드를 관용적 Kotlin으로 변환 가능 |
| 중급 | 3주 | Spring Boot + Kotlin, 코루틴 기초, 테스트 | Kotlin으로 CRUD API + 테스트 작성 |
| 고급 | 2주+ | 코루틴 심화, DSL, 성능/내부 동작 | 실무 프로젝트에 자신 있게 적용 |

---

## 1단계 — 입문 (1주)

**목표: Kotlin 코드를 읽고 기본 문법으로 작성할 수 있다**

### Day 1-2: 기본 문법
- `val` / `var`, 타입 추론
- 함수 선언, 기본 인자(default parameter), 이름 있는 인자(named argument)
  - → Java의 오버로딩 지옥과 Builder 패턴이 왜 불필요해지는지 체감하기
- 문자열 템플릿 `"$name is ${age + 1}"`
- `if`/`when`이 **식(expression)** 이라는 점 — `val x = if (...) a else b`

### Day 3: Null Safety
- `T` vs `T?` — 타입 시스템 차원의 구분
- `?.` (safe call), `?:` (elvis), `!!` (non-null assertion)
- 스마트 캐스트 (`if (x != null)` 이후 자동 승격)
- `let`을 이용한 null 처리 패턴 `x?.let { ... }`

### Day 4-5: 클래스와 객체
- 주 생성자/부 생성자, `init` 블록
- `data class` — equals/hashCode/toString/copy 자동 생성 (Lombok 대체)
- 프로퍼티 (getter/setter가 언어에 내장), backing field
- `object` (싱글톤), `companion object` (static 대체)
- 상속: 기본이 `final`, 열려면 `open` — Java와 정반대 철학

### Day 6-7: 실습
- [ ] **Kotlin Koans 완주** (play.kotlinlang.org 또는 IntelliJ 플러그인)
- [ ] IntelliJ에서 BOMS의 DTO 2~3개를 Java→Kotlin 자동 변환 후 diff 읽기
- [ ] 변환된 코드에서 어색한 부분(`!!` 남발 등)을 손으로 다듬어보기

**참고 자료**
- 공식 문서: kotlinlang.org/docs (한국어 일부 지원)
- Kotlin Koans: play.kotlinlang.org/koans

---

## 2단계 — 초급 (2주)

**목표: "Java스러운 Kotlin"이 아니라 관용적(idiomatic) Kotlin을 쓴다**

### Week 1: 함수형 스타일과 컬렉션

**Day 1-2: 람다와 고차 함수**
- 람다 문법, `it` 암시 파라미터, 후행 람다(trailing lambda)
- 함수 타입 `(Int) -> String`, 함수 참조 `::function`
- Java의 `Function<T,R>` / `Supplier` 등과 비교

**Day 3-4: 컬렉션 API**
- `listOf` / `mutableListOf` — 읽기전용 vs 가변 구분
- `map`, `filter`, `flatMap`, `groupBy`, `associateBy`, `partition`
- `Sequence` — Java Stream과의 차이 (즉시 평가 vs 지연 평가, 언제 뭘 쓰나)
- Java Stream 코드를 Kotlin 컬렉션 API로 옮겨보기

**Day 5: 스코프 함수 (실무 빈출)**
- `let`, `run`, `apply`, `also`, `with` — 각각 언제 쓰는지 결정 표 만들기
- 남용 주의: 중첩 스코프 함수는 오히려 가독성 해침

### Week 2: 타입 시스템과 Java 상호운용

**Day 1-2: 고급 클래스**
- `sealed class` / `sealed interface` — `when`과 결합한 완전성 검사(exhaustive check)
- `enum class`와 sealed class의 사용 구분
- 중첩/내부 클래스 (`inner`)

**Day 3: 확장 함수 (extension function)**
- 기존 클래스에 함수 추가 — 유틸 클래스(`StringUtils`) 소멸
- 확장 프로퍼티
- 실제로는 static 메서드로 컴파일된다는 내부 동작 이해

**Day 4-5: Java 상호운용 (Spring 개발자 필수)**
- 플랫폼 타입 `String!` — null safety의 구멍
- `@Nullable` / `@NonNull` 어노테이션을 Kotlin 컴파일러가 읽는 방식
- `@JvmStatic`, `@JvmOverloads`, `@JvmField` — Java에서 Kotlin 호출 시
- 실습: BOMS의 Service 클래스 하나를 Kotlin으로 변환하고 기존 Java 코드에서 호출

**참고 자료**
- 『Kotlin in Action』 (드미트리 제메로프) — 이 단계에서 정독 시작 추천
- 공식 문서 "Idioms" 페이지 — 관용 표현 모음

---

## 3단계 — 중급 (3주)

**목표: Kotlin + Spring Boot로 실무 수준 API를 만들고 테스트한다**

### Week 1: Spring Boot + Kotlin

**Day 1-2: 프로젝트 셋업**
- start.spring.io에서 Kotlin 선택 시 생기는 차이 (build.gradle.kts)
- Gradle Kotlin DSL 기초
- 필수 컴파일러 플러그인 이해:
  - `kotlin-spring` (= allopen): `@Component` 등이 붙은 클래스를 자동 `open` 처리
  - `kotlin-jpa` (= noarg): Entity에 기본 생성자 자동 생성
  - 이게 없으면 왜 프록시/JPA가 깨지는지 원리 이해

**Day 3-4: 계층별 Kotlin 패턴**
- Controller: DTO를 data class로, 기본 인자 활용
- Service: 생성자 주입이 한 줄로 (`class FooService(private val repo: FooRepository)`)
- Entity: data class를 Entity에 쓰면 안 되는 이유 (equals/hashCode와 프록시 문제)
- `@ConfigurationProperties` + data class

**Day 5: 검증과 예외 처리**
- Bean Validation과 Kotlin nullable 타입의 관계 (`@field:NotNull` 접두사 이슈)
- Kotlin스러운 예외 처리: `runCatching`, `Result<T>`

### Week 2: 코루틴 기초

**Day 1-2: 개념**
- 코루틴이 스레드와 다른 점 — 경량 동시성, suspend의 의미
- `suspend fun`, `launch` vs `async`/`await`
- 코루틴 빌더와 `CoroutineScope`

**Day 3-4: 구조화된 동시성 (structured concurrency)**
- 부모-자식 관계, 취소 전파
- `Dispatchers.IO` / `Default` / `Main` 사용 구분
- Java의 CompletableFuture / 가상 스레드(Virtual Thread)와 비교

**Day 5: Spring에서의 코루틴**
- Spring MVC vs WebFlux에서의 suspend 지원
- 주의: JPA(블로킹)와 코루틴을 섞을 때의 함정 — `Dispatchers.IO`로 감싸기

### Week 3: 테스트 + 종합 실습

**Day 1-2: 테스트 도구**
- JUnit5 + Kotlin (backtick 함수명 `` fun `주문 생성 시 재고 감소`() ``)
- MockK — Mockito의 Kotlin 대체재 (`every { } returns`, `verify { }`)
- Kotest 맛보기 (선택)

**Day 3-5: 미니 프로젝트**
- [ ] Kotlin + Spring Boot로 작은 CRUD API 구축 (예: Saveface의 일부 도메인)
- [ ] TDD 사이클 적용 (기존 Red/Green 워크플로우 그대로)
- [ ] MockK 기반 단위 테스트 + `@SpringBootTest` 통합 테스트
- [ ] Docker 이미지 빌드까지 (기존 BOMS 배포 파이프라인 응용)

**참고 자료**
- Spring 공식 Kotlin 가이드: spring.io/guides/tutorials/spring-boot-kotlin
- 『Kotlin Coroutines: Deep Dive』 (마르친 모스카와) — 코루틴 전용 최고 교재

---

## 4단계 — 고급 (2주+, 필요 시 확장)

**목표: 내부 동작을 이해하고 라이브러리 수준의 코드를 작성한다**

### Week 1: 코루틴 심화와 Flow

- `Flow` — 콜드 스트림, `flowOn`, `buffer`, 백프레셔
- `StateFlow` / `SharedFlow` — 상태 관리와 이벤트 브로드캐스트
- `Channel` — 코루틴 간 통신
- 예외 처리 전략: `CoroutineExceptionHandler`, `supervisorScope`
- 실전 시나리오: 외부 AI API 병렬 호출 + 타임아웃 + 재시도 (CS 자동화 파이프라인에 응용 가능)

### Week 2: 언어 심화

**제네릭과 변성(variance)**
- `in` / `out` (선언 지점 변성) vs Java의 `? extends` / `? super`
- `reified` + `inline` — 타입 소거 우회 (`inline fun <reified T> parse(...)`)

**위임 (delegation)**
- `by` 키워드 — 클래스 위임, 프로퍼티 위임
- `lazy`, `observable`, 커스텀 delegate 작성

**DSL 설계**
- 수신 객체 지정 람다 (lambda with receiver)
- Gradle Kotlin DSL / Ktor 라우팅이 어떻게 만들어졌는지 분석
- 간단한 DSL 직접 만들어보기 (예: HTML 빌더, 테스트 픽스처 빌더)

**내부 동작과 성능**
- Kotlin 코드가 어떤 바이트코드로 컴파일되는지 (IntelliJ "Show Kotlin Bytecode")
- `inline` 함수의 비용/이득, 람다와 객체 할당
- data class `copy()`의 얕은 복사 함정

### 선택 확장 주제
- Ktor — Spring 없이 Kotlin 네이티브 웹 프레임워크 경험
- Kotlin Multiplatform (KMP) — 관심 있으면
- Arrow 라이브러리 — 함수형 프로그래밍 심화 (`Either`, `Option`)
- 컴파일러 플러그인 / KSP (어노테이션 프로세싱)

---

## 학습 원칙

1. **변환기를 스승으로**: IntelliJ Java→Kotlin 변환 결과를 읽고, 왜 그렇게 변환됐는지 + 어디가 어색한지 분석하는 게 가장 빠른 학습법
2. **실전 코드 우선**: 튜토리얼 반복보다 BOMS 코드 일부 변환, Saveface를 Kotlin으로 시작하는 것이 효율적
3. **`!!`는 코드 스멜**: `!!`를 쓰고 싶어질 때마다 설계를 다시 보기
4. **중급까지가 실무 커트라인**: 3단계까지 마치면 실무 투입 가능. 고급은 병행하면서 천천히
5. **기존 워크플로우 재활용**: TDD, Docker 배포, CI/CD 등 이미 갖춘 파이프라인에 언어만 바꿔 끼우기

## 마일스톤 체크리스트

- [ ] 입문: Kotlin Koans 100% 완료
- [ ] 초급: BOMS의 Java 클래스 1개를 관용적 Kotlin으로 변환 (변환기 결과보다 개선)
- [ ] 중급: Kotlin + Spring Boot CRUD API + MockK 테스트 완성
- [ ] 고급: Flow 기반 비동기 파이프라인 1개 구현 (예: AI API 병렬 호출)
