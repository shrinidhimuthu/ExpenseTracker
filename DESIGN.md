# System Design & Architectural Documentation

## 1. Problem Statement
Managing personal daily finances requires real-time tracking of income, expenditure, and daily spending caps. Traditional spreadsheet templates or standalone desktop calculators often lack seamless persistence, responsive visual feedback, and clear visibility into daily allowable limits.

This project solves this challenge by delivering an enterprise-ready, web-based **Personal Expense & Daily Budget Tracker**. Configured specifically for Indian Rupees (₹), the system maintains real-time tracking of total expenditures, calculates remaining budget allocations, determines remaining days in the active billing month, and dynamically computes a safe daily allowance.

---

## 2. System Architecture

The application adopts an **N-Tier Enterprise Architecture** built on the Spring Boot ecosystem, maintaining a strict separation of concerns across presentation, business, and persistence layers.