# Java E-Commerce Console Application

A simple Java + MySQL console-based e-commerce project that demonstrates:

- User login and registration
- Product listing and stock management
- Cart handling (add/view/update quantities)
- Order placement with transaction support
- Out-of-stock detection and retry flow
- User order history

This repository is a good beginner-to-intermediate backend practice project for learning Java OOP, JDBC, and basic transactional workflows.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Core Flow](#core-flow)
5. [Database Setup (MySQL)](#database-setup-mysql)
6. [Configuration](#configuration)
7. [How to Run](#how-to-run)
8. [Sample Output Flow](#sample-output-flow)
9. [Push This Project to GitHub](#push-this-project-to-github)
10. [Future Improvements](#future-improvements)

---

## Project Overview

The application starts from `Main.java` and simulates a real order process:

1. Tries user login
2. Registers user if not found
3. Loads products from DB
4. Adds products into cart (including a deliberate over-stock case)
5. Attempts to place an order
6. If out of stock, adjusts quantities and retries safely
7. Shows order details, history, cart state, and updated stock

This demonstrates business logic, edge-case handling, and atomic DB updates.

---

## Tech Stack

- Java (console application)
- JDBC (database access)
- MySQL
- VS Code (or any Java IDE)

---

## Project Structure

```text
JavaProject/
├── src/
│   ├── Main.java                  # Main execution flow
│   ├── App.java                   # Basic hello-world entry class
│   ├── model/
│   │   ├── User.java
│   │   ├── Product.java
│   │   ├── Cart.java
│   │   ├── CartItem.java
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   ├── OrderResponse.java
│   │   ├── OrderStatus.java
│   │   └── StockIssue.java
│   ├── service/
│   │   ├── UserService.java
│   │   ├── ProductService.java
│   │   ├── CartService.java
│   │   └── OrderService.java
│   └── util/
│       └── DBConnection.java
├── bin/                           # Compiled classes
├── lib/                           # External jars (if any)
└── README.md
```

---

## Core Flow

### UserService
- `register(name, email, password)` inserts into `users`
- `login(email, password)` validates credentials

### ProductService
- `getAllProducts()` fetches product list
- `getProduct(id)` fetches one product
- `updateStock(productId, newStock)` updates inventory

### CartService
- `addToCart(cart, productId, quantity)` adds/increments item quantity
- `viewCart(cart)` prints cart items

### OrderService
- Validates cart and stock
- Uses transaction (`setAutoCommit(false)`) for order + item inserts + stock update
- Rolls back if something fails
- Returns `OrderResponse` with status:
  - `SUCCESS`
  - `CART_EMPTY`
  - `OUT_OF_STOCK`
  - `ERROR`

---

## Database Setup (MySQL)

Create the database and tables before running:

```sql
CREATE DATABASE IF NOT EXISTS ecommerce;
USE ecommerce;

CREATE TABLE IF NOT EXISTS users (
	id INT PRIMARY KEY AUTO_INCREMENT,
	name VARCHAR(100) NOT NULL,
	email VARCHAR(150) NOT NULL UNIQUE,
	password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
	id INT PRIMARY KEY,
	name VARCHAR(150) NOT NULL,
	price DOUBLE NOT NULL,
	stock INT NOT NULL,
	description TEXT
);

CREATE TABLE IF NOT EXISTS orders (
	id INT PRIMARY KEY AUTO_INCREMENT,
	user_id INT NOT NULL,
	total DOUBLE NOT NULL,
	status VARCHAR(30) NOT NULL,
	FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS order_items (
	id INT PRIMARY KEY AUTO_INCREMENT,
	order_id INT NOT NULL,
	product_id INT NOT NULL,
	quantity INT NOT NULL,
	price DOUBLE NOT NULL,
	FOREIGN KEY (order_id) REFERENCES orders(id),
	FOREIGN KEY (product_id) REFERENCES products(id)
);
```

Add some seed products:

```sql
INSERT INTO products (id, name, price, stock, description) VALUES
(101, 'Laptop', 75000, 5, 'Performance laptop'),
(102, 'Headphones', 2500, 10, 'Wireless headphones'),
(103, 'Keyboard', 1500, 15, 'Mechanical keyboard')
ON DUPLICATE KEY UPDATE
name = VALUES(name),
price = VALUES(price),
stock = VALUES(stock),
description = VALUES(description);
```

---

## Configuration

Update DB credentials in:

- `src/util/DBConnection.java`

Current default values:

```java
private static final String URL = "jdbc:mysql://localhost:3306/ecommerce";
private static final String USER = "root";
private static final String PASSWORD = "1234";
```

Change these according to your local MySQL setup.

---

## How to Run

### Option 1: Run in VS Code

1. Open project folder in VS Code
2. Ensure Java extension pack is installed
3. Configure MySQL and DB credentials
4. Run `Main.java`

### Option 2: Compile and Run from Terminal (PowerShell)

From project root:

```powershell
javac -d bin src\model\*.java src\service\*.java src\util\*.java src\Main.java
java -cp bin Main
```

If needed, include the MySQL JDBC driver JAR in classpath.

---

## Sample Output Flow

You should see logs similar to:

- User login/register result
- Available products
- Cart contents
- Out-of-stock warning for excessive quantity
- Retry with adjusted quantity
- Order placed successfully
- Order history
- Updated product stock

---

## Future Improvements

- Add password hashing (BCrypt)
- Add input-driven CLI menu instead of fixed flow in `Main.java`
- Add validation for email/quantity/price
- Add unit and integration tests
- Add Maven/Gradle for dependency management
- Add REST API layer (Spring Boot)

---

## Author

Maintained by Amit.

If you use this project for learning, feel free to fork and improve it.
