# Spring Boot 주문/배달 관리 API 만들기

## 프로젝트 소개

Spring Boot로 **주문-배달 관리 REST API**를 구현했습니다. 사용자 등록, 주문 생성, 배달 상태 조회/변경 기능을 제공합니다.

### 기술 스택
- **Java 25** / **Spring Boot 4.0.2**
- **Spring Data JPA** + **MySQL**
- **Jakarta Validation** (요청 검증)
- **Gradle 9.3**

### API 목록

| Method | URI | 설명 |
|--------|-----|------|
| POST | `/api/users` | 사용자 등록 |
| POST | `/api/orders` | 주문 생성 |
| GET | `/api/orders/{orderId}/delivery` | 배달 상태 조회 |
| PATCH | `/api/orders/{orderId}/delivery` | 배달 상태 변경 |

---

## 프로젝트 구조

```
com.demo/
├── DemoApplication.java
├── common/exception/
│   ├── GlobalExceptionHandler.java
│   ├── UserNotFoundException.java
│   └── OrderNotFoundException.java
├── user/
│   ├── controller/UserController.java
│   ├── service/UserService.java
│   ├── dto/
│   │   ├── CreateUserRequest.java
│   │   └── CreateUserResponse.java
│   ├── entity/User.java
│   └── repository/UserRepository.java
└── order/
    ├── controller/OrderController.java
    ├── service/OrderService.java
    ├── dto/
    │   ├── CreateOrderRequest.java
    │   ├── CreateOrderResponse.java
    │   ├── DeliveryStatusResponse.java
    │   └── UpdateDeliveryStatusRequest.java
    ├── entity/
    │   ├── Order.java
    │   └── DeliveryStatus.java
    └── repository/OrderRepository.java
```

**도메인별 패키지 분리**를 적용하여, `user`와 `order`가 각각 독립적인 레이어(Controller → Service → Repository → Entity)를 갖도록 구성했습니다. 공통으로 사용하는 예외 처리는 `common` 패키지에 분리했습니다.

---

## 1. 설정

### build.gradle

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.2'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    runtimeOnly 'com.mysql:mysql-connector-j'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/orderdb
    username: root
    password: 1234
  jpa:
    hibernate:
      ddl-auto: create
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

---

## 2. User 도메인

