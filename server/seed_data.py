"""
Inject realistic dummy data into GutTrace server for testing.
"""
import json
import requests
from datetime import datetime, timedelta
import random
import uuid

BASE_URL = "http://localhost:8000"

def gen_id():
    return str(uuid.uuid4())[:8]

# Generate 7 days of data ending today
today = datetime(2026, 5, 7)
events = []

meal_templates = [
    {"tags": ["lactose", "restaurant"], "notes": "Burger com queijo e milkshake"},
    {"tags": ["spicy", "fried"], "notes": "Frango frito apimentado"},
    {"tags": ["high-fodmap", "garlic/onion"], "notes": "Macarrão ao alho e óleo"},
    {"tags": ["wheat/pizza", "restaurant"], "notes": "Pizza margherita"},
    {"tags": ["beans/legumes"], "notes": "Feijão com arroz"},
    {"tags": ["large meal", "histamine-risk"], "notes": "Salmão grelhado com batatas"},
    {"tags": ["coffee"], "notes": "Café preto e torrada"},
    {"tags": ["fermented", "histamine-risk"], "notes": "Queijo e vinho"},
    {"tags": ["raw fish", "restaurant"], "notes": "Temaki e sashimi"},
    {"tags": ["protein supplement"], "notes": "Whey protein shake"},
    {"tags": ["late meal", "fried"], "notes": "Lanche da madrugada — batata frita"},
    {"tags": ["bubble tea"], "notes": "Milk tea com tapioca"},
]

# Typical meal times (hour, minute)
meal_times = [
    (8, 15),   # café
    (12, 30),  # almoço
    (15, 0),   # lanche
    (19, 30),  # jantar
]

symptom_schedules = [
    (9, 30),   # morning baseline
    (12, 0),   # before lunch
    (14, 30),  # 2h after lunch
    (19, 0),   # before dinner
    (21, 30),  # 2h after dinner
    (23, 30),  # bedtime
]

for day_offset in range(6, -1, -1):
    day = today - timedelta(days=day_offset)
    day_str = day.strftime("%Y-%m-%d")
    
    # Simulate a "bad lactose day" on day 5 ago and "pizza day" 2 days ago
    is_bad_day = day_offset in [5, 2, 0]
    is_late_meal_day = day_offset in [1, 3]
    
    # Add meals (not always all 4)
    for meal_idx, (h, m) in enumerate(meal_times):
        if random.random() < 0.75 or meal_idx in [0, 3]:  # always add breakfast and dinner
            meal_dt = day.replace(hour=h, minute=m)
            meal = random.choice(meal_templates)
            
            # Force bad tags on bad days
            if is_bad_day and meal_idx == 1:
                meal = {"tags": ["lactose", "large meal", "restaurant"], "notes": "Pizza com muito queijo e sorvete"}
            if is_late_meal_day and meal_idx == 3:
                meal = {"tags": ["late meal", "fried", "large meal"], "notes": "Jantar tardio pesado"}

            events.append({
                "id": gen_id(),
                "type": "meal",
                "created_at_utc": meal_dt.isoformat(),
                "local_datetime": meal_dt.isoformat(),
                "photo_ids": [],
                "tags": meal["tags"],
                "notes": meal["notes"],
                "meal_type_inferred": ["breakfast", "lunch", "snack", "dinner"][meal_idx],
            })
    
    # Add symptom check-ins
    for h, m in symptom_schedules:
        sym_dt = day.replace(hour=h, minute=m)
        
        # Worse symptoms in bad days, especially evening
        if is_bad_day and h >= 14:
            bloating = random.randint(3, 5)
            nausea   = random.randint(2, 4)
            belching = random.randint(3, 5)
            heartburn = random.randint(2, 4)
        elif is_late_meal_day and h >= 21:
            bloating = random.randint(3, 5)
            nausea   = random.randint(1, 3)
            belching = random.randint(2, 4)
            heartburn = random.randint(1, 3)
        else:
            bloating = random.randint(0, 2)
            nausea   = random.randint(0, 1)
            belching = random.randint(0, 2)
            heartburn = random.randint(0, 1)
        
        global_score = max(bloating, nausea, belching, heartburn)
        
        events.append({
            "id": gen_id(),
            "type": "symptom",
            "created_at_utc": sym_dt.isoformat(),
            "local_datetime": sym_dt.isoformat(),
            "trigger_type": "scheduled",
            "global_score": global_score,
            "upper_bloating_score": bloating,
            "nausea_score": nausea,
            "belching_score": belching,
            "heartburn_score": heartburn,
        })

print(f"Injecting {len(events)} events...")
r = requests.post(f"{BASE_URL}/sync/events", json=events)
print(f"Status: {r.status_code} — {r.json()}")
print("Done! Open http://localhost:8000 to see the data.")
