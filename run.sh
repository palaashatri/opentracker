#!/bin/bash

# Aetheris Platform Runner
# This script builds and runs the full-stack Geospatial Intelligence platform.

set -e

# --- Configuration ---
JAVA_21_HOME="/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home"
if [ -d "$JAVA_21_HOME" ]; then
    export JAVA_HOME="$JAVA_21_HOME"
fi

echo "🚀 Starting Aetheris Platform..."
echo "☕ Using Java: $(java -version 2>&1 | head -n 1)"
echo "📦 Using Node: $(node -v)"

# --- Cleanup on Exit ---
trap "kill 0" EXIT

# --- Build Phase ---
echo "🛠️  Building Backend..."
./gradlew build -x test

echo "🛠️  Installing Frontend Dependencies..."
(cd frontend && npm install --legacy-peer-deps)

# --- Execution Phase ---
echo "📡 Launching Ingestion Gateway (Port 8082)..."
./gradlew :backend:ingestion:bootRun > ingestion.log 2>&1 &

echo "🛰️  Launching Streaming Core (Port 8081)..."
./gradlew :backend:streaming-core:bootRun > streaming.log 2>&1 &

echo "📊 Launching GraphQL API (Port 8080)..."
./gradlew :backend:graphql-api:bootRun > graphql.log 2>&1 &

echo "🌐 Launching Frontend Dev Server..."
(cd frontend && npm run dev)
