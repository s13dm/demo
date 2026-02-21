# 개발 스펙 문서 (Development Specification)

> 작성일: 2026-02-21
> 프로젝트: Spring Boot 주문/배송 관리 REST API

---

## 1. 기술 스택 (Tech Stack)

| 구분 | 기술 | 버전 |
|------|------|------|
| Language | Java | 25 |
| Framework | Spring Boot | 4.0.2 |
| ORM | Spring Data JPA | (Spring Boot 관리) |
| Validation | Spring Boot Starter Validation | (Jakarta Validation) |
| Build Tool | Gradle | 9.3 |
| DB (운영) | MySQL | (최신) |
| DB (테스트) | H2 (In-memory, MySQL mode) | (최신) |
| API 문서 | SpringDoc OpenAPI (Swagger UI) | 2.8.4 |
| 코드 생성 | Lombok | (최신) |
| 테스트 | JUnit 5 (Spring Boot Test Classic) | (최신) |

---

## 2. 프로젝트 구조 (Project Structure)

```
src/
├── main/
│   ├── java/com/demo/
│   │   ├── DemoApplication.java              # 앱 진입점
│   │   ├── common/exception/                 # 공통 예외 처리
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── DuplicateEmailException.java
│   │   │   ├── DuplicateProductNameException.java
│   │   │   ├── InsufficientStockException.java
│   │   │   ├── InvalidCredentialsException.java
│   │   │   ├── OrderNotFoundException.java
│   │   │   ├── ProductNotFoundException.java
│   │   │   ├── UserNotFoundException.java
│   │   │   └── UnauthorizedException.java
│   │   ├── config/
│   │   │   └── SwaggerConfig.java            # Swagger 설정
│   │   ├── user/                             # 사용자 도메인
│   │   │   ├── controller/UserController.java
│   │   │   ├── service/UserService.java
│   │   │   ├── repository/UserRepository.java
│   │   │   ├── entity/User.java
│   │   │   ├── entity/Role.java
│   │   │   └── dto/                          # 요청/응답 DTO 5종
│   │   ├── order/                            # 주문 도메인
│   │   │   ├── controller/OrderController.java
│   │   │   ├── service/OrderService.java
│   │   │   ├── repository/OrderRepository.java
│   │   │   ├── entity/Order.java
│   │   │   ├── entity/DeliveryStatus.java
│   │   │   └── dto/                          # 요청/응답 DTO 5종
│   │   └── product/                          # 상품 도메인
│   │       ├── controller/ProductController.java
│   │       ├── service/ProductService.java
│   │       ├── repository/ProductRepository.java
│   │       ├── entity/Product.java
│   │       └── dto/                          # 요청/응답 DTO 2종
│   └── resources/
│       └── application.yml
└── test/
    ├── java/com/demo/
    │   ├── order/service/OrderServiceIntegrationTest.java
    │   ├── order/service/OrderConcurrencyTest.java
    │   ├── order/controller/OrderControllerTest.java
    │   ├── product/service/ProductServiceIntegrationTest.java
    │   ├── product/controller/ProductControllerTest.java
    │   ├── user/service/UserServiceIntegrationTest.java
    │   └── user/controller/UserControllerTest.java
    └── resources/
        └── application.yml
```

---

## 3. 아키텍처 패턴 (Architecture Pattern)

### 레이어드 아키텍처 (Layered Architecture)

```
Client → Controller → Service → Repository → Database
```

- **도메인 중심 패키지 구조**: `user`, `order`, `product` 독립적 도메인 패키지
- **단방향 의존성**: 상위 레이어가 하위 레이어를 의존, 역방향 불가
- **예외 중앙화**: `@RestControllerAdvice`로 모든 예외를 한 곳에서 처리

---

## 4. 데이터베이스 설계 (Database Design)

### 4.1 ERD 관계

```
User (1) ──────< Order (N)
Product (1) ───< Order (N)
```

### 4.2 테이블 스펙

#### `users` 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 사용자 ID |
| email | VARCHAR | NOT NULL, UNIQUE | 이메일 (로그인 키) |
| name | VARCHAR | NOT NULL | 이름 |
| password | VARCHAR | NOT NULL | 비밀번호 (평문 저장) |
| role | VARCHAR | NOT NULL | 권한 (ROLE_USER / ROLE_ADMIN) |

