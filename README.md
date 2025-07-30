# Fawry-final-projejct-orders

<img width="1920" height="1080" alt="Workflow Diagram Whiteboard" src="https://github.com/user-attachments/assets/8a6b8378-8548-4632-ac00-c100f1e0305d" />


---

## 🗃️ Database Tables

### `orders`

Stores high-level information about customer orders, including shipping, payment, discounts, and status.

| Column                | Type        | Description                                               |
| --------------------- | ----------- | --------------------------------------------------------- |
| `id`                  | `uuid`      | Primary key (unique identifier for the order)             |
| `order_number`        | `bigint`    | Public-facing unique order number                         |
| `user_id`             | `uuid`      | ID of the user placing the order                          |
| `shipping_address_id` | `text`      | Reference to the shipping address (from Shipping Service) |
| `payment_method`      | `text`      | Payment method used (e.g. card, cash, etc.)               |
| `status`              | `enum`      | Current order status (`PENDING`, `PAID`, `SHIPPED`, etc.) |
| `total_price`         | `bigint`    | Total amount after item prices, discounts, and shipping   |
| `discount_total`      | `bigint`    | Total discount applied to the order                       |
| `shipping_cost`       | `bigint`    | Shipping fee for the order                                |
| `coupon_id`           | `bigint`    | Optional coupon reference (from Coupon Service)           |
| `created_at`          | `timestamp` | Timestamp when order was created                          |
| `updated_at`          | `timestamp` | Timestamp of last order update                            |

---

### `order_items`

Stores individual items included in each order, with price breakdown and discount per item.

| Column             | Type      | Description                                                    |
| ------------------ | --------- | -------------------------------------------------------------- |
| `id`               | `uuid`    | Primary key (unique identifier for item record)                |
| `order_id`         | `uuid`    | Foreign key referencing `orders.id`                            |
| `product_id`       | `uuid`    | Product identifier (from Product Service)                      |
| `product_name`     | `text`    | Name of the product at the time of order                       |
| `unit_price`       | `decimal` | Price per unit before discount                                 |
| `quantity`         | `int`     | Number of units ordered                                        |
| `discount_applied` | `decimal` | Discount amount applied to this line item                      |
| `final_price`      | `decimal` | Total price = (`unit_price` × `quantity`) - `discount_applied` |



# order
# order
