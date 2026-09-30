# Order Management System

This is a REST API for the management of customers, products, and orders. The system
uses Spring Boot and JPA. The system has a clean design with separate layers.

## 1. What the application does

- **Customers** — You can add a customer. You can update a customer. You can get a
  customer by ID or in a list. Each email address is unique. Each phone number is
  unique.
- **Products** — You can add a product. You can update a product. You can get a product
  by ID or in a list. The price is a `BigDecimal` value. The stock value is an integer
  that is not less than zero.
- **Orders** — You can create an order for one or more products with quantities. The
  system checks the stock before it accepts the order. The system removes the stock in
  one atomic step. The system calculates the order total from the product prices at the
  time of the order.
- **Reports** — The system shows the total orders for each customer. The system shows
  the top 5 customers by order count. The database does the calculation for both
  reports.

## 2. Architecture

```
Client
  |
  v
REST Controller   (binds the HTTP request, checks the input, sets the status code)
  |
  v
Service Layer     (has the business rules and the transaction limits)
  |
  v
Repository Layer  (Spring Data JPA, has the database queries)
  |
  v
Database          (H2, Flyway manages the schema, the database checks the rules)
```

DTOs are the only objects that cross the controller boundary. The system does not send
JPA entities directly as JSON. This method stops lazy-loading errors and stops
recursion errors. This method also keeps the API contract separate from the persistence
model.

## 3. Project structure

```
src/main/java/com/example/ordermanagement
├── OrderManagementSystemApplication.java
├── config          # sets up OpenAPI and Swagger
├── controller       # REST controllers (Customer, Product, Order, Report)
├── service           # has the business logic and the transaction limits
├── repository       # has the Spring Data JPA repositories and the report queries
├── entity            # JPA entities (Customer, Product, Order, OrderItem)
├── dto
│   ├── request         # has the request objects; Jakarta Bean Validation checks them
│   └── response        # has the response objects
├── mapper            # converts data between entities and DTOs, with plain code
└── exception         # has the custom exceptions and the central error handler

src/main/resources
├── application.yml
└── db/migration/V1__init_schema.sql   # has the Flyway schema: tables, rules, indexes

src/test/java/com/example/ordermanagement
├── service              # has unit tests for the business logic; uses Mockito
├── controller           # has full integration tests; uses MockMvc, H2, and Flyway
├── concurrency          # has integration tests for concurrent stock use
└── support               # has the base class for the integration tests
```

## 4. Database schema / domain model

```
Customer                    Product
--------                    -------
id (PK)                     id (PK)
name        NOT NULL        name        NOT NULL
email       UNIQUE, NOT NULL price       NOT NULL, DECIMAL(19,2), CHECK (price >= 0)
phone       UNIQUE, NOT NULL stock       NOT NULL, CHECK (stock >= 0)
created_at, updated_at      created_at, updated_at

Order                        OrderItem
-----                        ---------
id (PK)                      id (PK)
customer_id (FK -> Customer) order_id   (FK -> Order)
total_amount  DECIMAL(19,2)  product_id (FK -> Product)
created_at                   quantity    NOT NULL, CHECK (quantity > 0)
                              unit_price  DECIMAL(19,2) NOT NULL
                              subtotal    DECIMAL(19,2) NOT NULL
```

The order items are a separate entity and table, called `OrderItem`. The system does
not put the product and quantity pairs inside the `Order` table. This design is the
normal shape for a "one order has many items" relationship. This design also makes it
possible to keep the price history for each item.

**Why the system stores `unit_price` on `OrderItem`:** the system records the price at
the time of the order. If a product price changes later, old orders keep their original
total. An old order must not change its value without notice.

The database enforces uniqueness for `customers.email` and `customers.phone`. The
database also enforces non-negative values for `products.price`, `products.stock`, and
`order_items.quantity`. The file `V1__init_schema.sql` sets these rules. Bean Validation
annotations also check these rules in the application. Two requests at the same time can
bypass an application check. Two requests at the same time cannot bypass a database
rule.

**Indexes:** the database has indexes on `orders.customer_id`, `orders.created_at`,
`order_items.order_id`, and `order_items.product_id`. The unique rules on
`customers.email` and `customers.phone` also create indexes.

## 5. API endpoints

### Customers
| Method | Path                | Description          |
|--------|----------------------|-----------------------|
| POST   | `/api/customers`      | Create a customer     |
| GET    | `/api/customers/{id}` | Get a customer        |
| GET    | `/api/customers`      | Get the list of customers |
| PUT    | `/api/customers/{id}` | Update a customer     |

### Products
| Method | Path               | Description         |
|--------|---------------------|-----------------------|
| POST   | `/api/products`      | Create a product     |
| GET    | `/api/products/{id}` | Get a product        |
| GET    | `/api/products`      | Get the list of products |
| PUT    | `/api/products/{id}` | Update a product     |