### Entity

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    protected User() {
    }

    public User(String email, String name) {
        this.email = email;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
}
```

- `protected` 기본 생성자는 JPA 스펙에서 요구하는 것으로, 외부에서 직접 호출하지 못하도록 접근 제한
- setter 없이 생성자를 통해서만 값을 설정하여 **불변성** 유지

### Repository

```java
public interface UserRepository extends JpaRepository<User, Long> {
}
```

### DTO

```java
public record CreateUserRequest(
        @NotBlank String name,
        @Email @NotBlank String email
) {
}
```

```java
public record CreateUserResponse(
        Long userId,
        String name,
        String email
) {
}
```

Java `record`를 사용하여 DTO를 간결하게 정의했습니다. `@NotBlank`, `@Email` 등의 Bean Validation으로 요청 값을 검증합니다.

### Service

```java
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CreateUserResponse registerUser(CreateUserRequest request) {
        User user = userRepository.save(new User(request.email(), request.name()));
        return new CreateUserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
```

### Controller

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUserResponse registerUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.registerUser(request);
    }
}
```

`@Valid`를 통해 DTO에 선언한 검증 어노테이션이 동작하도록 하고, 성공 시 **201 Created**를 응답합니다.

---

## 3. Order 도메인

### Entity

```java
public enum DeliveryStatus {
    ORDERED,
    PREPARING,
    SHIPPED,
    DELIVERED
}
```

```java
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus deliveryStatus;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    protected Order() {
    }

    public Order(User user, String productName, int quantity, String shippingAddress) {
        this.user = user;
        this.productName = productName;
        this.quantity = quantity;
        this.shippingAddress = shippingAddress;
        this.deliveryStatus = DeliveryStatus.ORDERED;
        this.orderedAt = LocalDateTime.now();
    }

    // getter 생략

    public void changeDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }
}
```

- `@ManyToOne(fetch = FetchType.LAZY)` — User를 지연 로딩하여 불필요한 쿼리 방지
- `@Enumerated(EnumType.STRING)` — enum을 문자열로 저장하여 DB 가독성 확보
- 주문 생성 시 `deliveryStatus`는 자동으로 `ORDERED`, `orderedAt`은 현재 시각으로 설정

### Repository

```java
public interface OrderRepository extends JpaRepository<Order, Long> {
}
```

### DTO

```java
public record CreateOrderRequest(
        @NotNull Long userId,
        @NotBlank String productName,
        @Min(1) int quantity,
        @NotBlank String shippingAddress
) {
}
```

```java
public record CreateOrderResponse(
        Long orderId,
        Long userId,
        String productName,
        int quantity,
        String shippingAddress,
        DeliveryStatus deliveryStatus,
        LocalDateTime orderedAt
) {
}
```

```java
public record UpdateDeliveryStatusRequest(
        @NotNull DeliveryStatus deliveryStatus
) {
}
```

```java
public record DeliveryStatusResponse(
        Long orderId,
        DeliveryStatus deliveryStatus,
        LocalDateTime orderedAt
) {
}
```

### Service

```java
@Service
@Transactional
public class OrderService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public OrderService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    public CreateOrderResponse placeOrder(CreateOrderRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        Order order = orderRepository.save(new Order(
                user,
                request.productName(),
                request.quantity(),
                request.shippingAddress()
        ));

        return new CreateOrderResponse(
                order.getId(), user.getId(),
                order.getProductName(), order.getQuantity(),
                order.getShippingAddress(), order.getDeliveryStatus(),
                order.getOrderedAt()
        );
    }

    @Transactional(readOnly = true)
    public DeliveryStatusResponse checkDeliveryStatus(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        return new DeliveryStatusResponse(
                order.getId(), order.getDeliveryStatus(), order.getOrderedAt());
    }

    public DeliveryStatusResponse updateDeliveryStatus(Long orderId, UpdateDeliveryStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.changeDeliveryStatus(request.deliveryStatus());

        return new DeliveryStatusResponse(
                order.getId(), order.getDeliveryStatus(), order.getOrderedAt());
    }
}
```

- 조회 메서드에 `@Transactional(readOnly = true)`를 적용하여 읽기 전용 최적화
- 존재하지 않는 사용자/주문에 대해 **커스텀 예외**를 던짐

### Controller

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateOrderResponse placeOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.placeOrder(request);
    }

    @GetMapping("/{orderId}/delivery")
    public DeliveryStatusResponse checkDeliveryStatus(@PathVariable Long orderId) {
        return orderService.checkDeliveryStatus(orderId);
    }

    @PatchMapping("/{orderId}/delivery")
    public DeliveryStatusResponse updateDeliveryStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateDeliveryStatusRequest request
    ) {
        return orderService.updateDeliveryStatus(orderId, request);
    }
}
```

---

## 4. 예외 처리

### 커스텀 예외

```java
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("사용자를 찾을 수 없습니다. id=" + userId);
    }
}
```

```java
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long orderId) {
        super("주문을 찾을 수 없습니다. id=" + orderId);
    }
}
```

기존에 `IllegalArgumentException` 하나로 모든 not-found 상황을 처리하던 것을, **도메인별 커스텀 예외**로 분리하여 어떤 리소스를 찾지 못했는지 명확히 구분할 수 있게 했습니다.

### GlobalExceptionHandler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleUserNotFound(UserNotFoundException ex) {
        return Map.of("message", ex.getMessage());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleOrderNotFound(OrderNotFoundException ex) {
        return Map.of("message", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationError(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("요청이 올바르지 않습니다.");

        return Map.of("message", message);
    }
}
```

