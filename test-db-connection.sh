#!/bin/bash

# Test database connection script
DB_HOST="127.0.0.1"
DB_PORT="3306"
DB_NAME="orchestrix"
DB_USER="root"
DB_PASS="123456"

echo "🧪 Testing Orchestrix database connection..."
echo "Connecting to: $DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"

mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS -e "
    SELECT 
        'Database Connection' as status, 
        'SUCCESS' as result,
        DATABASE() as current_db,
        NOW() as timestamp;
    
    SHOW TABLES;" $DB_NAME

if [ $? -eq 0 ]; then
    echo "✅ Database connection successful!"
    echo "💡 You can now run: ./populate-seed-data.sh"
else
    echo "❌ Database connection failed!"
    echo "Please check:"
    echo "  - MySQL is running: sudo systemctl status mysql"
    echo "  - Database exists: CREATE DATABASE IF NOT EXISTS orchestrix;"
    echo "  - Credentials are correct"
fi