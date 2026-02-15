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
| 테스트 | `@WebMvcTest` + MockMvc로 컨트롤러 슬라이스 테스트 |