`@RestControllerAdvice`로 전역 예외를 처리합니다:
- **404 Not Found** — 사용자/주문을 찾지 못한 경우
- **400 Bad Request** — 요청 데이터 검증 실패 시

---

## 5. 테스트

### 테스트 전략 개요

이 프로젝트에서는 두 가지 계층의 테스트를 작성합니다.

| 구분 | 어노테이션 | 대상 | DB | Mock 여부 |
|------|-----------|------|----|-----------|
| 컨트롤러 테스트 | `@WebMvcTest` | HTTP 요청/응답, 직렬화 | 없음 | Service를 `@MockBean`으로 목 처리 |
| 서비스 통합 테스트 | `@SpringBootTest` | 비즈니스 로직, DB 연동 | H2 인메모리 | 실제 빈 사용 (`@Autowired`) |

---

### 서비스 통합 테스트 (Service Integration Test)

서비스 테스트는 실제 DB와 연동하여 **비즈니스 로직이 올바르게 동작하는지** 검증합니다. 컨트롤러 테스트가 HTTP 레이어에 집중하는 것과 달리, 서비스 테스트는 **데이터 저장/조회/변경이 의도대로 이루어지는지**, 그리고 **예외 조건이 올바르게 처리되는지**에 집중합니다.

#### 핵심 어노테이션

```java
@SpringBootTest   // 전체 Spring 컨텍스트 로드 (실제 빈 사용)
@Transactional    // 각 테스트 후 롤백 → 테스트 간 데이터 격리
class OrderServiceIntegrationTest {

    @Autowired    // 실제 빈 주입 (Mock 아님)
    private OrderService orderService;
}
```

**`@SpringBootTest`**
- 애플리케이션 전체 컨텍스트를 로드합니다.
- Service, Repository, Entity 등 모든 빈이 실제로 동작합니다.
- `@WebMvcTest`와 달리 HTTP 레이어 없이 서비스 레이어를 직접 호출합니다.

**`@Transactional` (테스트용)**

프로덕션 코드의 `@Transactional`과 **동작 방식이 다릅니다.**

```
프로덕션 @Transactional
  → 메서드 정상 종료 시 커밋 (DB에 영구 반영)
  → 예외 발생 시 롤백

테스트 클래스의 @Transactional
  → 테스트 메서드 종료 후 항상 롤백 (커밋하지 않음)
```

이 동작 덕분에 테스트 간 데이터 격리가 보장됩니다:

```
@BeforeEach → setUp()
  └─ userService.registerUser() → DB에 유저 저장
  └─ productService.addProduct() → DB에 상품 저장

@Test → placeOrder_success()
  └─ orderService.placeOrder() → 주문 저장, 재고 감소
  └─ assert 검증

--- 테스트 종료 → 자동 롤백 ---
DB 원상복구: 유저, 상품, 주문 모두 사라짐

@Test → placeOrder_insufficientStock_throwsException()
  └─ @BeforeEach 재실행 → 유저, 상품을 새로 생성
  └─ 이전 테스트의 흔적 없음 → 깨끗한 상태 보장
```

롤백이 없으면 테스트마다 데이터가 누적되어 "이미 존재하는 이메일" 오류 등으로 테스트가 서로 간섭합니다. `@BeforeEach`에서 저장한 데이터도 동일한 트랜잭션 내에서 동작하므로 롤백 범위에 함께 포함됩니다.

#### 테스트 DB 설정

서비스 테스트는 실제 MySQL 대신 **H2 인메모리 DB**를 사용합니다. `src/test/resources/application.yml`에서 테스트 전용 설정을 오버라이드합니다.

```yaml
# src/test/resources/application.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MYSQL   # MySQL 호환 모드
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop              # 테스트 시작 시 스키마 생성, 종료 시 삭제
```

