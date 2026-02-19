# 주문/배송 확인 단일 서비스 (Spring Boot + JPA + MySQL)

## 실행 환경
- Java 25
- Spring Boot
- Spring Data JPA
- MySQL

## 주요 API
### 1) 사용자 등록
`POST /api/users`
```json
{
  "name": "홍길동",
  "email": "hong@example.com"
}
```

### 2) 주문 생성
`POST /api/orders`
```json
{
  "userId": 1,
  "productName": "노트북",
  "quantity": 1,
  "shippingAddress": "서울시 강남구"
}
```

### 3) 배송 상태 조회
`GET /api/orders/{orderId}/delivery`

### 4) 배송 상태 변경(관리자/테스트용)
`PATCH /api/orders/{orderId}/delivery`
```json
{
  "deliveryStatus": "SHIPPED"
}
```


