#!/bin/bash

echo "Checking for Dokka documentation..."
ls -la build/ || echo "No build directory found"
ls -la build/dokka/ || echo "No dokka directory found"

if [ ! -f build/dokka/index.html ]; then
    echo "❌ ERROR: Dokka index.html not found at build/dokka/"
    echo "Available HTML files:"
    find build -name "*.html" | head -10 || true
    exit 1
fi

echo "✅ Dokka documentation found at build/dokka/"
mkdir -p htmlMultiModule
cp -R build/dokka/* htmlMultiModule/
ls -la htmlMultiModule/