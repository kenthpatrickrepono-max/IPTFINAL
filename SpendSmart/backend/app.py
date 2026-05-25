import os
from flask import Flask, request, jsonify
from flask_cors import CORS
import bcrypt
import jwt
import datetime
from functools import wraps
from config import Config
import db  # unified DB abstraction (postgres or sqlite fallback)

app = Flask(__name__)
app.config.from_object(Config)

CORS(app)


# ─────────────────────────────────────────────
# JWT helper decorator
# ─────────────────────────────────────────────

def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = None
        auth_header = request.headers.get('Authorization', '')
        if auth_header.startswith('Bearer '):
            token = auth_header.split(' ')[1]

        if not token:
            return jsonify({'error': 'Token is missing'}), 401

        try:
            data = jwt.decode(token, Config.SECRET_KEY, algorithms=['HS256'])
            current_user_id = data['user_id']
        except jwt.ExpiredSignatureError:
            return jsonify({'error': 'Token has expired'}), 401
        except jwt.InvalidTokenError:
            return jsonify({'error': 'Invalid token'}), 401

        return f(current_user_id, *args, **kwargs)
    return decorated


# ─────────────────────────────────────────────
# Auth routes
# ─────────────────────────────────────────────

@app.route('/api/register', methods=['POST'])
def register():
    data = request.get_json()
    username = data.get('username', '').strip()
    email = data.get('email', '').strip()
    password = data.get('password', '')

    if not username or not email or not password:
        return jsonify({'error': 'All fields are required'}), 400

    if len(password) < 6:
        return jsonify({'error': 'Password must be at least 6 characters'}), 400

    password_hash = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')

    conn = db.get_conn()
    try:
        cur = conn.cursor()
        user_id = db.insert_returning_id(
            cur,
            'INSERT INTO users (username, email, password_hash) VALUES (%s, %s, %s) RETURNING id',
            (username, email, password_hash),
        )
        conn.commit()
        cur.close()
    except Exception:
        conn.rollback()
        return jsonify({'error': 'Username or email already exists'}), 409
    finally:
        conn.close()

    token = jwt.encode({
        'user_id': user_id,
        'exp': datetime.datetime.utcnow() + datetime.timedelta(hours=Config.JWT_EXPIRY_HOURS)
    }, Config.SECRET_KEY, algorithm='HS256')

    return jsonify({'message': 'Registered successfully', 'token': token, 'username': username}), 201


@app.route('/api/login', methods=['POST'])
def login():
    data = request.get_json()
    email = data.get('email', '').strip()
    password = data.get('password', '')

    if not email or not password:
        return jsonify({'error': 'Email and password are required'}), 400

    conn = db.get_conn()
    cur = conn.cursor()
    cur.execute(db.sql('SELECT * FROM users WHERE email = %s'), (email,))
    user = cur.fetchone()
    cur.close()
    conn.close()

    if not user or not bcrypt.checkpw(password.encode('utf-8'), user['password_hash'].encode('utf-8')):
        return jsonify({'error': 'Invalid email or password'}), 401

    token = jwt.encode({
        'user_id': user['id'],
        'exp': datetime.datetime.utcnow() + datetime.timedelta(hours=Config.JWT_EXPIRY_HOURS)
    }, Config.SECRET_KEY, algorithm='HS256')

    return jsonify({'message': 'Login successful', 'token': token, 'username': user['username']}), 200


# ─────────────────────────────────────────────
# Categories route
# ─────────────────────────────────────────────

@app.route('/api/categories', methods=['GET'])
@token_required
def get_categories(current_user_id):
    conn = db.get_conn()
    cur = conn.cursor()
    cur.execute(db.sql('SELECT * FROM categories ORDER BY name'))
    rows = cur.fetchall()
    cur.close()
    conn.close()
    # Normalise to plain dicts so jsonify handles sqlite3.Row uniformly.
    categories = [dict(r) for r in rows]
    return jsonify({'categories': categories}), 200


# ─────────────────────────────────────────────
# Expenses routes
# ─────────────────────────────────────────────

