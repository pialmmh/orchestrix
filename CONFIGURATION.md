# Orchestrix Configuration Management

This document explains how to use the profile-based configuration system for both frontend and backend applications.

## Overview

Orchestrix uses a comprehensive configuration system that supports three environments:
- **dev** - Development environment (localhost)
- **staging** - Staging environment (staging servers)  
- **prod** - Production environment (production servers)

## Configuration Files

### Main Configuration
- `config.properties` - Central configuration selector
- `scripts/switch-profile.sh` - Profile switching utility

### Backend Configuration (Spring Boot)
- `src/main/resources/application.properties` - Main application properties
- `src/main/resources/application-dev.properties` - Development profile
- `src/main/resources/application-staging.properties` - Staging profile
- `src/main/resources/application-prod.properties` - Production profile

### Frontend Configuration (React)
- `orchestrix-ui/.env` - Default environment file
- `orchestrix-ui/.env.dev` - Development environment
- `orchestrix-ui/.env.staging` - Staging environment
- `orchestrix-ui/.env.prod` - Production environment
- `orchestrix-ui/src/config/index.ts` - Configuration service

## Quick Start

### Viewing Current Configuration
```bash
./scripts/switch-profile.sh --show-config
```

### Switching Profiles
```bash
# Switch both frontend and backend to development
./scripts/switch-profile.sh dev

# Switch both to staging
./scripts/switch-profile.sh staging

# Switch both to production
./scripts/switch-profile.sh prod

# Switch only frontend
./scripts/switch-profile.sh staging --frontend-only

# Switch only backend
./scripts/switch-profile.sh prod --backend-only

# Dry run (show what would change)
./scripts/switch-profile.sh prod --dry-run
```

## Starting Applications

### Backend (Spring Boot)
```bash
# Using default profile (from application.properties)
mvn spring-boot:run

# Explicitly specify profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Using environment variable
SPRING_PROFILES_ACTIVE=staging mvn spring-boot:run
```

### Frontend (React)
```bash
cd orchestrix-ui

# Development
npm run start:dev

# Staging  
npm run start:staging

# Production
npm run start:prod

# Default (uses .env file)
npm start
```

## Environment-Specific Settings

### Development (dev)
- **API URL**: `http://localhost:8090/api`
- **Database**: `orchestrix_dev` on localhost
- **Logging**: DEBUG level, verbose output
- **Features**: Debug tools enabled, more permissive CORS
- **Port**: Backend 8090, Frontend 3010

### Staging (staging)
- **API URL**: `https://staging-api.orchestrix.com/api`
- **Database**: `orchestrix_staging` on staging server
- **Logging**: WARN level, limited output  
- **Features**: Analytics enabled, error reporting
- **Port**: Backend 8080, Frontend 3010 (for local testing)

### Production (prod)
- **API URL**: `https://api.orchestrix.com/api`
- **Database**: `orchestrix` on production server
- **Logging**: ERROR level only
- **Features**: All production features, strict security
- **Port**: Backend 8080, Frontend served via CDN

## Configuration Service (Frontend)

The frontend uses a configuration service at `src/config/index.ts` that:

- Loads settings from environment variables
- Provides type-safe configuration access
- Offers utility methods for API URLs
- Includes environment-specific logging
- Validates configuration in development

### Usage Example
```typescript
import config from '../config';

// Get API endpoint
const endpoint = config.getApiEndpoint('/users');

// Check environment
if (config.isDevelopment()) {
  console.log('Running in development mode');
}

// Access configuration values
const timeout = config.get('apiTimeout');
const debugEnabled = config.get('debug');
```

## Database Configuration

### Development
- **Host**: 127.0.0.1:3306
- **Database**: orchestrix_dev
- **User**: root
- **Password**: 123456

### Staging
- **Host**: staging-db.orchestrix.com:3306
- **Database**: orchestrix_staging
- **User**: orchestrix_user
- **Password**: Set via environment variable

### Production
- **Host**: Set via environment variable
- **Database**: orchestrix
- **User**: Set via environment variable
- **Password**: Set via environment variable

## Security Considerations

### Development
- Generated security passwords (shown in logs)
- More verbose logging
- CORS allows localhost origins
- Redux DevTools enabled

### Staging
- Limited endpoint exposure
- Moderate logging
- Environment-specific CORS origins
- No debug tools

### Production
- Minimal endpoint exposure
- Error-level logging only
- Strict CORS configuration
- SSL required
- All secrets via environment variables

## Environment Variables

### Backend (Production)
```bash
SPRING_PROFILES_ACTIVE=prod
DB_HOST=prod-db.orchestrix.com
DB_USERNAME=orchestrix_user
DB_PASSWORD=secure_password
JWT_SECRET=production_jwt_secret
REDIS_HOST=prod-redis.orchestrix.com
REDIS_PASSWORD=redis_password
SSL_KEYSTORE_PATH=/path/to/keystore.p12
SSL_KEYSTORE_PASSWORD=keystore_password
```

### Frontend (Production)
```bash
REACT_APP_API_URL=https://api.orchestrix.com/api
REACT_APP_ENVIRONMENT=production
BUILD_NUMBER=v1.0.0-build.123
REACT_APP_ANALYTICS_ID=UA-PRODUCTION-ID
```

## Deployment

### Development
```bash
# Start backend
mvn spring-boot:run

# Start frontend
cd orchestrix-ui && npm run start:dev
```

### Staging
```bash
# Switch to staging
./scripts/switch-profile.sh staging

# Build and deploy backend
mvn clean package -Pstaging
java -jar target/orchestrix-*.jar

# Build and deploy frontend  
cd orchestrix-ui
npm run build:staging
```

### Production
```bash
# Switch to production
./scripts/switch-profile.sh prod

# Build backend
mvn clean package -Pprod

# Build frontend
cd orchestrix-ui
npm run build:prod
```

## Troubleshooting

### Profile Not Active
- Check `application.properties` has correct `spring.profiles.active`
- Verify profile-specific files exist
- Check for typos in profile names

### Frontend Configuration Issues
- Ensure environment files are properly formatted
- Check `REACT_APP_` prefix on all custom variables
- Restart dev server after changing environment files

### Database Connection Issues
- Verify database exists for the target profile
- Check connection parameters in profile-specific properties
- Ensure database server is running

### API Connection Issues
- Verify API URL in environment configuration
- Check CORS configuration allows frontend origin
- Ensure backend is running on correct port

## Best Practices

1. **Never commit secrets** - Use environment variables for production
2. **Test profile switching** - Verify configurations work before deployment
3. **Use the configuration service** - Don't hardcode URLs in frontend components
4. **Monitor logs** - Check application startup logs for active profile
5. **Document changes** - Update this file when adding new configuration options

## Support

For configuration issues, check:
1. Application startup logs
2. Configuration service output (frontend)
3. Profile switching script output
4. Environment-specific property files