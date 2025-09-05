#!/bin/bash

# Orchestrix Database Seed Data Population Script
# This script populates the database with reference data like countries, states, cities, partners, and datacenters

DB_HOST="127.0.0.1"
DB_PORT="3306"
DB_NAME="orchestrix"
DB_USER="root"
DB_PASS="123456"

echo "🚀 Starting Orchestrix Database Seed Data Population..."
echo "Database: $DB_NAME @ $DB_HOST:$DB_PORT"
echo ""

# Check if MySQL is accessible
echo "📡 Testing database connection..."
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS -e "SELECT 1;" $DB_NAME > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ Error: Cannot connect to MySQL database"
    echo "Please ensure:"
    echo "  - MySQL is running"
    echo "  - Database '$DB_NAME' exists"
    echo "  - Credentials are correct (root/123456)"
    echo "  - Host $DB_HOST:$DB_PORT is accessible"
    exit 1
fi
echo "✅ Database connection successful"

# Execute the seed data script
echo "📊 Populating seed data..."
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS $DB_NAME < sql/seed_data.sql

if [ $? -eq 0 ]; then
    echo ""
    echo "🎉 Seed data population completed successfully!"
    echo ""
    echo "📈 Data Summary:"
    mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS $DB_NAME -e "
        SELECT 
            'Countries' as entity, COUNT(*) as count FROM countries
        UNION ALL SELECT 
            'States' as entity, COUNT(*) as count FROM states  
        UNION ALL SELECT 
            'Cities' as entity, COUNT(*) as count FROM cities
        UNION ALL SELECT 
            'Partners' as entity, COUNT(*) as count FROM partners
        UNION ALL SELECT 
            'Datacenters' as entity, COUNT(*) as count FROM datacenters;"
    echo ""
    echo "🏁 Database is ready for use!"
else
    echo "❌ Error occurred during seed data population"
    echo "Check the SQL file: sql/seed_data.sql"
    exit 1
fi