#### `products` 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 상품 ID |
| name | VARCHAR | NOT NULL, UNIQUE | 상품명 |
| price | INT | NOT NULL | 가격 |
| stock | INT | NOT NULL | 재고 수량 |

#### `orders` 테이블

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 주문 ID |
| user_id | BIGINT | FK → users.id, NOT NULL | 주문자 |
| product_id | BIGINT | FK → products.id, NOT NULL | 주문 상품 |
| product_name | VARCHAR | NOT NULL | 주문 시점 상품명 (스냅샷) |
| quantity | INT | NOT NULL | 주문 수량 |
| shipping_address | VARCHAR | NOT NULL | 배송지 |
| delivery_status | VARCHAR | NOT NULL | 배송 상태 (Enum) |
| ordered_at | DATETIME | NOT NULL | 주문 일시 |

### 4.3 Enum 정의

#### `Role` Enum
```
ROLE_USER   - 일반 사용자
ROLE_ADMIN  - 관리자
```

#### `DeliveryStatus` Enum
```
ORDERED    - 주문 접수
PREPARING  - 준비 중
SHIPPED    - 배송 중
DELIVERED  - 배송 완료
CANCELLED  - 취소됨
```

---

## 5. API 스펙 (REST API Specification)

### 5.1 User API (`/api/users`)

#### POST `/api/users` - 일반 사용자 회원가입

- **Request Body**
  ```json
  {
    "name": "홍길동",
    "email": "user@example.com",
    "password": "password123"
  }
  ```
- **Response** `201 Created`
  ```json
  {
    "userId": 1,
    "name": "홍길동",
    "email": "user@example.com"
  }
  ```
- **에러**: `400` (유효성 검증 실패), `409` (이메일 중복)

---

#### POST `/api/users/admin` - 관리자 회원가입

- **Request Body**
  ```json
  {
    "name": "관리자",
    "email": "admin@example.com",
    "password": "adminpass",
    "adminSecretKey": "ADMIN_SECRET_2026"
  }
  ```
- **Response** `201 Created`
  ```json
  {
    "userId": 2,
    "name": "관리자",
    "email": "admin@example.com"
  }
  ```
- **에러**: `400` (시크릿 키 불일치), `409` (이메일 중복)

---

#### POST `/api/users/login` - 로그인

- **Request Body**
  ```json
  {
    "email": "user@example.com",
    "password": "password123"
  }
  ```
- **Response** `200 OK`
  ```json
  {
    "userId": 1,
    "name": "홍길동",
    "email": "user@example.com",
    "role": "ROLE_USER",
    "message": "로그인 성공"
  }
  ```
- **부작용**: HttpSession에 `userId`, `role` 저장
- **에러**: `401` (이메일/비밀번호 불일치)

---

#### POST `/api/users/logout` - 로그아웃

- **Response** `200 OK`
  ```json
  { "message": "로그아웃 성공" }
  ```
- **부작용**: 세션 무효화 (`session.invalidate()`)

---

#### GET `/api/users/{userId}/orders` - 사용자 주문 목록 조회

- **접근 제어**: 본인 또는 ROLE_ADMIN만 가능 (세션 기반 검증)
- **Response** `200 OK`
  ```json
  {
    "userId": 1,
    "userName": "홍길동",
    "totalOrders": 2,
    "orders": [...]
  }
  ```
- **에러**: `401` (미로그인), `403` (권한 없음), `404` (사용자 없음)

---

#### GET `/api/users/admin/deliveries` - 전체 사용자 배송 상태 조회 (관리자 전용)

- **접근 제어**: ROLE_ADMIN만 가능
- **Response** `200 OK` - 모든 사용자의 배송 목록 배열
- **에러**: `401` (미로그인), `403` (관리자 아님)

---

### 5.2 Order API (`/api/orders`)

#### POST `/api/orders` - 주문 생성

- **Request Body**
  ```json
  {
    "userId": 1,
    "productId": 1,
    "quantity": 2,
    "shippingAddress": "서울시 강남구 ..."
  }
  ```
- **Response** `201 Created`
  ```json
  {
    "orderId": 1,
    "userId": 1,
    "productName": "상품명",
    "quantity": 2,
    "shippingAddress": "서울시 강남구 ...",
    "deliveryStatus": "ORDERED",
    "orderedAt": "2026-02-21T10:00:00"
  }
  ```
- **내부 동작**: 비관적 락(PESSIMISTIC_WRITE)으로 상품 조회 후 재고 차감
- **에러**: `400` (재고 부족), `404` (사용자/상품 없음)