- `MODE=MYSQL`: H2가 MySQL 문법을 흉내 내도록 설정합니다.
- `create-drop`: 테스트 컨텍스트 시작 시 테이블을 새로 만들고, 종료 시 전부 삭제합니다.
- 외부 DB 없이 어디서든 테스트를 실행할 수 있어 CI/CD 환경에 적합합니다.

#### `@BeforeEach`로 공통 데이터 준비

각 테스트마다 필요한 기반 데이터(유저, 상품)를 `@BeforeEach`에서 세팅합니다.

```java
private Long userId;
private Long productId;

@BeforeEach
void setUp() {
    CreateUserResponse user = userService.registerUser(
            new CreateUserRequest("주문자", "order-test@example.com", "pass1234")
    );
    userId = user.userId();

    ProductResponse product = productService.addProduct(
            new CreateProductRequest("맥북 프로", 2500000, 100)
    );
    productId = product.productId();
}
```

- 직접 Repository를 호출하지 않고 **Service를 통해 데이터를 생성**합니다. 이렇게 하면 서비스 레이어의 검증 로직도 함께 거쳐 더 현실적인 테스트 환경을 만들 수 있습니다.
- 생성된 ID를 인스턴스 변수에 저장하여 각 `@Test`에서 재사용합니다.

#### `@Nested` + `@DisplayName`으로 테스트 구조화

```java
@Nested
@DisplayName("주문 생성 (placeOrder)")
class PlaceOrderTest {

    @Test
    @DisplayName("정상적인 주문 생성 → 주문 정보 반환 및 재고 감소")
    void placeOrder_success() { ... }

    @Test
    @DisplayName("존재하지 않는 유저로 주문 → UserNotFoundException")
    void placeOrder_userNotFound_throwsException() { ... }

    @Test
    @DisplayName("재고 부족 시 주문 → InsufficientStockException")
    void placeOrder_insufficientStock_throwsException() { ... }
}
```

- `@Nested`: 메서드 단위로 테스트를 그룹화합니다. 하나의 서비스 메서드에 대한 성공/실패 케이스를 묶어서 관리합니다.
- `@DisplayName`: 테스트 실행 결과에 표시될 한글 설명을 붙입니다. `메서드명 → 기대 결과` 형식으로 작성하면 리포트만 봐도 어떤 케이스인지 파악할 수 있습니다.

#### 성공 케이스 — 반환값 + 부수 효과 모두 검증

"부수 효과"란 메서드가 **반환값 외에 추가로 일으키는 상태 변화**입니다.

```
placeOrder() 실행 결과
 ├─ 반환값:   CreateOrderResponse (orderId, deliveryStatus ...)
 └─ 부수 효과: 상품 재고 100 → 99 감소 (DB 상태 변화)
```

반환값만 검증하면 "주문 응답 객체가 잘 만들어졌다"는 것만 알 수 있습니다. **재고가 실제로 줄었는지는 별도로 조회해야** 합니다.

```java
@Test
@DisplayName("정상적인 주문 생성 → 주문 정보 반환 및 재고 감소")
void placeOrder_success() {
    CreateOrderRequest request = new CreateOrderRequest(
            userId, productId, 1, "서울시 강남구 테헤란로 123"
    );

    CreateOrderResponse response = orderService.placeOrder(request);

    // 1) 반환값 검증
    assertThat(response.orderId()).isNotNull();
    assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.ORDERED);

    // 2) 부수 효과 검증 — 주문 후 DB에서 재고를 다시 조회해서 확인
    ProductResponse updatedProduct = productService.getProduct(productId);
    assertThat(updatedProduct.stock()).isEqualTo(99);  // 100 → 99
}
```

컨트롤러 테스트는 Service가 `@MockBean`이라 실제 로직이 실행되지 않으므로 재고 감소 같은 부수 효과를 검증할 수 없습니다. 서비스 테스트에서는 **실제 DB에 반영된 결과**를 다시 조회하여 검증합니다.

