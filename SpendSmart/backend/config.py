import os

class Config:
    # PostgreSQL connection — a single DATABASE_URL is the standard
    # provided by free hosts (Render, Railway, Neon, etc.)
    # Local example: postgresql://postgres:password@localhost:5432/spendsmart
    DATABASE_URL = os.environ.get(
        'DATABASE_URL',
        'postgresql://postgres:postgres@localhost:5432/spendsmart'
    )

    # Secret key for JWT signing — change this in production
    SECRET_KEY = os.environ.get('SECRET_KEY', 'spendsmart_secret_key_change_me')
    JWT_EXPIRY_HOURS = 24
