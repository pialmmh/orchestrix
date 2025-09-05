#!/bin/bash

echo "🚀 Starting Orchestrix with Claude Bridge..."

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Kill any existing processes on ports
kill -9 $(lsof -t -i:3456) 2>/dev/null
kill -9 $(lsof -t -i:3010) 2>/dev/null

# Start Claude Bridge server
echo "📡 Starting Claude Bridge server..."
cd "$SCRIPT_DIR/claude-bridge" && npm start &
BRIDGE_PID=$!

# Wait for bridge to start
sleep 2

# Start React app
echo "⚛️ Starting React app..."
cd "$SCRIPT_DIR" && npm start &
REACT_PID=$!

echo "✅ Both servers started!"
echo "   - Claude Bridge: http://localhost:3456"
echo "   - React App: http://localhost:3010"
echo ""
echo "Press Ctrl+C to stop both servers"

# Handle Ctrl+C
trap "echo 'Stopping servers...'; kill $BRIDGE_PID $REACT_PID; exit" INT

# Wait for both processes
wait