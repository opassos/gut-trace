#!/bin/bash

echo "🚀 Starting GutTrace Server..."

# Kill any existing process using port 8000
PIDS=$(lsof -t -i:8000)
if [ ! -z "$PIDS" ]; then
  echo "⚠️ Found an existing server running on port 8000. Killing it..."
  kill -9 $PIDS
fi

# Get the absolute path to the directory where the script is located
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR/server"

# Activate virtual environment
if [ -f "venv/bin/activate" ]; then
    source venv/bin/activate
else
    echo "❌ Error: Virtual environment not found in server/venv/"
    exit 1
fi

# Start the server
echo "✅ Server is running!"
echo "📡 Listening on your local network for connections..."
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
