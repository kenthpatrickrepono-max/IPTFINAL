-- SpendSmart Database Schema (PostgreSQL)
-- Run this file against your PostgreSQL database to set up the tables.
-- The database itself is created by your host (Render/Neon/Aiven) --
-- there is no CREATE DATABASE here, just connect and run this script.
--
-- This script is IDEMPOTENT and NON-DESTRUCTIVE: every statement uses
-- CREATE ... IF NOT EXISTS / ON CONFLICT DO NOTHING so it is safe to run
-- on every cold start without wiping existing user data.

BEGIN;

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_username_key UNIQUE (username),
    CONSTRAINT users_email_key UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT categories_name_key UNIQUE (name)
);

INSERT INTO categories (name) VALUES
    ('Food'),
    ('Transport'),
    ('Shopping'),
    ('Entertainment'),
    ('Health'),
    ('Bills'),
    ('Other')
ON CONFLICT ON CONSTRAINT categories_name_key DO NOTHING;

CREATE TABLE IF NOT EXISTS expenses (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id INTEGER NOT NULL REFERENCES categories(id),
    amount DECIMAL(10, 2) NOT NULL,
    description VARCHAR(255),
    expense_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS expenses_user_id_idx ON expenses(user_id);
CREATE INDEX IF NOT EXISTS expenses_category_id_idx ON expenses(category_id);
CREATE INDEX IF NOT EXISTS expenses_expense_date_idx ON expenses(expense_date);

COMMIT;
