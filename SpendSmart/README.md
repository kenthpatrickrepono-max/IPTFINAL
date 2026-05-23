# SpendSmart – Personal Expense Tracker

A mobile application built with **Android (Java)**, **Flask REST API**, and **MySQL** that lets users register, log in, and track their personal expenses by category.

---

## Tech Stack

| Layer    | Technology                  |
|----------|-----------------------------|
| Mobile   | Android Studio (Java)       |
| Backend  | Python Flask                |
| Database | MySQL                       |
| Auth     | JWT (JSON Web Tokens)       |
| HTTP     | Retrofit 2 + OkHttp         |

---

## Features

- User registration and login with hashed passwords
- JWT-based session authentication
- Add expenses with amount, category, description, and date
- View all expenses with total spending summary
- Delete expenses
- 7 default categories: Food, Transport, Shopping, Entertainment, Health, Bills, Other

---

## Project Structure

```
SpendSmart/
├── backend/
│   ├── app.py            # Flask REST API
│   ├── config.py         # Database & JWT config
│   ├── schema.sql        # MySQL database schema
│   └── requirements.txt  # Python dependencies
├── android/
│   └── app/src/main/
│       ├── java/com/example/spendsmart/
│       │   ├── LoginActivity.java
│       │   ├── RegisterActivity.java
│       │   ├── DashboardActivity.java
│       │   ├── AddExpenseActivity.java
│       │   ├── ExpenseAdapter.java
│       │   ├── SessionManager.java
│       │   ├── RetrofitClient.java
│       │   ├── ApiService.java
│       │   └── (model classes)
│       └── res/layout/
│           ├── activity_login.xml
│           ├── activity_register.xml
│           ├── activity_dashboard.xml
│           ├── activity_add_expense.xml
│           └── item_expense.xml
└── README.md
```

---

## Setup Instructions

### 1. MySQL Database

1. Open MySQL Workbench or your MySQL client
2. Run the schema file:
   ```sql
   source path/to/SpendSmart/backend/schema.sql;
   ```

### 2. Flask Backend

1. Make sure you have Python 3.8+ installed
2. Navigate to the backend folder:
   ```bash
   cd SpendSmart/backend
   ```
3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
4. Edit `config.py` and set your MySQL credentials:
   ```python
   MYSQL_USER = 'your_mysql_username'
   MYSQL_PASSWORD = 'your_mysql_password'
   ```
5. Run the server:
   ```bash
   python app.py
   ```
   The API will be running at `http://localhost:5000`

### 3. Android App

1. Open the `android/` folder in **Android Studio**
2. If testing on an **emulator**, the base URL in `RetrofitClient.java` is already set to `http://10.0.2.2:5000/` (this maps to localhost)
3. If testing on a **real device**, replace `10.0.2.2` with your computer's local IP address (e.g. `http://192.168.1.5:5000/`)
4. Build and run the app

---

## API Endpoints

| Method | Endpoint                  | Auth Required | Description          |
|--------|---------------------------|---------------|----------------------|
| POST   | `/api/register`           | No            | Register a new user  |
| POST   | `/api/login`              | No            | Login and get token  |
| GET    | `/api/categories`         | Yes           | List all categories  |
| GET    | `/api/expenses`           | Yes           | Get user's expenses  |
| POST   | `/api/expenses`           | Yes           | Add a new expense    |
| DELETE | `/api/expenses/<id>`      | Yes           | Delete an expense    |
| PUT    | `/api/expenses/<id>`      | Yes           | Update an expense    |

Authentication uses `Authorization: Bearer <token>` headers.

---

## GitHub Commit Strategy

To meet the **3 meaningful commits per member** requirement, split work as follows:

**Member 1 – Backend & Database**
1. `Add MySQL schema and database setup`
2. `Add Flask auth routes (register, login)`
3. `Add Flask expenses CRUD endpoints`

**Member 2 – Android UI**
1. `Add Login and Register screens`
2. `Add Dashboard with expense list and RecyclerView`
3. `Add AddExpense screen with date picker and category spinner`

**Member 3 – Integration & Polish**
1. `Add Retrofit client and API service interface`
2. `Add SessionManager for JWT token storage`
3. `Fix error handling and UI polish across all screens`

---

## Group Members

| Name | Role | Commits |
|------|------|---------|
|      |      |         |
|      |      |         |
|      |      |         |

---

## License

This project was developed as a final project for Mobile App Development.
