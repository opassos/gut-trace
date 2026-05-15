from fastapi import FastAPI, UploadFile, File, Request, Form
from fastapi.responses import HTMLResponse, StreamingResponse
from fastapi.staticfiles import StaticFiles
import uvicorn
import os
import shutil
import csv
import io
import html
from typing import List, Dict, Any
from datetime import datetime
from collections import defaultdict

from database import init_db, save_events, get_all_events, PHOTOS_DIR

app = FastAPI(title="GutTrace Server")

os.makedirs(PHOTOS_DIR, exist_ok=True)
app.mount("/photos", StaticFiles(directory=PHOTOS_DIR), name="photos")


NAV = """
<nav style="background:#2c3e50;color:white;padding:16px 20px;display:flex;gap:20px;box-shadow:0 2px 4px rgba(0,0,0,.1)">
  <a href="/" style="color:white;text-decoration:none;font-weight:500;font-size:1.1em">Timeline</a>
  <a href="/heatmap" style="color:white;text-decoration:none;font-weight:500;font-size:1.1em">Heatmap</a>
  <a href="/explorer" style="color:white;text-decoration:none;font-weight:500;font-size:1.1em">Explorer</a>
  <a href="/export/csv" style="color:white;text-decoration:none;font-weight:500;font-size:1.1em">Export CSV</a>
</nav>
"""

BASE_STYLE = """
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1.0">
<style>
  body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;margin:0;padding:0;background:#f5f5f5;color:#333}
  .container{max-width:900px;margin:20px auto;padding:0 20px}
  .card{background:white;border-radius:8px;padding:16px;margin-bottom:16px;box-shadow:0 1px 3px rgba(0,0,0,.1)}
  h1{margin-bottom:8px}
  .tag{background:#e0e0e0;padding:4px 8px;border-radius:4px;font-size:.85em;margin-right:4px;display:inline-block}
  .symptom{background:#ffebee;color:#c62828;padding:8px;border-radius:4px;text-align:center;font-weight:bold;display:inline-block;margin:4px}
  .event-time{color:#666;font-size:.9em;margin-bottom:8px}
  img{max-width:100%;border-radius:8px;margin-top:8px}
</style>
"""

def page(title: str, body: str) -> str:
    return f"<!DOCTYPE html><html><head>{BASE_STYLE}<title>{title} | GutTrace</title></head><body>{NAV}<div class='container'>{body}</div></body></html>"


@app.on_event("startup")
def startup_event():
    init_db()


@app.post("/sync/events")
async def sync_events(events: List[Dict[str, Any]]):
    save_events(events)
    return {"status": "success", "count": len(events)}

@app.get("/health")
async def health_check():
    return {"status": "ok"}


@app.post("/sync/photos")
async def sync_photos(file: UploadFile = File(...), photo_id: str = Form(...)):
    file_path = os.path.join(PHOTOS_DIR, f"{photo_id}.jpg")
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
    return {"status": "success", "photo_id": photo_id}


@app.get("/", response_class=HTMLResponse)
async def dashboard():
    events = get_all_events()
    cards = ""
    for event in events:
        t = html.escape(str(event.get("local_datetime", "")))
        etype = event.get("type", "")
        if etype == "meal":
            photos_html = "".join(
                f'<img src="/photos/{pid}.jpg" alt="Meal" onerror="this.style.display=\'none\'">'
                for pid in event.get("photo_ids", [])
            )
            tags_html = "".join(f'<span class="tag">{html.escape(str(tg))}</span>' for tg in event.get("tags", []))
            cards += f'<div class="card"><div class="event-time">{t}</div><h3>🍽️ Meal</h3>{photos_html}<div style="margin-top:8px">{tags_html}</div></div>'
        elif etype == "symptom":
            syms = []
            for label, key in [("Bloating", "upper_bloating_score"), ("Nausea", "nausea_score"), ("Belching", "belching_score"), ("Heartburn", "heartburn_score")]:
                v = event.get(key)
                if v:
                    syms.append(f'<span class="symptom">{label}: {html.escape(str(v))}/5</span>')
            cards += f'<div class="card"><div class="event-time">{t}</div><h3>🤢 Symptoms</h3>{"".join(syms)}</div>'
        elif etype == "medication":
            med = event.get("medication", "Unknown")
            alias = event.get("alias", "")
            disp = f"{med} ({alias})" if alias else med
            cards += f'<div class="card"><div class="event-time">{t}</div><h3>💊 Medication</h3><div><span class="tag" style="background:#e3f2fd;color:#1565c0;font-weight:bold;font-size:1em;padding:6px 10px">{html.escape(disp)}</span></div></div>'
        elif etype == "deleted":
            continue
        elif etype == "bowel":
            scale = event.get("bristol_scale", "?")
            notes = event.get("notes", "")
            notes_html = f'<div style="margin-top:4px;font-size:0.9em;color:#666">{html.escape(notes)}</div>' if notes else ""
            cards += f'<div class="card"><div class="event-time">{t}</div><h3>💩 Bowel Movement</h3><div><span class="tag" style="background:#8d6e63;color:white;font-weight:bold">Bristol Scale: {scale}</span></div>{notes_html}</div>'
        else:
            cards += f'<div class="card"><div class="event-time">{t}</div><h3>{html.escape(etype)}</h3></div>'

    if not cards:
        cards = '<div class="card"><p>No events recorded yet. Sync from the Android app to see data here.</p></div>'

    return HTMLResponse(page("Timeline", f"<h1>Timeline</h1>{cards}"))