@app.route('/api/expenses', methods=['GET'])
@token_required
def get_expenses(current_user_id):
    conn = db.get_conn()
    cur = conn.cursor()
    cur.execute(db.sql('''
        SELECT e.id, e.amount, e.description, e.expense_date,
               c.name AS category, e.created_at,
               SUM(e.amount) OVER () AS total
        FROM expenses e
        JOIN categories c ON e.category_id = c.id
        WHERE e.user_id = %s
        ORDER BY e.expense_date DESC, e.created_at DESC
    '''), (current_user_id,))
    rows = cur.fetchall()
    cur.close()
    conn.close()

    total = 0.0
    expenses = []
    for row in rows:
        row = dict(row)
        total = float(row.pop('total'))
        row['expense_date'] = str(row['expense_date'])
        row['created_at'] = str(row['created_at'])
        row['amount'] = float(row['amount'])
        expenses.append(row)

    return jsonify({'expenses': expenses, 'total': total}), 200


@app.route('/api/expenses', methods=['POST'])
@token_required
def add_expense(current_user_id):
    data = request.get_json()
    amount = data.get('amount')
    category_id = data.get('category_id')
    description = data.get('description', '')
    expense_date = data.get('expense_date')

    if not amount or not category_id or not expense_date:
        return jsonify({'error': 'amount, category_id and expense_date are required'}), 400

    try:
        amount = float(amount)
        if amount <= 0:
            raise ValueError()
    except ValueError:
        return jsonify({'error': 'amount must be a positive number'}), 400

    conn = db.get_conn()
    cur = conn.cursor()
    expense_id = db.insert_returning_id(
        cur,
        'INSERT INTO expenses (user_id, category_id, amount, description, expense_date) '
        'VALUES (%s, %s, %s, %s, %s) RETURNING id',
        (current_user_id, category_id, amount, description, expense_date),
    )
    conn.commit()
    cur.close()
    conn.close()

    return jsonify({'message': 'Expense added', 'id': expense_id}), 201


@app.route('/api/expenses/<int:expense_id>', methods=['DELETE'])
@token_required
def delete_expense(current_user_id, expense_id):
    conn = db.get_conn()
    cur = conn.cursor()
    cur.execute(db.sql('SELECT id FROM expenses WHERE id = %s AND user_id = %s'),
                (expense_id, current_user_id))
    expense = cur.fetchone()

    if not expense:
        cur.close()
        conn.close()
        return jsonify({'error': 'Expense not found'}), 404

    cur.execute(db.sql('DELETE FROM expenses WHERE id = %s'), (expense_id,))
    conn.commit()
    cur.close()
    conn.close()

    return jsonify({'message': 'Expense deleted'}), 200


@app.route('/api/expenses/<int:expense_id>', methods=['PUT'])
@token_required
def update_expense(current_user_id, expense_id):
    data = request.get_json()
    conn = db.get_conn()
    cur = conn.cursor()
    cur.execute(db.sql('SELECT id FROM expenses WHERE id = %s AND user_id = %s'),
                (expense_id, current_user_id))
    expense = cur.fetchone()

    if not expense:
        cur.close()
        conn.close()
        return jsonify({'error': 'Expense not found'}), 404

    amount = data.get('amount')
    category_id = data.get('category_id')
    description = data.get('description', '')
    expense_date = data.get('expense_date')

    if not amount or not category_id or not expense_date:
        cur.close()
        conn.close()
        return jsonify({'error': 'amount, category_id and expense_date are required'}), 400

    try:
        amount = float(amount)
        if amount <= 0:
            raise ValueError()
    except (ValueError, TypeError):
        cur.close()
        conn.close()
        return jsonify({'error': 'amount must be a positive number'}), 400

    cur.execute(db.sql('''
        UPDATE expenses
        SET amount = %s, category_id = %s, description = %s, expense_date = %s
        WHERE id = %s AND user_id = %s
    '''), (amount, category_id, description, expense_date, expense_id, current_user_id))
    conn.commit()
    cur.close()
    conn.close()

    return jsonify({'message': 'Expense updated'}), 200


# ─────────────────────────────────────────────
# Health check
# ─────────────────────────────────────────────

@app.route('/api/health', methods=['GET'])
def health():
    return jsonify({'status': 'ok', 'backend': db.BACKEND}), 200


# ─────────────────────────────────────────────
# Schema bootstrap (runs once at startup)
# ─────────────────────────────────────────────

db.bootstrap()


if __name__ == '__main__':
    debug_mode = os.environ.get('FLASK_DEBUG', 'false').lower() == 'true'
    # Hosts inject a $PORT env var; fall back to 5000 for local dev.
    port = int(os.environ.get('PORT', 5000))
    app.run(debug=debug_mode, host='0.0.0.0', port=port)