### Orders
| Method | Path                          | Description                  |
|--------|--------------------------------|--------------------------------|
| POST   | `/api/orders`                   | Create a new order            |
| GET    | `/api/orders/customer/{customerId}` | Get the orders for a customer |

### Reports
| Method | Path                                | Description                          |
|--------|--------------------------------------|-----------------------------------------|
| GET    | `/api/reports/orders-per-customer`   | Get the total orders for each customer |
| GET    | `/api/reports/top-customers`         | Get the top 5 customers by order count |

You can view the interactive API docs at `/swagger-ui.html` when the application runs.
You can view the raw OpenAPI file at `/v3/api-docs`.

## 6. Example requests/responses

**Create customer** — `POST /api/customers`
```json
{ "name": "John Doe", "email": "john@example.com", "phone": "9999999999" }
```
```json
{
  "id": 1, "name": "John Doe", "email": "john@example.com",
  "phone": "9999999999", "createdAt": "2026-09-30T07:45:01.391475Z"
}
```

**Create product** — `POST /api/products`
```json
{ "name": "Laptop", "price": 50000.00, "stock": 10 }
```

**Create order** — `POST /api/orders`
```json
{
  "customerId": 1,
  "items": [
    { "productId": 10, "quantity": 2 },
    { "productId": 20, "quantity": 1 }
  ]
}
```
```json
{
  "id": 1001,
  "customer": { "id": 1, "name": "John Doe", "email": "john@example.com", "phone": "9999999999", "createdAt": "..." },
  "items": [
    { "productId": 10, "productName": "Laptop", "quantity": 2, "unitPrice": 50000.00, "subtotal": 100000.00 }
  ],
  "totalAmount": 100000.00,
  "createdAt": "2026-09-30T07:45:01.506297Z"
}
```

**Error response** — every error has the same shape:
```json
{
  "timestamp": "2026-09-30T07:45:01.571241Z",
  "status": 409,
  "error": "INSUFFICIENT_STOCK",
  "message": "Insufficient stock for product 'Laptop' (id=1): requested 999, available 8",
  "path": "/api/orders"
}
```

HTTP status codes:
- **201** — the system created the item.
- **200** — the system read or updated the item.
- **400** — the request failed a check, or the request is not correct.
- **404** — the system could not find the item.
- **409** — the item is a duplicate, or the stock is not enough, or there is a data
  conflict.
- **500** — the system had an error that it did not expect.

## 7. How to run the application

You need Java 17 or a later version. You do not need to install Maven. The Maven
Wrapper is in the repository. The Maven Wrapper downloads Maven the first time you run
it.

```bash
./mvnw spring-boot:run
```

or build and run the jar file:

```bash
./mvnw clean package
java -jar target/order-management-system.jar
```

The application starts at `http://localhost:8080`. Open `http://localhost:8080/swagger-ui.html` in a browser — you can create customers/products, place orders, and hit the report endpoints interactively. The application uses a file-based H2
database at `./data/oms` by default. The data stays after you stop and start the
application again. Delete the `data/` folder to reset the data. You can change the
configuration in `application.yml` with environment variables: `DB_URL`,
`DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, and more. The code does not have hardcoded
secret values.

You can use the H2 web console to view the database:
`http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/oms`, user `sa`,
empty password).

## 8. How to run tests

```bash
./mvnw clean test
```

This command runs 54 tests. Mockito unit tests check the service layer. MockMvc
integration tests check the real HTTP layer, the JPA layer, and the H2 database with
Flyway. Section 9 describes the concurrency tests. Each integration test starts an
isolated in-memory H2 database. Each test clears all tables before it runs. This method
makes the test results the same every time, in any test order.

## 9. Concurrency strategy for stock

**Problem:** two customers can order the same product at the same time. Both orders
must not succeed if the result would take stock below zero. Example: the stock is 5.
One request asks for 4 items. Another request asks for 3 items. Both requests must not
succeed.

**Method: pessimistic row locking.** The system gets this lock inside the
`@Transactional` method for the order:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id ASC")
List<Product> findAllByIdInForUpdate(@Param("ids") List<Long> ids);
```

This query sends a `SELECT ... FOR UPDATE` command for each product in the order. Two
transactions can try to order the same product at the same time. The second transaction
must wait at the `SELECT FOR UPDATE` step. The second transaction waits until the first
transaction commits and releases the lock, or until the first transaction rolls back.
The second transaction then reads the current stock value again. This method stops both
transactions from reading the same old stock value and both succeeding. The database
prevents this error at the row level; the application code does not check this in
memory.

**Deadlock avoidance:** the system locks products in order, from the lowest product ID
to the highest. The system sorts the ID list before it runs the query. Example: order A
wants products 1 and 2. Order B wants products 2 and 1. The system locks the products in
the same order for both requests. This method stops a deadlock.

