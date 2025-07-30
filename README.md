# Fawry-final-projejct-orders

<img width="1920" height="1080" alt="Workflow Diagram Whiteboard" src="https://github.com/user-attachments/assets/8a6b8378-8548-4632-ac00-c100f1e0305d" />


---

## 🗃️ Database Tables

### `orders`

Stores high-level information about customer orders, including shipping, payment, discounts, and status.

| Column                | Type        | Description                                               |
| --------------------- | ----------- | --------------------------------------------------------- |
| `id`                  | `uuid`      | Primary key (unique identifier for the order)             |
| `order_number`        | `Long`    | Public-facing unique order number                         |
| `user_id`             | `String`      | ID of the user placing the order                          |
| `shipping_address_id` | `String`      | Reference to the shipping address (from Shipping Service) |
| `payment_method`      | `String`      | Payment method used (e.g. card, cash, etc.)               |
| `status`              | `String`      | Current order status (`PENDING`, `PAID`, `SHIPPED`, etc.) |
| `total_price`         | `double`    | Total amount after item prices, discounts, and shipping   |
| `discount_total`      | `double`    | Total discount applied to the order                       |
| `shipping_cost`       | `double`    | Shipping fee for the order                                |
| `couponName`           | `String`    | Optional coupon reference (from Coupon Service)          |
| `created_at`          | `timestamp` | Timestamp when order was created                          |
| `updated_at`          | `timestamp` | Timestamp of last order update                            |
| `merchantId`          | `String` | merchantId                                                   |
| `withdrawalTxnId`          | `String` | withdrawalTxnId                                         |
| `items`          | `order` | Timestamp of last order update                                     |

---

### `order_items`

Stores individual items included in each order, with price breakdown and discount per item.

| Column             | Type      | Description                                                    |
| ------------------ | --------- | -------------------------------------------------------------- |
| `id`               | `uuid`    | Primary key (unique identifier for item record)                |
| `order_id`         | `uuid`    | Foreign key referencing `orders.id`                            |
| `product_id`       | `String`    | Product identifier (from Product Service)                      |
| `product_name`     | `String`    | Name of the product at the time of order                       |
| `unit_price`       | `double` | Price per unit before discount                                 |
| `quantity`         | `int`     | Number of units ordered                                        |
| `discount_applied` | `double` | Discount amount applied to this line item                      |
| `final_price`      | `double` | Total price = (`unit_price` × `quantity`) - `discount_applied` |



# order
# order
install rabbitMQ
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
Sure! Here's a clean and well-formatted `README.md` version of your **Order Management API Documentation** that you can paste directly into your GitHub repository:

---

# 📦 Order Management API Documentation

This document outlines the REST API endpoints for the **Order Management System**, implemented in the `OrderController` class under the `com.fawry_fridges.order.controller` package. The API supports creating, retrieving, and searching orders, with integration to **RabbitMQ** for asynchronous processing.

> All endpoints are hosted at: `http://localhost:8081/api/orders`

---

## 📚 Table of Contents

