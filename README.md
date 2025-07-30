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
Order Management API Documentation
This document describes the REST API endpoints provided by the OrderController class in the com.fawry_fridges.order.controller package. The API manages orders for the Fawry Fridges application, allowing users to create, retrieve, and search orders. The application integrates with RabbitMQ for asynchronous message processing, particularly for order creation and checkout operations.
Table of Contents

Overview
RabbitMQ Integration
API Endpoints
Create a New Order
Checkout and Create Order
Get Order by ID
Search Orders with Filters
Get Customer Orders


Request and Response Formats
Error Handling
Dependencies

Overview
The OrderController is a Spring Boot REST controller that handles HTTP requests related to order management. It uses the OrderService to process business logic and interacts with RabbitMQ to publish messages for asynchronous tasks, such as order creation and checkout. The controller supports pagination, sorting, and filtering for order searches.
RabbitMQ Integration
The application uses RabbitMQ as a message broker to handle asynchronous operations:

Order Creation: When a new order is created via the POST /api/orders endpoint, a message is published to a RabbitMQ queue (e.g., order-creation-queue) to process the order asynchronously.
Checkout: The POST /api/orders/checkout endpoint publishes a message to a RabbitMQ queue (e.g., order-checkout-queue) to handle payment processing, inventory updates, and order confirmation in the background.
Queue Configuration: Ensure RabbitMQ is running and configured with the appropriate queues and exchanges. The application uses Spring AMQP to integrate with RabbitMQ.

Example RabbitMQ configuration in application.yml:
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

API Endpoints
Create a New Order

Endpoint: POST /api/orders
Description: Creates a new order based on the provided order details.
Request Body: OrderDto (JSON)
Response: OrderDto (JSON) with HTTP status 201 Created
Example:curl -X POST http://localhost:8080/api/orders \
-H "Content-Type: application/json" \
-d '{"userId":"user123","items":[{"productId":"prod1","quantity":2}],"totalAmount":100.0}'


Response Example:{
  "id": "order123",
  "userId": "user123",
  "items": [{"productId": "prod1", "quantity": 2}],
  "totalAmount": 100.0,
  "createdAt": "2025-07-30T22:20:00"
}



Checkout and Create Order

Endpoint: POST /api/orders/checkout
Description: Processes the checkout for an order, including payment and shipping details. Publishes a message to RabbitMQ for asynchronous processing.
Request Body: CheckoutRequest (JSON)
Response: CheckoutResponse (JSON) with HTTP status 200 OK
Example:curl -X POST http://localhost:8080/api/orders/checkout \
-H "Content-Type: application/json" \
-d '{"orderId":"order123","userId":"user123","shippingAddressId":"addr1","paymentMethod":"CREDIT_CARD","couponId":"coupon1"}'


Response Example:{
  "orderId": "order123",
  "message": "Order placed successfully",
  "status": "SUCCESS"
}



Get Order by ID

Endpoint: GET /api/orders/{orderId}
Description: Retrieves an order by its unique ID.
Path Parameter: orderId (String)
Response: OrderDto (JSON) with HTTP status 200 OK
Example:curl -X GET http://localhost:8080/api/orders/order123


Response Example:{
  "id": "order123",
  "userId": "user123",
  "items": [{"productId": "prod1", "quantity": 2}],
  "totalAmount": 100.0,
  "createdAt": "2025-07-30T22:20:00"
}



Search Orders with Filters

Endpoint: GET /api/orders/search
Description: Searches orders for a customer with optional date filters, pagination, and sorting.
Query Parameters:
customerId (String, required): The ID of the customer.
startDate (LocalDate, optional): Start date for filtering (ISO format, e.g., 2025-01-01).
endDate (LocalDate, optional): End date for filtering (ISO format, e.g., 2025-07-30).
page (int, default: 0): Page number for pagination.
size (int, default: 10): Number of records per page.
sortBy (String, default: createdAt): Field to sort by.
sortDirection (String, default: desc): Sort direction (asc or desc).


Response: Page<OrderDto> (JSON) with HTTP status 200 OK
Example:curl -X GET "http://localhost:8080/api/orders/search?customerId=user123&startDate=2025-01-01&endDate=2025-07-30&page=0&size=10&sortBy=createdAt&sortDirection=desc"


Response Example:{
  "content": [
    {
      "id": "order123",
      "userId": "user123",
      "items": [{"productId": "prod1", "quantity": 2}],
      "totalAmount": 100.0,
      "createdAt": "2025-07-30T22:20:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {"property": "createdAt", "direction": "DESC"}
  },
  "totalElements": 1,
  "totalPages": 1
}



Get Customer Orders

Endpoint: GET /api/orders/customer/{customerId}
Description: Retrieves all orders for a specific customer within the last year, with pagination.
Path Parameter: customerId (String)
Query Parameters:
page (int, default: 0): Page number for pagination.
size (int, default: 10): Number of records per page.


Response: Page<OrderDto> (JSON) with HTTP status 200 OK
Example:curl -X GET "http://localhost:8080/api/orders/customer/user123?page=0&size=10"


Response Example:{
  "content": [
    {
      "id": "order123",
      "userId": "user123",
      "items": [{"productId": "prod1", "quantity": 2}],
      "totalAmount": 100.0,
      "createdAt": "2025-07-30T22:20:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {"property": "createdAt", "direction": "DESC"}
  },
  "totalElements": 1,
  "totalPages": 1
}



Request and Response Formats

OrderDto: Represents an order with fields like id, userId, items, totalAmount, and createdAt.
CheckoutRequest: Contains orderId, userId, shippingAddressId, paymentMethod, and couponId.
CheckoutResponse: Includes orderId, message, and status.

Example OrderDto:
{
  "id": "order123",
  "userId": "user123",
  "items": [
    {
      "productId": "prod1",
      "quantity": 2
    }
  ],
  "totalAmount": 100.0,
  "createdAt": "2025-07-30T22:20:00"
}

Error Handling

The controller logs errors using SLF4J (@Slf4j) and throws exceptions for invalid requests or processing failures.
Common HTTP status codes:
201 Created: Successful order creation.
200 OK: Successful retrieval or checkout.
400 Bad Request: Invalid request data.
404 Not Found: Order or customer not found.
500 Internal Server Error: Server-side errors.



Dependencies

Spring Boot: For building the REST API.
Spring Data JPA: For database interactions.
Spring AMQP: For RabbitMQ integration.
Lombok: For reducing boilerplate code.
SLF4J: For logging.

To run the application:

Ensure RabbitMQ is installed and running.
Configure RabbitMQ connection details in application.yml.
Start the Spring Boot application.

For more details, refer to the Spring Boot documentation and RabbitMQ documentation.