The design uses pessimistic locking, not optimistic locking with `@Version` and a retry
step. Pessimistic locking gives correct results without a retry step in the application
code. This method fits the size of this system, and it is simpler to understand than a
retry loop. The design does not use plain SQL and code without a lock, because each
request reads the stock value and then writes a new value back. Two threads can read the
stock value at the same time. Without a lock, both threads would then calculate from the
same start value.

**Tests check this design**, in `OrderConcurrencyIntegrationTest`:
- Test 1: the stock is 10. Two orders ask for 7 items at the same time. One order
  succeeds with status 201. The other order fails with status 409, error
  `INSUFFICIENT_STOCK`. The final stock is exactly 3. The stock never goes below zero.
- Test 2: the stock is 20. 20 orders ask for 1 item each, at the same time. All 20
  orders succeed. The final stock is exactly 0. The system does not lose an update. The
  system does not remove too much stock.

## 10. Transactional / atomicity strategy

`OrderService.placeOrder` is one `@Transactional` method. This method finds the
customer, locks the products, checks the stock, removes the stock, and saves the order
and the order items. Any step can fail: the customer is not found, a product is not
found, or the stock is not enough. If a step fails, the method throws an error before
the transaction commits. Spring then rolls back every change from that request. This
rollback also removes stock changes from earlier items in the same order.

A test checks this rule:
`OrderControllerIntegrationTest#placeOrder_secondItemInsufficientStock_rollsBackFirstItemsDeductionToo`.
This test creates an order with two items. The first item would succeed alone. The
second item fails. The test checks that the system rolls back the first item too.

## 11. Important design decisions / assumptions

The assignment does not state some details. This list shows the choices made for these
details:

- **Database:** the system uses H2. The file mode is for development and for manual
  tests. The in-memory mode is for automated tests. The assignment does not name a
  production database. The schema uses Flyway and plain ANSI SQL. You can move the
  schema to PostgreSQL with few or no changes.
- **Schema management:** the system uses Flyway migrations, not Hibernate auto-DDL. The
  setting `ddl-auto: validate` checks the schema; it does not create the schema. This
  method makes the `UNIQUE` and `CHECK` rules clear, in version control, and easy to
  review. The system does not infer these rules only from JPA annotations.
- **Product price rule:** the assignment says the price is a `BigDecimal` value. The
  assignment does not say if zero is a valid price. This system requires a price above
  zero (`@DecimalMin(inclusive = false)`). The system does not support a free product.
- **`GET /api/customers` and `GET /api/products`** return a plain list. These endpoints
  match the API shape in the assignment. The list does not use pages. This system is
  small, so pages are not needed now. You could add pages with `Pageable` if the
  customer list or the product list grows large.
- **`GET /api/orders/customer/{customerId}`** returns status 404 if the customer does
  not exist. The system does not return an empty list for this case. This rule shows
  the difference between "no orders yet" and "no such customer." The other endpoints
  use the same rule for a customer or a product that does not exist.
- **The `orders-per-customer` report** includes customers with zero orders. The report
  uses a `LEFT JOIN` and a `COUNT` function. The phrase "total orders for each customer"
  means every customer. **The `top-customers` report** only ranks a customer with at
  least one order. The report uses an `INNER JOIN`. A customer with zero orders is not a
  "top" customer. When two customers have the same order count, the system sorts by
  customer ID, from low to high.
- **Duplicate products in one order:** the system supports the same product ID in two
  items of one order. The system processes each item with the same locked `Product`
  object. The system adds the quantities together correctly. The stock check stays
  correct for the combined quantity.
- **`Order`/`OrderItem` do not change after creation.** The assignment does not have an
  endpoint to update or cancel an order. The `Order` to `OrderItem` link uses
  `CascadeType.PERSIST` only, not `CascadeType.ALL`. The system does not use
  `orphanRemoval`.
- **The `PUT` method for a product** can change the stock value, the name, and the
  price. The term "update product" means a full update of the resource. The assignment
  does not name a separate endpoint to add more stock.

## 12. Commands used to build and test

```bash
./mvnw clean test           # run the full test suite (unit + integration + concurrency)
./mvnw clean package         # build the jar file (target/order-management-system.jar)
./mvnw spring-boot:run       # run the application on your computer
```

The development process included a manual, complete test of the flow:
1. Create a customer.
2. Create a product.
3. Create an order.
4. Check that the stock went down.
5. Check the order total.
6. Get the orders for the customer.
7. Get the orders-per-customer report.
8. Get the top-5 report.
9. Try a duplicate email; the system must reject it with status 409.
10. Try an order with not enough stock; the system must reject it with status 409.

This test used `curl` against a running instance of the application. The automated test
suite also checks these steps.
