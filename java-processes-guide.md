# Java Spring Boot 프로젝트 학습 정리

## 목차
1. [핵심 프로세스 설명](#1-핵심-프로세스-설명)
2. [전체적인 흐름 정리](#2-전체적인-흐름-정리)
3. [Java 주요 문법 및 프레임워크/라이브러리 메소드 정리](#3-java-주요-문법-및-프레임워크라이브러리-메소드-정리)

---

## 1. 핵심 프로세스 설명

### 1.1 회원가입 프로세스

**일반 사용자 회원가입** (`POST /api/users`)

```
클라이언트 요청 → Controller(유효성 검증) → Service(중복 이메일 체크) → Repository(DB 저장) → 응답 반환
```

1. 클라이언트가 `name`, `email`, `password`를 JSON으로 전송
2. `@Valid`가 DTO의 `@NotBlank`, `@Email`, `@Size(min=4)` 조건을 검증
3. `UserService.registerUser()`에서 `existsByEmail()`로 이메일 중복 확인
4. 중복이면 `DuplicateEmailException` 발생 → `GlobalExceptionHandler`가 409 응답
5. 중복이 아니면 `User` 엔티티 생성 (Role = `ROLE_USER`) 후 DB 저장
6. `CreateUserResponse(userId, name, email)` 반환 (HTTP 201)

**관리자 회원가입** (`POST /api/users/admin`)

- 일반 회원가입과 동일하나 `adminSecretKey` 검증이 추가됨
- 시크릿 키가 `"ADMIN_SECRET_2026"`과 일치하지 않으면 `UnauthorizedException` 발생
- 통과 시 Role = `ROLE_ADMIN`으로 저장

### 1.2 로그인/로그아웃 프로세스

**로그인** (`POST /api/users/login`)

```
요청 → 이메일로 사용자 조회 → 비밀번호 비교 → 세션에 사용자 정보 저장 → 응답
```

1. `email`, `password`로 요청
2. `findByEmail()`로 사용자 조회, 없으면 `InvalidCredentialsException` (401)
3. 비밀번호 불일치 시 `InvalidCredentialsException` (401)
4. 성공 시 `HttpSession`에 `userId`와 `role` 저장
5. `LoginResponse(userId, name, email, role, "로그인 성공")` 반환

**로그아웃** (`POST /api/users/logout`)

1. `session.invalidate()`로 세션 무효화
2. `{"message": "로그아웃 성공"}` 반환

### 1.3 주문 생성 프로세스

**주문하기** (`POST /api/orders`)

```
요청 → 사용자 존재 확인 → Order 엔티티 생성(상태: ORDERED) → DB 저장 → 응답
```

1. `userId`, `productName`, `quantity`, `shippingAddress`를 전송
2. `userRepository.findById()`로 사용자 존재 확인, 없으면 `UserNotFoundException` (404)
3. `Order` 생성자에서 `deliveryStatus = ORDERED`, `orderedAt = LocalDateTime.now()` 자동 설정
4. DB에 저장 후 `CreateOrderResponse` 반환 (HTTP 201)

### 1.4 배송 상태 관리 프로세스

**배송 상태 조회** (`GET /api/orders/{orderId}/delivery`)

1. `orderId`로 주문 조회, 없으면 `OrderNotFoundException` (404)
2. `DeliveryStatusResponse(orderId, deliveryStatus, orderedAt)` 반환

**배송 상태 변경** (`PATCH /api/orders/{orderId}/delivery`)

```
요청 → 주문 조회 → 엔티티의 상태 변경 → 트랜잭션 커밋 시 자동 UPDATE → 응답
```

1. `orderId`로 주문 조회
2. `order.changeDeliveryStatus()`로 상태 변경
3. `@Transactional` 덕분에 메서드 종료 시 JPA 변경 감지(Dirty Checking)로 자동 UPDATE
4. 변경된 상태를 `DeliveryStatusResponse`로 반환

배송 상태 흐름: `ORDERED → PREPARING → SHIPPED → DELIVERED`

### 1.5 사용자별 주문 조회 프로세스

**내 주문 조회** (`GET /api/users/{userId}/orders`)

```
세션 인증 확인 → 권한 검증 → 사용자 조회 → 주문 목록 조회 → 응답
```

1. 세션에서 `userId`와 `role`을 꺼냄
2. 세션에 `userId`가 없으면 → `UnauthorizedException` (403)
3. 요청 `userId`와 세션 `userId`가 다르고, 관리자(ROLE_ADMIN)도 아니면 → `UnauthorizedException`
4. `userRepository.findById()`로 사용자 조회
5. `orderRepository.findByUserIdOrderByOrderedAtDesc()`로 주문 목록 최신순 조회
6. Stream API로 `Order` 엔티티 목록 → `CreateOrderResponse` DTO 목록으로 변환
7. `UserOrdersResponse(userId, userName, totalOrders, orders)` 반환

### 1.6 전역 예외 처리 프로세스

`GlobalExceptionHandler`가 `@RestControllerAdvice`로 모든 컨트롤러의 예외를 가로챔:

| 예외 클래스 | HTTP 상태 | 발생 상황 |
|---|---|---|
| `UserNotFoundException` | 404 NOT_FOUND | 존재하지 않는 사용자 ID로 조회 |
| `OrderNotFoundException` | 404 NOT_FOUND | 존재하지 않는 주문 ID로 조회 |
| `InvalidCredentialsException` | 401 UNAUTHORIZED | 이메일/비밀번호 불일치 |
| `UnauthorizedException` | 403 FORBIDDEN | 미로그인 또는 권한 부족 |
| `DuplicateEmailException` | 409 CONFLICT | 이미 존재하는 이메일로 가입 시도 |
| `MethodArgumentNotValidException` | 400 BAD_REQUEST | `@Valid` 유효성 검증 실패 |

---

## 2. 전체적인 흐름 정리

### 2.1 프로젝트 아키텍처

```
Spring Boot 4.0.2 / Java 25 / Gradle
├── MySQL (운영) / H2 (테스트)
└── Spring Data JPA + Hibernate
```

### 2.2 패키지 구조 (도메인별 분리)

```
com.demo
├── DemoApplication.java                ← 애플리케이션 진입점
├── common/
│   └── exception/                      ← 전역 예외 처리
│       ├── GlobalExceptionHandler      ← @RestControllerAdvice
│       ├── UserNotFoundException
│       ├── OrderNotFoundException
│       ├── InvalidCredentialsException
│       ├── UnauthorizedException
│       └── DuplicateEmailException
├── user/                               ← 사용자 도메인
│   ├── controller/UserController       ← REST API 엔드포인트
│   ├── service/UserService             ← 비즈니스 로직
│   ├── repository/UserRepository       ← 데이터 접근 계층
│   ├── entity/                         ← JPA 엔티티
│   │   ├── User
│   │   └── Role (enum)
│   └── dto/                            ← 데이터 전송 객체
│       ├── CreateUserRequest
│       ├── CreateUserResponse
│       ├── CreateAdminRequest
│       ├── LoginRequest
│       ├── LoginResponse
│       └── UserOrdersResponse
└── order/                              ← 주문 도메인
    ├── controller/OrderController
    ├── service/OrderService
    ├── repository/OrderRepository
    ├── entity/
    │   ├── Order
    │   └── DeliveryStatus (enum)
    └── dto/
        ├── CreateOrderRequest
        ├── CreateOrderResponse
        ├── UpdateDeliveryStatusRequest
        └── DeliveryStatusResponse
```

### 2.3 계층별 역할 (Layered Architecture)

```
[클라이언트]
    ↓ HTTP 요청 (JSON)
[Controller 계층]  ← 요청/응답 매핑, 유효성 검증(@Valid), 세션 처리
    ↓ DTO
[Service 계층]     ← 비즈니스 로직, 트랜잭션 관리(@Transactional)
    ↓ Entity
[Repository 계층]  ← 데이터베이스 CRUD (Spring Data JPA)
    ↓ SQL
[Database]         ← MySQL (운영) / H2 (테스트)
```

- **Controller**: HTTP 요청을 받아 Service에 위임하고 응답을 반환. 세션 기반 인증 처리 담당.
- **Service**: 핵심 비즈니스 로직 수행. `@Transactional`로 트랜잭션 범위 관리.
- **Repository**: `JpaRepository`를 상속하여 기본 CRUD + 커스텀 쿼리 메서드 제공.
- **Entity**: 데이터베이스 테이블과 매핑되는 JPA 엔티티. 도메인 로직 포함.
- **DTO (record)**: 계층 간 데이터 전송용 불변 객체. 유효성 검증 어노테이션 포함.

### 2.4 전체 API 엔드포인트 정리

| HTTP Method | URI | 설명 | 인증 필요 |
|---|---|---|---|
| `POST` | `/api/users` | 일반 사용자 회원가입 | X |
| `POST` | `/api/users/admin` | 관리자 회원가입 | X (시크릿 키) |
| `POST` | `/api/users/login` | 로그인 | X |
| `POST` | `/api/users/logout` | 로그아웃 | X |
| `GET` | `/api/users/{userId}/orders` | 사용자별 주문 조회 | O (세션) |
| `POST` | `/api/orders` | 주문 생성 | X |
| `GET` | `/api/orders/{orderId}/delivery` | 배송 상태 조회 | X |
| `PATCH` | `/api/orders/{orderId}/delivery` | 배송 상태 변경 | X |

### 2.5 데이터 모델 (ERD)

```
┌──────────────────────┐        ┌──────────────────────────────┐
│       users          │        │          orders              │
├──────────────────────┤        ├──────────────────────────────┤
│ id (PK, BIGINT)      │   1:N  │ id (PK, BIGINT)              │
│ email (UNIQUE)       │◄──────│ user_id (FK, NOT NULL)        │
│ name                 │        │ product_name                 │
│ password             │        │ quantity                     │
│ role (ENUM)          │        │ shipping_address             │
│   - ROLE_USER        │        │ delivery_status (ENUM)       │
│   - ROLE_ADMIN       │        │   - ORDERED                  │
└──────────────────────┘        │   - PREPARING                │
                                │   - SHIPPED                  │
                                │   - DELIVERED                │
                                │ ordered_at (DATETIME)        │
                                └──────────────────────────────┘
```

**관계**: User(1) ↔ Order(N) — 한 명의 사용자가 여러 주문을 가질 수 있음 (`@ManyToOne`)

### 2.6 요청-응답 데이터 흐름 예시 (주문 생성)

```
1. 클라이언트 → POST /api/orders
   Body: {"userId": 1, "productName": "노트북", "quantity": 2, "shippingAddress": "서울시 ..."}

2. OrderController.placeOrder()
   - @Valid로 @NotNull, @NotBlank, @Min(1) 검증
   - CreateOrderRequest record로 역직렬화

3. OrderService.placeOrder()
   - @Transactional 시작
   - userRepository.findById(1) → User 엔티티 반환
   - new Order(user, "노트북", 2, "서울시 ...") → deliveryStatus=ORDERED, orderedAt=now()
   - orderRepository.save(order) → INSERT SQL 실행
   - @Transactional 커밋

4. 응답 → HTTP 201
   Body: {"orderId": 1, "userId": 1, "productName": "노트북", "quantity": 2,
          "shippingAddress": "서울시 ...", "deliveryStatus": "ORDERED",
          "orderedAt": "2026-02-15T14:30:00"}
```

---

## 3. Java 주요 문법 및 프레임워크/라이브러리 메소드 정리

### 3.1 Java 핵심 문법

#### record (Java 16+)

불변 데이터 클래스를 간결하게 선언하는 문법. `equals()`, `hashCode()`, `toString()`, getter가 자동 생성된다.

```java
// 이 프로젝트의 모든 DTO가 record로 작성됨
public record CreateUserRequest(
    @NotBlank String name,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 4) String password
) {}

// 사용법: request.name(), request.email(), request.password()
// 일반 클래스의 getName()이 아닌 name()으로 접근
```

**이 프로젝트에서 사용된 record 목록:**
- `CreateUserRequest`, `CreateUserResponse`, `CreateAdminRequest`
- `LoginRequest`, `LoginResponse`, `UserOrdersResponse`
- `CreateOrderRequest`, `CreateOrderResponse`
- `UpdateDeliveryStatusRequest`, `DeliveryStatusResponse`

#### enum

고정된 상수 집합을 타입 안전하게 정의한다.

```java
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}

public enum DeliveryStatus {
    ORDERED,    // 주문 완료
    PREPARING,  // 준비 중
    SHIPPED,    // 배송 중
    DELIVERED   // 배송 완료
}
```

#### Stream API (Java 8+)

컬렉션 데이터를 함수형 스타일로 처리한다.

```java
// UserService - Order 엔티티 리스트를 DTO 리스트로 변환
List<CreateOrderResponse> orderResponses = orders.stream()
        .map(order -> new CreateOrderResponse(
                order.getId(),
                user.getId(),
                order.getProductName(),
                order.getQuantity(),
                order.getShippingAddress(),
                order.getDeliveryStatus(),
                order.getOrderedAt()
        ))
        .toList();  // Java 16+, collect(Collectors.toList()) 대신 사용
```

```java
// GlobalExceptionHandler - 첫 번째 필드 에러 메시지 추출
String message = ex.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
        .orElse("요청이 올바르지 않습니다.");
```

| 메소드 | 설명 | 사용 위치 |
|---|---|---|
| `.stream()` | 컬렉션을 스트림으로 변환 | UserService, GlobalExceptionHandler |
| `.map(Function)` | 각 요소를 변환 | Order → CreateOrderResponse 변환 |
| `.toList()` | 스트림을 불변 List로 수집 (Java 16+) | UserService |
| `.findFirst()` | 첫 번째 요소를 Optional로 반환 | GlobalExceptionHandler |
| `.orElse(T)` | Optional이 비어있을 때 기본값 반환 | GlobalExceptionHandler |

#### Optional (Java 8+)

null을 안전하게 처리하기 위한 래퍼 클래스.

```java
// Repository에서 Optional 반환
Optional<User> findByEmail(String email);

// Service에서 Optional 처리
User user = userRepository.findByEmail(request.email())
        .orElseThrow(InvalidCredentialsException::new);  // 없으면 예외 발생

User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));  // 람다로 예외 생성
```

| 메소드 | 설명 | 사용 위치 |
|---|---|---|
| `.orElseThrow(Supplier)` | 값이 없으면 예외를 발생시킴 | UserService, OrderService |

#### 메소드 참조 (Method Reference)

람다 표현식을 더 간결하게 작성하는 문법.

```java
// 메소드 참조 (생성자 참조)
.orElseThrow(InvalidCredentialsException::new);
// 동일한 람다 표현식: .orElseThrow(() -> new InvalidCredentialsException());
```

#### 불변 컬렉션 팩토리 메소드 (Java 9+)

```java
// Map.of()로 불변 Map 생성
return Map.of("message", "로그아웃 성공");
return Map.of("message", ex.getMessage());
```

### 3.2 Spring Boot 어노테이션

#### 애플리케이션 설정

| 어노테이션 | 설명 | 사용 위치 |
|---|---|---|
| `@SpringBootApplication` | 컴포넌트 스캔 + 자동 설정 + 설정 클래스를 한번에 선언 | DemoApplication |

#### 웹 계층 (Controller)

| 어노테이션 | 설명 | 사용 위치 |
|---|---|---|
| `@RestController` | `@Controller` + `@ResponseBody`. JSON 응답을 반환하는 컨트롤러 | UserController, OrderController |
| `@RequestMapping("/api/users")` | 컨트롤러 레벨의 기본 URL 경로 매핑 | UserController, OrderController |
| `@GetMapping` | HTTP GET 요청 매핑 | 배송 상태 조회, 주문 조회 |
| `@PostMapping` | HTTP POST 요청 매핑 | 회원가입, 로그인, 주문 생성 |
| `@PatchMapping` | HTTP PATCH 요청 매핑 | 배송 상태 변경 |
| `@PathVariable` | URL 경로의 변수를 파라미터로 매핑 (`/{orderId}`) | OrderController, UserController |
| `@RequestBody` | HTTP 요청 본문(JSON)을 객체로 역직렬화 | 모든 POST/PATCH 메서드 |
| `@ResponseStatus(HttpStatus.CREATED)` | 응답 HTTP 상태 코드를 201로 설정 | 회원가입, 주문 생성 |
| `@Valid` | 요청 DTO의 유효성 검증을 실행 | 모든 요청 DTO |

#### 서비스 계층

| 어노테이션 | 설명 | 사용 위치 |
|---|---|---|
| `@Service` | 서비스 계층 빈으로 등록 | UserService, OrderService |
| `@Transactional` | 메서드 실행을 트랜잭션으로 감싸고 커밋/롤백을 자동 관리 | UserService, OrderService |
| `@Transactional(readOnly = true)` | 읽기 전용 트랜잭션. 성능 최적화(Flush 생략) | login(), getUserOrders(), checkDeliveryStatus() |

#### 예외 처리

| 어노테이션 | 설명 | 사용 위치 |
|---|---|---|
| `@RestControllerAdvice` | 전역 예외 처리 클래스 선언 | GlobalExceptionHandler |
| `@ExceptionHandler(예외.class)` | 특정 예외 타입을 처리하는 메서드 지정 | GlobalExceptionHandler의 각 핸들러 |

### 3.3 JPA / Hibernate 어노테이션

#### 엔티티 매핑

| 어노테이션 | 설명 | 사용 위치 |
|---|---|---|
| `@Entity` | JPA 엔티티 클래스로 선언 | User, Order |
| `@Table(name = "users")` | 매핑할 테이블 이름 지정 | User, Order |
| `@Id` | 기본키(Primary Key) 필드 지정 | User.id, Order.id |
| `@GeneratedValue(strategy = GenerationType.IDENTITY)` | 기본키를 DB의 auto_increment로 자동 생성 | User.id, Order.id |
| `@Column(nullable = false)` | NOT NULL 제약 조건 설정 | 대부분의 필드 |
| `@Column(nullable = false, unique = true)` | NOT NULL + UNIQUE 제약 조건 | User.email |
| `@Enumerated(EnumType.STRING)` | enum 값을 문자열로 저장 (기본값 ORDINAL은 숫자로 저장) | User.role, Order.deliveryStatus |

#### 연관관계 매핑

| 어노테이션 | 설명 | 사용 위치 |
|---|---|---|
| `@ManyToOne(fetch = FetchType.LAZY, optional = false)` | N:1 관계 매핑. LAZY 로딩으로 필요할 때만 조회 | Order.user |
| `@JoinColumn(name = "user_id", nullable = false)` | 외래키 컬럼 이름 지정 | Order.user |

#### JPA 변경 감지 (Dirty Checking)

```java
// OrderService.updateDeliveryStatus()
Order order = orderRepository.findById(orderId)...;
order.changeDeliveryStatus(request.deliveryStatus());
// save() 호출 없이 트랜잭션 커밋 시 자동으로 UPDATE SQL 실행됨
```

JPA의 영속성 컨텍스트(Persistence Context)가 엔티티의 변경을 자동으로 감지하여 트랜잭션 커밋 시 UPDATE 쿼리를 발생시킨다. 이것이 `updateDeliveryStatus()`에서 `save()`를 호출하지 않아도 되는 이유이다.

### 3.4 Spring Data JPA Repository 메소드

#### JpaRepository 기본 제공 메소드 (상속)

`JpaRepository<Entity, ID>`를 상속하면 아래 메소드들이 자동으로 제공된다:

| 메소드 | 설명 | 사용 위치 |
|---|---|---|
| `save(Entity)` | 엔티티 저장 (INSERT 또는 UPDATE) | 회원가입, 주문 생성 |
| `findById(ID)` | 기본키로 조회, `Optional<Entity>` 반환 | 사용자/주문 조회 |

#### 쿼리 메소드 (메서드 이름으로 자동 생성)

Spring Data JPA는 메서드 이름을 파싱하여 자동으로 SQL 쿼리를 생성한다:

```java
// UserRepository
Optional<User> findByEmail(String email);
// → SELECT * FROM users WHERE email = ?

boolean existsByEmail(String email);
// → SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)

// OrderRepository
List<Order> findByUserIdOrderByOrderedAtDesc(Long userId);
// → SELECT * FROM orders WHERE user_id = ? ORDER BY ordered_at DESC
```

| 키워드 | 의미 | 예시 |
|---|---|---|
| `findBy` | 조건 조회 | `findByEmail` → WHERE email = ? |
| `existsBy` | 존재 여부 확인 (boolean 반환) | `existsByEmail` → EXISTS(...) |
| `OrderBy...Desc` | 내림차순 정렬 | `OrderByOrderedAtDesc` → ORDER BY ordered_at DESC |

### 3.5 Jakarta Validation 어노테이션

`spring-boot-starter-validation` 의존성을 통해 사용하며, `@Valid`와 함께 요청 DTO의 필드를 검증한다.

| 어노테이션 | 설명 | 사용 위치 |
|---|---|---|
| `@NotBlank` | null, 빈 문자열, 공백만 있는 문자열 불허 | name, email, password 등 |
| `@NotNull` | null 불허 (빈 문자열은 허용) | userId, deliveryStatus |
| `@Email` | 이메일 형식 검증 | email 필드 |
| `@Size(min = 4)` | 문자열 최소 길이 4 | password 필드 |
| `@Min(1)` | 숫자 최솟값 1 | quantity 필드 |

검증 실패 시 `MethodArgumentNotValidException`이 발생하고, `GlobalExceptionHandler`에서 400 응답으로 처리된다.

### 3.6 Jakarta Servlet (세션 관리)

```java
// HttpSession - 서버 측 세션 관리
// 로그인 시 세션에 정보 저장
session.setAttribute("userId", response.userId());
session.setAttribute("role", response.role());

// 세션에서 정보 조회 (타입 캐스팅 필요)
Long sessionUserId = (Long) session.getAttribute("userId");
Role sessionRole = (Role) session.getAttribute("role");

// 로그아웃 시 세션 무효화
session.invalidate();
```

| 메소드 | 설명 | 사용 위치 |
|---|---|---|
| `session.setAttribute(key, value)` | 세션에 값 저장 | 로그인 |
| `session.getAttribute(key)` | 세션에서 값 조회 | 주문 조회 (인증 확인) |
| `session.invalidate()` | 세션 전체 무효화 | 로그아웃 |

### 3.7 Java 표준 라이브러리

| 클래스/메소드 | 설명 | 사용 위치 |
|---|---|---|
| `LocalDateTime.now()` | 현재 날짜/시간 생성 | Order 생성자 (주문 시각) |
| `Map.of(key, value)` | 불변 Map 생성 (Java 9+) | 로그아웃 응답, 예외 응답 |
| `List<T>` | 순서가 있는 컬렉션 | 주문 목록 |
| `Optional<T>` | null-safe 래퍼 | Repository 반환값 |
| `String.equals(Object)` | 문자열 비교 | 비밀번호 비교, 시크릿 키 비교 |

### 3.8 Spring 핵심 개념 요약

#### 의존성 주입 (DI - Dependency Injection)

생성자 주입 방식으로 의존성을 관리한다. 생성자가 하나이면 `@Autowired` 생략 가능.

```java
// UserService - 생성자 주입
private final UserRepository userRepository;
private final OrderRepository orderRepository;

public UserService(UserRepository userRepository, OrderRepository orderRepository) {
    this.userRepository = userRepository;
    this.orderRepository = orderRepository;
}
```

#### 트랜잭션 관리

- `@Transactional`: 메서드 실행 시 트랜잭션 시작, 정상 종료 시 커밋, 예외 발생 시 롤백
- `@Transactional(readOnly = true)`: 읽기 전용. Hibernate Flush 생략으로 성능 향상
- 클래스 레벨에 `@Transactional`을 선언하면 모든 메서드에 적용, 메서드 레벨에서 오버라이드 가능

#### 커스텀 예외 패턴

모든 커스텀 예외는 `RuntimeException`을 상속하여 Unchecked Exception으로 구현됨.
이를 통해 `@Transactional`이 자동으로 롤백을 수행한다 (Checked Exception은 기본적으로 롤백하지 않음).

```java
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("사용자를 찾을 수 없습니다. id=" + userId);
    }
}
```
