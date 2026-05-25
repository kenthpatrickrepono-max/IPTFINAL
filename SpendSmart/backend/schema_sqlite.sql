-- SpendSmart Database Schema (SQLite fallback)
-- Mirrors schema.sql but uses SQLite-compatible types.
-- Idempotent: CREATE TABLE IF NOT EXISTS + INSERT OR IGNORE on seed data.

BEGIN;

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

INSERT OR IGNORE INTO categories (name) VALUES ('Food');
INSERT OR IGNORE INTO categories (name) VALUES ('Transport');
INSERT OR IGNORE INTO categories (name) VALUES ('Shopping');
INSERT OR IGNORE INTO categories (name) VALUES ('Entertainment');
INSERT OR IGNORE INTO categories (name) VALUES ('Health');
INSERT OR IGNORE INTO categories (name) VALUES ('Bills');
INSERT OR IGNORE INTO categories (name) VALUES ('Other');

CREATE TABLE IF NOT EXISTS expenses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id INTEGER NOT NULL REFERENCES categories(id),
    amount REAL NOT NULL,
    description TEXT,
    expense_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS expenses_user_id_idx ON expenses(user_id);
CREATE INDEX IF NOT EXISTS expenses_category_id_idx ON expenses(category_id);
CREATE INDEX IF NOT EXISTS expenses_expense_date_idx ON expenses(expense_date);

COMMIT;
