# API Documentation & Endpoint Specifications

Base URL for all API endpoints: `http://localhost:8080`

---

## 1. Expense Management REST APIs (`ExpenseController`)

### **Get All Expenses**
Retrieves the complete transaction history stored in the SQLite database.

- **URL:** `/api/expenses`
- **Method:** `GET`
- **Request Body:** *None*
- **Response Status:** `200 OK`
- **Response Body (JSON Example):**
  ```json
  [
    {
      "id": 1,
      "title": "Groceries",
      "amount": 750.00,
      "category": "Food & Groceries",
      "date": "2026-10-20"
    },
    {
      "id": 2,
      "title": "Electricity Bill",
      "amount": 1400.50,
      "category": "Utilities & Bills",
      "date": "2026-10-21"
    }
  ]