import sqlite3
import json
import os

DB_PATH = "data/guttrace.db"
PHOTOS_DIR = "data/photos"

def init_db():
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    os.makedirs(PHOTOS_DIR, exist_ok=True)
    
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS events (
            id TEXT PRIMARY KEY,
            type TEXT NOT NULL,
            payload TEXT NOT NULL,
            created_at TEXT NOT NULL,
            sync_status TEXT DEFAULT 'synced'
        )
    ''')
    conn.commit()
    conn.close()

def save_events(events):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    for event in events:
        event_id = event.get('id')
        event_type = event.get('type', 'unknown')
        created_at = event.get('created_at_utc', '')
        payload = json.dumps(event)
        
        cursor.execute('''
            INSERT OR REPLACE INTO events (id, type, payload, created_at)
            VALUES (?, ?, ?, ?)
        ''', (event_id, event_type, payload, created_at))
        
    conn.commit()
    conn.close()

def get_all_events():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('SELECT payload FROM events ORDER BY created_at DESC')
    rows = cursor.fetchall()
    conn.close()
    return [json.loads(row[0]) for row in rows]