주문 취소 테스트에서 부수 효과는 더 명확합니다:

```java
// 취소 "전" 재고를 먼저 기록
ProductResponse beforeCancel = productService.getProduct(productId);
int stockBeforeCancel = beforeCancel.stock();   // 예: 97

orderService.cancelOrder(orderId);              // 수량 3개짜리 주문 취소

// 취소 "후" 재고가 정확히 3만큼 늘었는지 확인
ProductResponse afterCancel = productService.getProduct(productId);
assertThat(afterCancel.stock()).isEqualTo(stockBeforeCancel + 3);  // 97 → 100
```

#### 예외 케이스 — `assertThatThrownBy`

```java
@Test
@DisplayName("재고 부족 시 주문 → InsufficientStockException")
void placeOrder_insufficientStock_throwsException() {
    CreateOrderRequest request = new CreateOrderRequest(
            userId, productId, 999, "서울시 강남구"
    );

    assertThatThrownBy(() -> orderService.placeOrder(request))
            .isInstanceOf(InsufficientStockException.class);
}
```

- `assertThatThrownBy(() -> ...)`: AssertJ 방식의 예외 검증으로, 람다 안의 코드를 실행했을 때 예외가 발생하는지 확인합니다.
- `.isInstanceOf(SomeException.class)`: 발생한 예외의 타입을 검증합니다.
- JUnit의 `assertThrows`보다 가독성이 높고 체이닝이 자유롭습니다.

#### 상태 전이 검증

서비스 테스트에서는 여러 단계를 순서대로 실행하며 **상태 흐름 전체**를 검증할 수 있습니다.

```java
@Test
@DisplayName("ORDERED → PREPARING → SHIPPED → DELIVERED 순차 변경")
void updateDeliveryStatus_fullFlow_success() {
    DeliveryStatusResponse step1 = orderService.updateDeliveryStatus(
            orderId, new UpdateDeliveryStatusRequest(DeliveryStatus.PREPARING)
    );
    assertThat(step1.deliveryStatus()).isEqualTo(DeliveryStatus.PREPARING);

    DeliveryStatusResponse step2 = orderService.updateDeliveryStatus(
            orderId, new UpdateDeliveryStatusRequest(DeliveryStatus.SHIPPED)
    );
    assertThat(step2.deliveryStatus()).isEqualTo(DeliveryStatus.SHIPPED);

    DeliveryStatusResponse step3 = orderService.updateDeliveryStatus(
            orderId, new UpdateDeliveryStatusRequest(DeliveryStatus.DELIVERED)
    );
    assertThat(step3.deliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
}
```

같은 트랜잭션 안에서 순차적으로 상태를 변경하고 각 단계에서 결과를 검증합니다.

#### 주문 취소 — 재고 복구까지 검증

```java
@Test
@DisplayName("ORDERED 상태 주문 취소 → CANCELLED 상태 + 재고 복구")
void cancelOrder_success() {
    // 취소 전 재고 기록
    ProductResponse beforeCancel = productService.getProduct(productId);
    int stockBeforeCancel = beforeCancel.stock();

    DeliveryStatusResponse response = orderService.cancelOrder(orderId);

    assertThat(response.deliveryStatus()).isEqualTo(DeliveryStatus.CANCELLED);

    // 재고가 주문 수량(3)만큼 복구됐는지 확인
    ProductResponse afterCancel = productService.getProduct(productId);
    assertThat(afterCancel.stock()).isEqualTo(stockBeforeCancel + 3);
}
```

취소 전후 재고를 직접 조회하여 수량이 정확히 복구되었는지 수치로 검증합니다.

---

### 동시성 테스트 (Concurrency Test)

비관적 락(Pessimistic Lock) 같은 동시성 제어 로직은 일반 서비스 테스트로는 검증할 수 없습니다. 여러 스레드가 동시에 요청을 보내는 상황을 직접 만들어야 합니다.