@app.get("/heatmap", response_class=HTMLResponse)
async def heatmap():
    events = get_all_events()
    raw: dict[str, list[int]] = {}
    for event in events:
        if event.get("type") == "symptom":
            s = event.get("local_datetime", "")
            if len(s) >= 19:
                try:
                    dt = datetime.fromisoformat(s[:19])
                    day = dt.strftime("%Y-%m-%d")
                    idx = dt.hour // 3
                    scores = [int(event.get(k, 0) or 0) for k in ("upper_bloating_score", "nausea_score", "belching_score", "heartburn_score")]
                    raw.setdefault(day, [0]*8)
                    raw[day][idx] = max(raw[day][idx], max(scores))
                except (ValueError, IndexError):
                    pass

    score_color = ["white", "#fff9c4", "#ffe082", "#ffb74d", "#ef5350", "#b71c1c"]
    score_text_color = ["#333", "#333", "#333", "#333", "white", "white"]

    labels = ["00-03", "03-06", "06-09", "09-12", "12-15", "15-18", "18-21", "21-24"]
    header = "<tr><th>Date</th>" + "".join(f"<th>{l}</th>" for l in labels) + "</tr>"
    rows_html = ""
    for day in sorted(raw.keys(), reverse=True):
        cells = ""
        for score in raw[day]:
            bg = score_color[min(score, 5)]
            fc = score_text_color[min(score, 5)]
            val = str(score) if score > 0 else "-"
            cells += f'<td style="background:{bg};color:{fc};font-weight:{"bold" if score >= 4 else "normal"}">{val}</td>'
        rows_html += f"<tr><td><strong>{html.escape(day)}</strong></td>{cells}</tr>"

    if not rows_html:
        rows_html = '<tr><td colspan="9">No symptom data available yet.</td></tr>'

    table_style = "border-collapse:collapse;width:100%;background:white;box-shadow:0 1px 3px rgba(0,0,0,.1);border-radius:8px;overflow:hidden"
    cell_style = "border:1px solid #ddd;padding:12px;text-align:center;font-size:.95em"

    body = f"""
    <h1>Symptom Heatmap</h1>
    <p>Max symptom score per 3-hour window. Colors: 
      <span style="padding:2px 8px;background:#fff9c4">1-mild</span>
      <span style="padding:2px 8px;background:#ffe082">2</span>
      <span style="padding:2px 8px;background:#ffb74d">3</span>
      <span style="padding:2px 8px;background:#ef5350;color:white">4</span>
      <span style="padding:2px 8px;background:#b71c1c;color:white">5-severe</span>
    </p>
    <div style="overflow-x:auto">
      <table style="{table_style}">
        <style>table td,table th{{border:1px solid #ddd;padding:12px;text-align:center;font-size:.95em}} table th{{background:#f8f9fa;font-weight:600}}</style>
        <thead>{header}</thead>
        <tbody>{rows_html}</tbody>
      </table>
    </div>
    """
    return HTMLResponse(page("Heatmap", body))


@app.get("/explorer", response_class=HTMLResponse)
async def explorer():
    events = get_all_events()
    tags_count: dict[str, int] = defaultdict(int)
    for event in events:
        if event.get("type") == "meal":
            for tag in event.get("tags", []):
                tags_count[str(tag)] += 1

    sorted_tags = sorted(tags_count.items(), key=lambda x: x[1], reverse=True)
    tags_html = "".join(f"<li><strong>{html.escape(t)}</strong>: {c} meals</li>" for t, c in sorted_tags)
    if not tags_html:
        tags_html = "<li>No tags recorded yet.</li>"

    body = f"""
    <h1>Trigger Explorer</h1>
    <div class="card"><h3>Most Frequent Meal Tags</h3><ul>{tags_html}</ul></div>
    <div class="card"><h3>Correlation Analysis — Coming Soon</h3>
      <p>Future versions will show correlations like "lactose meals → +35% bloating within 4h".</p>
    </div>
    """
    return HTMLResponse(page("Explorer", body))


@app.get("/export/csv")
async def export_csv():
    events = get_all_events()
    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["id", "type", "created_at_utc", "local_datetime", "tags", "meal_notes",
                     "upper_bloating_score", "nausea_score", "belching_score", "heartburn_score", "symptom_notes"])
    for event in events:
        tags = event.get("tags", [])
        tags_str = ",".join(tags) if isinstance(tags, list) else str(tags)
        writer.writerow([
            event.get("id", ""), event.get("type", ""), event.get("created_at_utc", ""),
            event.get("local_datetime", ""), tags_str,
            event.get("notes", "") if event.get("type") == "meal" else "",
            event.get("upper_bloating_score", ""), event.get("nausea_score", ""),
            event.get("belching_score", ""), event.get("heartburn_score", ""),
            event.get("notes", "") if event.get("type") == "symptom" else ""
        ])
    response = StreamingResponse(iter([output.getvalue()]), media_type="text/csv")
    response.headers["Content-Disposition"] = "attachment; filename=guttrace_export.csv"
    return response


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