* [Overview](#overview)
* [RabbitMQ Integration](#rabbitmq-integration)
* [API Endpoints](#api-endpoints)

  * [Create a New Order](#create-a-new-order)
  * [Checkout and Create Order](#checkout-and-create-order)
  * [Get Order by ID](#get-order-by-id)
  * [Get Customer Orders](#get-customer-orders)
  * [Search Orders with Filters](#search-orders-with-filters)
* [Error Handling](#error-handling)
* [Dependencies](#dependencies)
* [Setup Instructions](#setup-instructions)

---

## 📝 Overview

The `OrderController` is a Spring Boot REST controller that handles HTTP requests for order management. It delegates business logic to `OrderService` and uses **RabbitMQ** for tasks like order creation and checkout asynchronously.

Key Features:

* RESTful endpoints
* Pagination, sorting, and filtering
* Asynchronous messaging with RabbitMQ

---

## 🐇 RabbitMQ Integration

The application integrates with **RabbitMQ** for non-blocking processing:

* **Order Creation**: Publishes messages to `order-creation-queue`
* **Order Checkout**: Publishes messages to `order-checkout-queue`

#### 🔧 Sample RabbitMQ Configuration (application.yml)

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

---

## 🔗 API Endpoints

### ➕ Create a New Order

* **Endpoint**: `POST /api/orders`
* **Description**: Creates a new order with product and pricing details.

#### Request Example

```json
{
  "userId": "user-455",
  "merchantId": "merchant-789",
  "couponId": "WELCOME10",
  "totalPrice": 150.50,
  "shippingCost": 10.00,
  "discountTotal": 15.00,
  "items": [
    {
      "productId": "AK-MM-RD",
      "storeId": "1",
      "quantity": 2,
      "unitPrice": 75.00,
      "finalPrice": 65.00
    },
    {
      "productId": "AA-LL-XM",
      "storeId": "1",
      "quantity": 1,
      "unitPrice": 90.50,
      "finalPrice": 85.50
    }
  ]
}
```

#### Response Example

<details><summary>Click to expand</summary>

```json
{
  "id": "f2ffd0fc-e828-43ad-a808-b7ff6ec779ab",
  "orderNumber": 3,
  "status": "PENDING",
  ...
  "items": [...]
}
```

</details>

---

### 🧾 Checkout and Create Order

* **Endpoint**: `POST /api/orders/checkout`
* **Description**: Processes checkout, applies coupon, and updates payment/shipping info.

#### Request Example

```json
{
  "orderId": "f2ffd0fc-e828-43ad-a808-b7ff6ec779ab",
  "userId": "user-456",
  "shippingAddressId": "addr-123",
  "paymentMethod": "CREDIT_CARD",
  "couponId": 1
}
```

#### Response Example

```json
{
  "orderId": "f2ffd0fc-e828-43ad-a808-b7ff6ec779ab",
  "message": "Order placed successfully",
  "status": "SUCCESS",
  "timestamp": "2025-07-30T21:44:56.084562"
}
```

---

### 🔍 Get Order by ID

* **Endpoint**: `GET /api/orders/{orderId}`
* **Description**: Retrieves full details of an order by its ID.

---

### 👤 Get Customer Orders

* **Endpoint**: `GET /api/orders/customer/{customerId}?page=0&size=10`
* **Description**: Retrieves orders for a specific customer with pagination.

---

### 🔎 Search Orders with Filters

* **Endpoint**:
  `GET /api/orders/search?startDate=2025-07-30&endDate=2025-07-30&page=0&size=5&sortBy=orderNumber&sortDirection=desc&customerId=user-455`

* **Description**: Retrieves filtered orders with support for pagination and sorting.

---

## ❗ Error Handling

The API uses standard HTTP status codes:

| Status Code                 | Description                    |
| --------------------------- | ------------------------------ |
| `200 OK`                    | Request processed successfully |
| `201 Created`               | New order created              |
| `400 Bad Request`           | Invalid request payload        |
| `404 Not Found`             | Order or user not found        |
| `500 Internal Server Error` | Server-side failure            |

---

## ⚙️ Dependencies

* **Spring Boot** – Web framework
* **Spring Data JPA** – ORM layer
* **Spring AMQP** – RabbitMQ messaging
* **Lombok** – Simplifies Java boilerplate
* **SLF4J** – Logging

---

## 🚀 Setup Instructions

1. Install and run RabbitMQ locally or on Docker.
2. Configure `application.yml` with RabbitMQ credentials.
3. Build and run the Spring Boot application:

   ```bash
   ./mvnw clean install
   ./mvnw spring-boot:run
   ```

---

## 📖 References

* [Spring Boot Docs](https://spring.io/projects/spring-boot)
* [RabbitMQ Official Docs](https://www.rabbitmq.com/documentation.html)

---

Let me know if you'd like to include authentication, Swagger support, Docker instructions, or database setup next!

