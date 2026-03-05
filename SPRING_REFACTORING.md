# Spring 프레임워크 리팩토링

기존 서블릿 기반의 직접 DB 접근 방식에서 **Spring IoC 컨테이너**를 도입하여 3계층(Controller → Service → DAO) 구조로 전환한 리팩토링 기록입니다.

---

## 목차

1. [리팩토링 배경](#리팩토링-배경)
2. [적용된 Spring 기능 요약](#적용된-spring-기능-요약)
3. [신규 생성 파일 (7개)](#신규-생성-파일-7개)
4. [수정 파일 (5개)](#수정-파일-5개)
5. [변경 전 vs 변경 후 구조 비교](#변경-전-vs-변경-후-구조-비교)

---

## 리팩토링 배경

| 항목 | 기존 방식 | 변경 방식 |
|------|-----------|-----------|
| DB 연결 관리 | 서블릿에서 `DataSource` 직접 획득 | Spring Bean으로 위임 |
| 계층 분리 | 서블릿 내에 SQL 직접 작성 | Controller → Service → DAO 분리 |
| 객체 생성/관리 | `new` 키워드로 직접 생성 | Spring IoC 컨테이너가 관리 |
| 의존성 주입 | 수동 참조 | `@Autowired` 자동 주입 |
| DataSource 초기화 | `ApplicationContextListener`에서 HikariCP 직접 설정 | `SpringConfig`의 `@Bean`으로 선언 |

---

## 적용된 Spring 기능 요약

### 1. IoC 컨테이너 — `AnnotationConfigApplicationContext`

Java 설정 클래스(`SpringConfig`)를 기반으로 Spring IoC 컨테이너를 생성합니다.
컨테이너는 `@ComponentScan` 범위 내의 Bean들을 자동으로 탐지·등록·관리합니다.

```java
// ApplicationContextListener.java
springContext = new AnnotationConfigApplicationContext(SpringConfig.class);
ctx.setAttribute("SPRING_CONTEXT", springContext);
```

서블릿은 `ServletContext`에서 이 컨테이너를 꺼내 필요한 Bean을 조회합니다.

```java
// 서블릿 공통 패턴
AnnotationConfigApplicationContext springCtx =
    ApplicationContextListener.getSpringContext(getServletContext());
XxxService service = springCtx.getBean(XxxService.class);
```

---

### 2. Java 기반 설정 — `@Configuration` + `@Bean`

XML 설정 없이 순수 Java 클래스로 Bean을 선언합니다.

```java
@Configuration
@ComponentScan(basePackages = "dev")   // dev 패키지 전체 컴포넌트 스캔
public class SpringConfig {

    @Bean
    public DataSource dataSource() {   // HikariCP DataSource를 Bean으로 등록
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://localhost:3306/card_db?...");
        config.setUsername("root");
        config.setPassword("1234");
        return new HikariDataSource(config);
    }
}
```

- `@Configuration` : 이 클래스가 Bean 정의 소스임을 선언
- `@ComponentScan` : `dev` 패키지 하위의 `@Component`, `@Service`, `@Repository` 등을 자동 탐지
- `@Bean` : 메서드 반환값을 Spring 컨테이너에 Bean으로 등록

---

### 3. 컴포넌트 스테레오타입 어노테이션

| 어노테이션 | 적용 계층 | 역할 |
|------------|-----------|------|
| `@Repository` | DAO | DB 접근 계층. 데이터 예외를 Spring 예외로 변환하는 의미 부여 |
| `@Service` | Service | 비즈니스 로직 계층임을 명시 |

`@ComponentScan`이 이 어노테이션들을 자동으로 탐지해 Bean으로 등록합니다.

---

### 4. 의존성 주입 — `@Autowired`

Bean 간의 의존관계를 Spring이 자동으로 연결합니다.

```
SpringConfig (@Bean DataSource)
    └─ UserDAO     (@Repository) ←── @Autowired DataSource
    └─ PaymentDAO  (@Repository) ←── @Autowired DataSource
    └─ FixedCostDAO(@Repository) ←── @Autowired DataSource

LoginService    (@Service) ←── @Autowired UserDAO
PaymentService  (@Service) ←── @Autowired PaymentDAO
FixedCostService(@Service) ←── @Autowired FixedCostDAO
```

개발자가 `new`로 객체를 생성하거나 의존성을 수동으로 연결하지 않아도 됩니다.

---

## 신규 생성 파일 (7개)

### `dev/config/SpringConfig.java`

**역할** : Spring IoC 컨테이너의 최상위 설정 클래스. HikariCP DataSource를 Bean으로 등록합니다.

**핵심 Spring 기능**

| 어노테이션/API | 설명 |
|----------------|------|
| `@Configuration` | Bean 정의 클래스 선언 |
| `@ComponentScan(basePackages = "dev")` | `dev` 패키지 하위 자동 스캔 |
| `@Bean` | `dataSource()` 메서드 반환값을 Spring Bean으로 등록 |

**변경 포인트**

기존에 `ApplicationContextListener`에서 `new HikariDataSource(config)`로 직접 생성하던 DataSource를 이 클래스의 `@Bean` 메서드로 이전하여 Spring이 생명주기를 관리하게 합니다.

---

### `dev/dao/UserDAO.java`

**역할** : 사용자 SEQ 조회 (로그인 검증용)

**SQL**
```sql
SELECT SEQ FROM CARD_TRANSACTION WHERE SEQ = ?
```

**핵심 Spring 기능**

| 어노테이션/API | 설명 |
|----------------|------|
| `@Repository` | DAO 계층 Bean 등록 |
| `@Autowired DataSource` | SpringConfig에서 등록한 DataSource 자동 주입 |

**메서드**

| 메서드 | 파라미터 | 반환 | 설명 |
|--------|----------|------|------|
| `findUserBySeq(String userId)` | userId | `String` or `null` | SEQ 존재 시 반환, 없으면 null |

---

### `dev/dao/PaymentDAO.java`

**역할** : 결제 날짜 목록 조회 및 월별 결제 리포트 조회

**SQL**
```sql
-- 날짜 목록
SELECT BAS_YH FROM CARD_TRANSACTION WHERE SEQ = ? GROUP BY BAS_YH

-- 월별 리포트
SELECT FSBZ_AM, AUTO_AM, DIST_AM, TRVL_AM, HOS_AM, TOT_USE_AM
FROM CARD_TRANSACTION WHERE SEQ = ? AND BAS_YH = ?
```

**핵심 Spring 기능**

| 어노테이션/API | 설명 |
|----------------|------|
| `@Repository` | DAO 계층 Bean 등록 |
| `@Autowired DataSource` | DataSource 자동 주입 |

**메서드**

| 메서드 | 반환 | 설명 |
|--------|------|------|
| `findPaymentDates(String seq)` | `List<String>` | 결제 연월 목록 |
| `findMonthlyReport(String seq, String date)` | `CardTransactionVO` | 월별 카테고리별 금액 |

---

### `dev/dao/FixedCostDAO.java`

**역할** : 고정 지출 금액 업데이트

**SQL**
```sql
UPDATE CARD_TRANSACTION SET {category} = ? WHERE SEQ = ? AND BAS_YH = ?
```

**핵심 Spring 기능**

| 어노테이션/API | 설명 |
|----------------|------|
| `@Repository` | DAO 계층 Bean 등록 |
| `@Autowired DataSource` | DataSource 자동 주입 |

**메서드**

| 메서드 | 반환 | 설명 |
|--------|------|------|
| `updateFixedCost(String seq, String date, String category, long cost)` | `boolean` | 업데이트 성공 여부 (영향 행 = 1 이면 true) |

---

### `dev/service/LoginService.java`

**역할** : 로그인 비즈니스 로직. UserDAO에 위임.

**핵심 Spring 기능**

| 어노테이션/API | 설명 |
|----------------|------|
| `@Service` | 서비스 계층 Bean 등록 |
| `@Autowired UserDAO` | UserDAO Bean 자동 주입 |

**메서드**

| 메서드 | 반환 | 설명 |
|--------|------|------|
| `login(String userId)` | `String` or `null` | DB에서 SEQ 조회 후 반환 |

**흐름**
```
LoginServlet → LoginService.login() → UserDAO.findUserBySeq()
```

---

### `dev/service/PaymentService.java`

**역할** : 결제 날짜 목록 및 월별 리포트 비즈니스 로직. PaymentDAO에 위임.

**핵심 Spring 기능**

| 어노테이션/API | 설명 |
|----------------|------|
| `@Service` | 서비스 계층 Bean 등록 |
| `@Autowired PaymentDAO` | PaymentDAO Bean 자동 주입 |

**메서드**

| 메서드 | 반환 | 설명 |
|--------|------|------|
| `getPaymentDates(String seq)` | `List<String>` | 결제 연월 목록 |
| `getMonthlyReport(String seq, String date)` | `CardTransactionVO` | 월별 카테고리별 금액 |

**흐름**
```
PaymentDatesServlet  → PaymentService.getPaymentDates()   → PaymentDAO.findPaymentDates()
ReportMonthsServlet  → PaymentService.getMonthlyReport()  → PaymentDAO.findMonthlyReport()
```

---

### `dev/service/FixedCostService.java`

**역할** : 고정 지출 등록 비즈니스 로직. FixedCostDAO에 위임.

**핵심 Spring 기능**

| 어노테이션/API | 설명 |
|----------------|------|
| `@Service` | 서비스 계층 Bean 등록 |
| `@Autowired FixedCostDAO` | FixedCostDAO Bean 자동 주입 |

**메서드**

| 메서드 | 반환 | 설명 |
|--------|------|------|
| `addFixedCost(String seq, String date, String category, long cost)` | `boolean` | 업데이트 성공 여부 |

**흐름**
```
AddFixedCostServlet → FixedCostService.addFixedCost() → FixedCostDAO.updateFixedCost()
```

---

## 수정 파일 (5개)

> 기존 DB 직접 접근 코드는 모두 `// [주석처리]`로 보존되어 있습니다.

---

### `dev/common/ApplicationContextListener.java`

**수정 내용**

| 제거(주석처리) | 추가 |
|----------------|------|
| HikariCP Source/Replica/단일 DataSource 직접 생성 | `AnnotationConfigApplicationContext` 생성 |
| `Class.forName()` 드라이버 로딩 | — |
| `ctx.setAttribute("DATA_SOURCE", ds)` 등 | `ctx.setAttribute("SPRING_CONTEXT", springContext)` |
| `getDataSource()`, `getSourceDataSource()`, `getReplicaDataSource()` 정적 메서드 | `getSpringContext()` 정적 메서드 |

**핵심 로직**

```java
// contextInitialized
springContext = new AnnotationConfigApplicationContext(SpringConfig.class);
ctx.setAttribute("SPRING_CONTEXT", springContext);

// contextDestroyed
if (springContext != null) springContext.close();  // 컨테이너 정상 종료

// 서블릿에서 호출하는 헬퍼 메서드
public static AnnotationConfigApplicationContext getSpringContext(ServletContext ctx) {
    return (AnnotationConfigApplicationContext) ctx.getAttribute("SPRING_CONTEXT");
}
```

**Spring과의 연결 방식**

`@WebListener`로 WAS 기동 시 자동 실행 → Spring 컨테이너를 `ServletContext`에 보관 → 각 서블릿이 필요할 때 꺼내 사용하는 **수동 브릿지** 패턴입니다.
(Spring MVC DispatcherServlet 방식이 아닌, 순수 서블릿과 Spring IoC 컨테이너를 공존시키는 방식)

---

### `dev/controller/servlet/user/LoginServlet.java`

**수정 내용**

| 제거(주석처리) | 추가 |
|----------------|------|
| `DataSource ds = ApplicationContextListener.getDataSource(...)` | Spring Context에서 `LoginService` Bean 획득 |
| SQL 직접 작성 (`PreparedStatement`, `ResultSet`) | `loginService.login(userId)` 호출 |

**변경 전 → 변경 후 핵심 코드**

```java
// [주석처리] 기존 방식
// DataSource ds = ApplicationContextListener.getDataSource(getServletContext());
// try (Connection conn = ds.getConnection(); PreparedStatement pstmt = ...) { ... }

// 변경 후
AnnotationConfigApplicationContext springCtx =
    ApplicationContextListener.getSpringContext(getServletContext());
LoginService loginService = springCtx.getBean(LoginService.class);
String seq = loginService.login(userId);
```

**Spring 활용 포인트** : `getBean(LoginService.class)`로 IoC 컨테이너에서 빈을 타입으로 조회합니다.

---

### `dev/controller/servlet/user/PaymentDatesServlet.java`

**수정 내용**

| 제거(주석처리) | 추가 |
|----------------|------|
| `DataSource` 직접 획득 및 SQL 실행 | Spring Context에서 `PaymentService` Bean 획득 |
| `ArrayList`, `ResultSet` 직접 처리 | `paymentService.getPaymentDates(userNo)` 호출 |

**변경 전 → 변경 후 핵심 코드**

```java
// [주석처리] 기존 방식
// DataSource ds = ApplicationContextListener.getDataSource(getServletContext());
// try (Connection conn = ds.getConnection()) { ... }

// 변경 후
AnnotationConfigApplicationContext springCtx =
    ApplicationContextListener.getSpringContext(getServletContext());
PaymentService paymentService = springCtx.getBean(PaymentService.class);
List<String> dates = paymentService.getPaymentDates(userNo);
out.print(gson.toJson(dates));
```

---

### `dev/controller/servlet/payment_amount/ReportMonthsServlet.java`

**수정 내용**

| 제거(주석처리) | 추가 |
|----------------|------|
| Replica/단일 DataSource 직접 획득 및 SQL 실행 | Spring Context에서 `PaymentService` Bean 획득 |
| `CardTransactionVO` 직접 빌드 | `paymentService.getMonthlyReport(userNo, date)` 호출 |

**변경 전 → 변경 후 핵심 코드**

```java
// [주석처리] 기존 방식
// DataSource ds = ApplicationContextListener.getReplicaDataSource(getServletContext());
// try (Connection conn = ds.getConnection()) { ... CardTransactionVO.builder()... }

// 변경 후
AnnotationConfigApplicationContext springCtx =
    ApplicationContextListener.getSpringContext(getServletContext());
PaymentService paymentService = springCtx.getBean(PaymentService.class);
CardTransactionVO transaction = paymentService.getMonthlyReport(userNo, date);
if (transaction != null) out.print(gson.toJson(transaction));
```

---

### `dev/controller/servlet/fixed_cost/AddFixedCostServlet.java`

**수정 내용**

| 제거(주석처리) | 추가 |
|----------------|------|
| `DataSource` 직접 획득 및 UPDATE SQL 실행 | Spring Context에서 `FixedCostService` Bean 획득 |
| `pstmt.executeUpdate()` 직접 처리 | `fixedCostService.addFixedCost(...)` 호출 |

**변경 전 → 변경 후 핵심 코드**

```java
// [주석처리] 기존 방식
// DataSource ds = ApplicationContextListener.getDataSource(getServletContext());
// try (Connection conn = ds.getConnection()) { ... pstmt.executeUpdate(); }

// 변경 후
AnnotationConfigApplicationContext springCtx =
    ApplicationContextListener.getSpringContext(getServletContext());
FixedCostService fixedCostService = springCtx.getBean(FixedCostService.class);
boolean isSuccess = fixedCostService.addFixedCost(userNo, date, category, cost);
out.print(isSuccess);
```

---

## 변경 전 vs 변경 후 구조 비교

### 변경 전 (서블릿 직접 DB 접근)

```
[LoginServlet]
    │
    ├── ApplicationContextListener.getDataSource()
    │       └── HikariDataSource (ServletContext 속성)
    │
    └── conn.prepareStatement("SELECT SEQ FROM ...")
            └── ResultSet 처리 → 세션 저장
```

### 변경 후 (Spring IoC + 3계층)

```
[LoginServlet]  (Controller)
    │
    └── ApplicationContextListener.getSpringContext()
            └── AnnotationConfigApplicationContext
                    │
                    └── springCtx.getBean(LoginService.class)
                                │
                                └── LoginService  (@Service)
                                        │
                                        └── UserDAO  (@Repository)
                                                │
                                                └── @Autowired DataSource
                                                        └── HikariCP (SpringConfig @Bean)
```

---

## 사용된 Spring 어노테이션 / API 정리

| 어노테이션 / API | 파일 | 역할 |
|-----------------|------|------|
| `@Configuration` | SpringConfig | Bean 정의 클래스 선언 |
| `@ComponentScan` | SpringConfig | `dev` 패키지 자동 스캔 |
| `@Bean` | SpringConfig | DataSource Bean 등록 |
| `@Repository` | UserDAO, PaymentDAO, FixedCostDAO | DAO 계층 Bean 등록 |
| `@Service` | LoginService, PaymentService, FixedCostService | 서비스 계층 Bean 등록 |
| `@Autowired` | DAO 3개, Service 3개 | 의존성 자동 주입 |
| `AnnotationConfigApplicationContext` | ApplicationContextListener | Java Config 기반 IoC 컨테이너 |
| `context.getBean(Class)` | 서블릿 5개 | 타입으로 Bean 조회 |
| `context.close()` | ApplicationContextListener | 컨테이너 정상 종료 (WAS 종료 시) |
