#!/bin/bash
# Apply migrations to PRODUCTION database

set -e

# Load environment variables
source "$(dirname "$0")/.env"

echo "⚠️  WARNING: Applying migration to PRODUCTION database!"
echo "Database: papairs_com_db_auth"
echo ""
read -p "Are you sure? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "❌ Cancelled"
    exit 1
fi

echo ""
echo "🚀 Applying migration to PRODUCTION..."

# Build and run migration
cd "$(dirname "$0")"
mvn flyway:migrate \
  -Dflyway.url="$DB_URL" \
  -Dflyway.user="$DB_USERNAME" \
  -Dflyway.password="$DB_PASSWORD" \
  -Dflyway.locations=filesystem:src/main/resources/db/migration

echo ""
echo "✅ Production migration completed!"
echo ""
echo "Restart Docker to apply: docker-compose restart auth-service"
