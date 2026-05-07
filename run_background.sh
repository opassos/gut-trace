#!/bin/bash

echo "🌑 Starting GutTrace Server in background mode..."

# Use the existing start logic to kill old processes
./start_server.sh > server.log 2>&1 &

echo "✅ Server is running in the background."
echo "📝 Logs are being written to: server.log"
echo "🚫 To stop it later, just run ./start_server.sh (it kills the old one) and press Ctrl+C."
echo "🚀 You can now close this terminal safely."
