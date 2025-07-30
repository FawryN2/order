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
