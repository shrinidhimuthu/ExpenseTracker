## 1. Problem Statement
Managing personal daily finances requires real-time tracking of income, expenditure, and daily spending caps. This project provides a robust, enterprise-ready web application that helps users monitor their monthly budget, automatically calculates safe daily allowances based on remaining days in the month, and persists transactions reliably in Indian Rupees (₹).

## 2. Layered Architecture
The application follows a strict n-tier enterprise architecture:
1. **Frontend Layer (`static/index.html`):** Built with Tailwind CSS and asynchronous Fetch API.
2. **Controller Layer (`controller/`):** Exposes REST endpoints (`ExpenseController`) using Spring MVC and legacy Jakarta Servlets.
3. **Service Layer (`service/`):** Contains core business logic and transaction management.
4. **Repository Layer (`repository/`):** Uses Spring Data JPA (`ExpenseRepository`) for ORM-based database operations.
5. **Data Storage Layer:** Lightweight SQLite relational database (`expenses.db`).

## 3. Entity Relationship / Data Model (`expenses` Table)
- `id` (Long, Primary Key, Auto-increment)
- `title` (String, Expense description)
- `amount` (double, Transaction value in ₹)
- `category` (String, Classification type)
- `date` (String, Transaction date)