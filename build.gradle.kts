plugins {
    java
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.study"
version = "0.0.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

// 코테(백준) 풀이 파일을 docs/career/week1 에 두고 컴파일 대상에 포함시킨다.
// 파일마다 고유 클래스명을 사용할 것 (제출 시에만 Main 으로 변경).
sourceSets {
    main {
        java {
            srcDir("docs/career/")
        }
    }
}

tasks.register<JavaExec>("runClass") {
    group = "application"
    description = "지정한 클래스의 main 을 실행한다. 예) -PmainClass=Boj2577"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = providers.gradleProperty("mainClass")
    standardInput = System.`in`
}

// docs/career 의 코테 풀이들도 main 소스셋에 있어 main() 이 여러 개다.
// Spring Boot 실행 진입점을 명시해 bootRun/bootJar 의 메인 클래스 모호성을 없앤다.
springBoot {
    mainClass = "com.study.Application"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