---

#### GET `/api/orders/{orderId}/delivery` - 배송 상태 조회

- **Response** `200 OK`
  ```json
  {
    "orderId": 1,
    "deliveryStatus": "SHIPPED",
    "orderedAt": "2026-02-21T10:00:00"
  }
  ```
- **에러**: `404` (주문 없음)

---

#### PATCH `/api/orders/{orderId}/delivery` - 배송 상태 업데이트

- **Request Body**
  ```json
  { "deliveryStatus": "SHIPPED" }
  ```
- **Response** `200 OK` - 업데이트된 배송 상태
- **에러**: `404` (주문 없음)

---

#### POST `/api/orders/{orderId}/cancel` - 주문 취소

- **제약**: `ORDERED` 상태일 때만 취소 가능
- **부작용**: 취소 시 해당 상품 재고 복구 (`increaseStock`)
- **Response** `200 OK` - CANCELLED 상태로 변경된 주문 정보
- **에러**: `400` (취소 불가 상태), `404` (주문 없음)

---

### 5.3 Product API (`/api/products`)

#### POST `/api/products` - 상품 등록

- **Request Body**
  ```json
  {
    "name": "노트북",
    "price": 1500000,
    "stock": 100
  }
  ```
- **Response** `201 Created`
  ```json
  {
    "productId": 1,
    "name": "노트북",
    "price": 1500000,
    "stock": 100
  }
  ```
- **에러**: `409` (상품명 중복)

---

#### GET `/api/products/{productId}` - 상품 단건 조회

- **Response** `200 OK` - 상품 상세 정보
- **에러**: `404` (상품 없음)

---

#### GET `/api/products` - 전체 상품 목록 조회

- **Response** `200 OK` - 상품 배열

---

## 6. 인증/인가 스펙 (Auth & Authorization)

### 방식: 세션 기반 인증 (HttpSession)

| 세션 키 | 타입 | 저장 시점 |
|---------|------|-----------|
| `userId` | `Long` | 로그인 성공 시 |
| `role` | `Role` | 로그인 성공 시 |

### 접근 제어 규칙

| 엔드포인트 | 접근 조건 |
|-----------|-----------|
| `GET /api/users/{userId}/orders` | 세션 존재 + (본인 ID 일치 OR ROLE_ADMIN) |
| `GET /api/users/admin/deliveries` | 세션 존재 + ROLE_ADMIN |
| 그 외 | 제한 없음 (세션 불필요) |

### 관리자 등록 시크릿 키
```
ADMIN_SECRET_2026
```

---

## 7. 예외 처리 스펙 (Exception Handling)

### 전역 예외 핸들러: `GlobalExceptionHandler` (`@RestControllerAdvice`)

| 예외 클래스 | HTTP 상태 | 발생 상황 |
|------------|----------|-----------|
| `UserNotFoundException` | 404 | 사용자 ID로 조회 실패 |
| `OrderNotFoundException` | 404 | 주문 ID로 조회 실패 |
| `ProductNotFoundException` | 404 | 상품 ID로 조회 실패 |
| `InvalidCredentialsException` | 401 | 이메일/비밀번호 불일치 |
| `UnauthorizedException` | 403 | 권한 부족 (미로그인, 권한 없음) |
| `DuplicateEmailException` | 409 | 이메일 중복 |
| `DuplicateProductNameException` | 409 | 상품명 중복 |
| `InsufficientStockException` | 400 | 재고 부족 |
| `IllegalStateException` | 400 | 잘못된 상태 (예: 취소 불가 주문) |
| `MethodArgumentNotValidException` | 400 | Jakarta Validation 실패 |

### 에러 응답 형식
```json
{ "message": "에러 설명 문자열" }
```

---

## 8. 동시성 제어 스펙 (Concurrency Control)

### 비관적 락 (Pessimistic Locking)

- **적용 위치**: `ProductRepository.findByIdWithPessimisticLock()`
- **락 유형**: `LockModeType.PESSIMISTIC_WRITE` (SELECT ... FOR UPDATE)
- **적용 이유**: 동시 주문 요청 시 재고 차감 중 다른 트랜잭션이 같은 row를 읽거나 수정하지 못하도록 차단하여 초과 판매(over-selling) 방지
- **트랜잭션 범위**: `OrderService.placeOrder()` 전체 메서드 (`@Transactional`)

