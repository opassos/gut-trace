#!/bin/bash

# Check if server is already running on port 8000
PIDS=$(lsof -t -i:8000)
if [ ! -z "$PIDS" ]; then
  echo "⚠️ Found an existing GutTrace server running. Restarting it in background mode..."
else
  echo "🌑 Starting GutTrace Server in background mode..."
fi

# Use the existing start logic to kill old processes
./start_server.sh > server.log 2>&1 &
sleep 1 # Wait a moment to ensure it starts without errors

echo "✅ Server is successfully running in the background."
echo "📝 Logs are being written to: server.log"
echo "🚫 To stop it later, run: pkill -f uvicorn"
echo "🚀 You can now close this terminal safely."
