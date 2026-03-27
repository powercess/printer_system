#!/bin/bash
set -e

# Entrypoint script for printer_system container
# Runs both backend (Spring Boot) and frontend (Nuxt) services

echo "Starting printer_system..."

# Function to handle shutdown gracefully
shutdown() {
    echo "Received shutdown signal, stopping services..."
    if [ -n "$BACKEND_PID" ]; then
        kill $BACKEND_PID 2>/dev/null || true
    fi
    if [ -n "$FRONTEND_PID" ]; then
        kill $FRONTEND_PID 2>/dev/null || true
    fi
    exit 0
}

trap shutdown SIGTERM SIGINT SIGQUIT

# Start backend (Spring Boot on port 8080)
echo "Starting backend service on port 8080..."
java $JAVA_OPTS -jar /app/app.jar &
BACKEND_PID=$!
echo "Backend started with PID: $BACKEND_PID"

# Wait for backend to be ready
echo "Waiting for backend to be ready..."
for i in {1..30}; do
    if curl -sf http://localhost:8080/api-docs > /dev/null 2>&1; then
        echo "Backend is ready!"
        break
    fi
    echo "Waiting for backend... ($i/30)"
    sleep 2
done

# Start frontend (Nuxt on port 3000)
echo "Starting frontend service on port 3000..."
cd /app
node .output/server/index.mjs &
FRONTEND_PID=$!
echo "Frontend started with PID: $FRONTEND_PID"

echo "Both services are running!"
echo "Backend API: http://0.0.0.0:8080"
echo "Frontend: http://0.0.0.0:3000"

# Wait for any process to exit
wait -n $BACKEND_PID $FRONTEND_PID

# If we reach here, one of the processes has exited
echo "A service has exited unexpectedly, shutting down..."
shutdown