### 주문 취소 시 재고 복구
- 주문 취소(`cancelOrder`) 시 동일 트랜잭션 내에서 `product.increaseStock(quantity)` 호출
- JPA Dirty Checking에 의해 별도 save 호출 없이 자동 반영

---

## 9. 트랜잭션 스펙 (Transaction Spec)

| 메서드 | 트랜잭션 설정 |
|--------|-------------|
| `OrderService` (클래스 레벨) | `@Transactional` (read-write) |
| `OrderService.checkDeliveryStatus()` | `@Transactional(readOnly = true)` |
| `UserService` (클래스 레벨) | `@Transactional` (read-write) |
| `UserService.login()` | `@Transactional(readOnly = true)` |
| `UserService.getUserOrders()` | `@Transactional(readOnly = true)` |
| `UserService.getAllUsersDeliveryStatus()` | `@Transactional(readOnly = true)` |

---

## 10. 설정 파일 스펙 (Configuration)

### `src/main/resources/application.yml` (운영)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/orderdb
    username: root
    password: 1234
  jpa:
    hibernate:
      ddl-auto: create
    properties:
      hibernate:
        format_sql: true
        show_sql: true
```

### `src/test/resources/application.yml` (테스트)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MYSQL
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        format_sql: true
```

---

## 11. 테스트 스펙 (Test Specification)

| 테스트 파일 | 유형 | 설명 |
|------------|------|------|
| `UserServiceIntegrationTest` | 통합 테스트 | 사용자 등록, 로그인, 조회 |
| `UserControllerTest` | 컨트롤러 테스트 | User API 엔드포인트 |
| `OrderServiceIntegrationTest` | 통합 테스트 (243줄) | 주문 생성, 취소, 배송 상태 변경 |
| `OrderConcurrencyTest` | 동시성 테스트 (151줄) | 동시 주문 시 재고 정합성 검증 |
| `OrderControllerTest` | 컨트롤러 테스트 | Order API 엔드포인트 |
| `ProductServiceIntegrationTest` | 통합 테스트 | 상품 등록, 조회 |
| `ProductControllerTest` | 컨트롤러 테스트 | Product API 엔드포인트 |

- **테스트 DB**: H2 In-memory (MySQL mode) — `create-drop` 전략으로 테스트마다 초기화
- **테스트 프레임워크**: JUnit 5 (`useJUnitPlatform()`)

---

## 12. API 문서 (Swagger)

- **접속 URL**: `http://localhost:8080/swagger-ui.html`
- **Swagger 설정**: `SwaggerConfig.java`
- **어노테이션 사용**: `@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`
- **그룹화**: `User`, `Order`, `Product` 3개 태그로 분류

---

## 13. 주요 비즈니스 규칙 (Business Rules)

1. **이메일 유일성**: 동일 이메일로 중복 회원가입 불가
2. **상품명 유일성**: 동일 상품명 중복 등록 불가
3. **재고 차감**: 주문 시 재고가 주문 수량보다 적으면 `InsufficientStockException`
4. **주문 취소 제약**: `ORDERED` 상태일 때만 취소 가능, 그 외 상태(`PREPARING`, `SHIPPED`, `DELIVERED`)에서는 취소 불가
5. **취소 시 재고 복구**: 주문 취소 시 해당 수량만큼 재고 자동 증가
6. **관리자 등록 제한**: 시크릿 키(`ADMIN_SECRET_2026`) 없이 관리자 계정 생성 불가
7. **주문 조회 권한**: 본인 주문만 조회 가능, 관리자는 모든 주문 조회 가능
8. **상품명 스냅샷**: 주문 생성 시점의 상품명을 `orders.product_name`에 저장 (상품명 변경 시에도 주문 내역 유지)

---

## 14. 알려진 제약사항 및 미구현 사항

| 항목 | 현황 |
|------|------|
| 비밀번호 암호화 | 미구현 (평문 저장) |
| Spring Security | 미적용 (수동 세션 관리) |
| 페이지네이션 | 미구현 (전체 조회만 지원) |
| 상품 수정/삭제 API | 미구현 |
| 사용자 수정/삭제 API | 미구현 |
| 정렬/필터링 | 주문 목록 `orderedAt DESC` 고정 |
| `getAllUsersDeliveryStatus` N+1 문제 | User 목록 조회 후 각 User별 Order 개별 조회 (N+1) |
