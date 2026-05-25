"""Database abstraction supporting both PostgreSQL (Aiven) and SQLite fallback.

The active backend is selected once at import time based on whether a
DATABASE_URL is set AND reachable. The rest of the app uses get_conn() and
sql() without caring which engine is active.

- get_conn(): returns a connection whose cursors yield dict-like rows.
- sql(query): rewrites '%s' placeholders to '?' when running on SQLite.
- bootstrap(): runs the right schema for the active backend.
- BACKEND: 'postgres' or 'sqlite' (string, useful for engine-specific tweaks).
"""

import os
import sqlite3
from config import Config

BACKEND = 'sqlite'           # set in _select_backend()
_PG_URL = None               # postgres URL with sslmode, set if BACKEND == 'postgres'
_SQLITE_PATH = os.path.join(os.path.dirname(__file__), 'spendsmart_local.db')


# ─────────────────────────────────────────────
# Postgres URL helper (kept here so app.py doesn't need to know)
# ─────────────────────────────────────────────

def _pg_url_with_ssl(url):
    if 'sslmode=' in url:
        return url
    if 'localhost' in url or '127.0.0.1' in url:
        return url
    separator = '&' if '?' in url else '?'
    return f'{url}{separator}sslmode=require'


# ─────────────────────────────────────────────
# Backend selection (called once at import)
# ─────────────────────────────────────────────

def _select_backend():
    """Decide postgres vs sqlite. Probes postgres with a short timeout."""
    global BACKEND, _PG_URL

    url = os.environ.get('DATABASE_URL')
    # Treat the bundled default localhost URL as "no real DB" — that's only
    # there so config import doesn't crash; it's almost never reachable in dev.
    if not url or 'localhost' in url or '127.0.0.1' in url:
        BACKEND = 'sqlite'
        print(f'[db] using sqlite fallback at {_SQLITE_PATH}')
        return

    candidate = _pg_url_with_ssl(url)
    try:
        import psycopg2
        # Short connect_timeout so offline startup doesn't hang.
        test_conn = psycopg2.connect(candidate, connect_timeout=5)
        test_conn.close()
        _PG_URL = candidate
        BACKEND = 'postgres'
        print('[db] using postgres (aiven)')
    except Exception as e:
        BACKEND = 'sqlite'
        print(f'[db] postgres unreachable ({e.__class__.__name__}); using sqlite fallback at {_SQLITE_PATH}')


_select_backend()


# ─────────────────────────────────────────────
# Connection / placeholder helpers
# ─────────────────────────────────────────────

def get_conn():
    """Return a connection with dict-row cursors."""
    if BACKEND == 'postgres':
        import psycopg2
        from psycopg2.extras import RealDictCursor
        return psycopg2.connect(_PG_URL, cursor_factory=RealDictCursor)

    conn = sqlite3.connect(_SQLITE_PATH)
    conn.row_factory = sqlite3.Row
    # Enforce FK constraints (off by default in SQLite).
    conn.execute('PRAGMA foreign_keys = ON')
    return conn


def sql(query):
    """Rewrite '%s' placeholders to '?' for SQLite. No-op for postgres."""
    if BACKEND == 'sqlite':
        return query.replace('%s', '?')
    return query


def insert_returning_id(cur, query, params):
    """Run an INSERT and return the new row's id, portably.

    Postgres needs `RETURNING id` in the query string; SQLite uses
    cursor.lastrowid. Caller passes the postgres-flavoured query (with
    RETURNING id at the end). On SQLite we strip the RETURNING clause.
    """
    if BACKEND == 'sqlite':
        # Strip a trailing "RETURNING id" (case-insensitive, optional whitespace)
        import re
        q = re.sub(r'\s+RETURNING\s+id\s*$', '', query, flags=re.IGNORECASE)
        cur.execute(sql(q), params)
        return cur.lastrowid

    cur.execute(query, params)
    return cur.fetchone()['id']


# ─────────────────────────────────────────────
# Schema bootstrap
# ─────────────────────────────────────────────

def bootstrap():
    """Apply the schema for the active backend. Idempotent."""
    here = os.path.dirname(__file__)
    schema_file = 'schema_sqlite.sql' if BACKEND == 'sqlite' else 'schema.sql'
    path = os.path.join(here, schema_file)
    if not os.path.exists(path):
        print(f'[db] schema file missing: {schema_file}')
        return

    try:
        conn = get_conn()
        with open(path, 'r', encoding='utf-8') as f:
            script = f.read()

        if BACKEND == 'sqlite':
            # sqlite3 supports executing multi-statement scripts via executescript.
            conn.executescript(script)
        else:
            cur = conn.cursor()
            cur.execute(script)
            cur.close()

        conn.commit()
        conn.close()
        print(f'[db] schema applied ({BACKEND})')
    except Exception as e:
        print(f'[db] bootstrap skipped: {e}')