#### `@Transactional`을 붙이지 않는 이유

```java
@SpringBootTest
// @Transactional 없음 — 의도적으로 제거
class OrderConcurrencyTest {
```

동시성 테스트에 `@Transactional`을 붙이면 **모든 스레드가 하나의 트랜잭션 안에서 실행**됩니다. 비관적 락은 트랜잭션 간의 경합을 막는 장치인데, 같은 트랜잭션이면 락 경합 자체가 발생하지 않아 동시성 문제를 재현할 수 없습니다.

```
@Transactional 있을 때 (잘못된 설정)
  메인 트랜잭션 시작
   ├─ 스레드 1 → placeOrder() → 같은 트랜잭션 내에서 실행
   ├─ 스레드 2 → placeOrder() → 같은 트랜잭션 내에서 실행
   └─ 락 경합 없음 → 동시성 문제 재현 불가

@Transactional 없을 때 (올바른 설정)
   ├─ 스레드 1 → 자체 트랜잭션 시작 → SELECT FOR UPDATE → 커밋
   ├─ 스레드 2 → 자체 트랜잭션 시작 → SELECT FOR UPDATE → 락 대기 → 커밋
   └─ 트랜잭션 간 경합 발생 → 비관적 락 효과 검증 가능
```

`@Transactional`을 빼면 테스트가 끝난 뒤 데이터가 DB에 남습니다. H2의 `create-drop` 설정이 테스트 컨텍스트 종료 시 전체 스키마를 삭제해 정리합니다.

#### CountDownLatch로 동시 실행 제어

`ExecutorService.submit()`은 작업을 스레드 풀에 넘기고 **즉시 반환**합니다. 아무 동기화가 없으면 100개 스레드가 다 끝나기 전에 `assert`가 실행됩니다.

```java
// CountDownLatch 없는 경우 (잘못된 예)
for (int i = 0; i < 100; i++) {
    executorService.submit(() -> orderService.placeOrder(...));
}
// 이 시점에 스레드들이 아직 실행 중일 수 있음
assertThat(result.stock()).isZero();  // 틀린 결과가 나올 수 있음
```

`CountDownLatch`는 지정한 카운트가 0이 될 때까지 `await()`을 호출한 스레드를 블로킹합니다:

```java
CountDownLatch latch = new CountDownLatch(100);  // 카운터: 100

for (int i = 0; i < 100; i++) {
    executorService.submit(() -> {
        try {
            orderService.placeOrder(...);
            successCount.incrementAndGet();
        } catch (Exception e) {
            failCount.incrementAndGet();
        } finally {
            latch.countDown();  // 성공/실패 여부와 무관하게 카운터 -1
        }                       // 100 → 99 → 98 → ... → 0
    });
}

latch.await();  // 카운터가 0이 될 때까지 메인 스레드 블로킹
                // = 100개 스레드 모두 완료될 때까지 대기

// 여기부터는 모든 스레드가 끝난 것이 보장됨
assertThat(result.stock()).isZero();  // 안전하게 검증 가능
```

`countDown()`을 `finally`에 넣는 이유는 예외가 발생해도 반드시 카운트를 감소시켜야 하기 때문입니다. `try` 블록에 넣으면 예외 발생 시 `countDown()`이 호출되지 않아 `latch.await()`이 영원히 블로킹됩니다.

#### AtomicInteger — 스레드 안전한 카운터

여러 스레드가 동시에 `int` 변수를 읽고 쓰면 값이 손실됩니다:

```
일반 int successCount = 0 일 때

스레드 A: successCount 읽기 → 5
스레드 B: successCount 읽기 → 5   ← A가 아직 저장하기 전에 읽음
스레드 A: 5 + 1 = 6, 저장
스레드 B: 5 + 1 = 6, 저장         ← B의 증가분이 A에 덮어씌워짐
결과: 2번 증가했는데 6만 반영 → 값 손실
```

