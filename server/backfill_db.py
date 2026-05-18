import sqlite3
import json

db_path = "/Users/adrianopassos/gut-trace/server/data/guttrace.db"
conn = sqlite3.connect(db_path)
cursor = conn.cursor()

cursor.execute("SELECT id, payload FROM events WHERE type='symptom'")
rows = cursor.fetchall()

updated = 0
for row in rows:
    id_ = row[0]
    payload = json.loads(row[1])
    if "belly_ache_score" not in payload:
        payload["belly_ache_score"] = 0
        cursor.execute("UPDATE events SET payload=? WHERE id=?", (json.dumps(payload), id_))
        updated += 1

conn.commit()
conn.close()
print(f"Updated {updated} records.")
