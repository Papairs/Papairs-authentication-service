#!/bin/bash
# Test migrations on test database before applying to production

set -e

# Load environment variables
source "$(dirname "$0")/.env"

echo "🧪 Testing migration on TEST database..."
echo "Database: papairs_com_db_auth_test"
echo ""

# Run with test database
export SPRING_DATASOURCE_URL="$DB_TEST_URL"

# Build and run migration
cd "$(dirname "$0")"
mvn flyway:migrate \
  -Dflyway.url="$DB_TEST_URL" \
  -Dflyway.user="$DB_USERNAME" \
  -Dflyway.password="$DB_PASSWORD" \
  -Dflyway.locations=filesystem:src/main/resources/db/migration

echo ""
echo "✅ Test migration completed successfully!"
echo ""
echo "To apply to PRODUCTION, run: ./apply-migration.sh"