`AtomicInteger`는 읽기-증가-저장을 **원자적(CAS, Compare-And-Swap)**으로 처리해서 중간에 끊기지 않습니다:

```java
AtomicInteger successCount = new AtomicInteger(0);
successCount.incrementAndGet();  // 읽기 + 증가 + 저장이 한 번에 (분리 불가)
```

`AtomicInteger`가 정확하지 않으면 비관적 락이 실제로 동작해서 10개만 성공했어도 `successCount`가 9나 11로 읽힐 수 있어 **테스트 자체를 신뢰할 수 없습니다.**

#### 동시성 테스트 검증

```java
// 재고 100개, 스레드 100개 → 모두 성공해야 함
assertThat(successCount.get()).isEqualTo(100);
assertThat(failCount.get()).isZero();
assertThat(result.stock()).isZero();

// 재고 10개, 스레드 100개 → 정확히 10개만 성공
assertThat(successCount.get()).isEqualTo(10);
assertThat(failCount.get()).isEqualTo(90);
assertThat(result.stock()).isZero();
```

비관적 락이 없으면 동시 읽기로 인해 재고보다 더 많은 주문이 성공하는 **초과 판매(over-selling)**가 발생합니다. 비관적 락(`SELECT ... FOR UPDATE`)이 정상 동작하면 재고 수량 정확히 만큼만 성공하고 나머지는 `InsufficientStockException`으로 실패합니다.

---

### UserControllerTest

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void registerUser_returnsCreatedUser() throws Exception {
        when(userService.registerUser(any()))
                .thenReturn(new CreateUserResponse(1L, "홍길동", "hong@example.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "홍길동",
                                  "email": "hong@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("hong@example.com"));
    }
}
```

### OrderControllerTest

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void placeOrder_returnsCreatedOrder() throws Exception {
        when(orderService.placeOrder(any()))
                .thenReturn(new CreateOrderResponse(
                        22L, 1L, "노트북", 1, "서울시 강남구",
                        DeliveryStatus.ORDERED,
                        LocalDateTime.parse("2026-01-01T09:00:00")
                ));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "productName": "노트북",
                                  "quantity": 1,
                                  "shippingAddress": "서울시 강남구"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(22))
                .andExpect(jsonPath("$.deliveryStatus").value("ORDERED"));
    }

    @Test
    void checkDeliveryStatus_returnsCurrentState() throws Exception {
        when(orderService.checkDeliveryStatus(10L))
                .thenReturn(new DeliveryStatusResponse(
                        10L, DeliveryStatus.SHIPPED,
                        LocalDateTime.parse("2026-01-01T10:00:00")));

        mockMvc.perform(get("/api/orders/10/delivery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.deliveryStatus").value("SHIPPED"));
    }
}
```

`@WebMvcTest`를 사용하여 컨트롤러 계층만 슬라이스 테스트합니다. Service는 `@MockBean`으로 목 처리하여 **HTTP 요청/응답** 검증에 집중합니다.

---

## 정리

| 구분 | 내용 |
|------|------|
| 패키지 구조 | 도메인별 분리 (`user`, `order`, `common`) |
| DTO | Java `record`로 간결하게 정의 |
| 검증 | Jakarta Bean Validation (`@NotBlank`, `@Email`, `@Min`) |
| 예외 처리 | 커스텀 예외 + `@RestControllerAdvice` 전역 핸들러 |
| 트랜잭션 | 클래스 레벨 `@Transactional`, 조회는 `readOnly = true` |
| 컨트롤러 테스트 | `@WebMvcTest` + `MockMvc`로 HTTP 요청/응답 검증 |
| 서비스 통합 테스트 | `@SpringBootTest` + H2로 비즈니스 로직 및 DB 연동 검증 |
| 동시성 테스트 | `@SpringBootTest` (트랜잭션 없음) + `CountDownLatch`로 락 정합성 검증 |
