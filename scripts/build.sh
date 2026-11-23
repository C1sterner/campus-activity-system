#!/usr/bin/env sh
set -eu
cd "$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)"
npm --prefix frontend ci --no-audit --no-fund
npm --prefix frontend run build
mkdir -p backend/src/main/resources/static
cp -R frontend/dist/. backend/src/main/resources/static/
cd backend
sh ./mvnw -B package
printf '%s\n' 'Built backend/target/campus-activity-0.1.0.jar'
