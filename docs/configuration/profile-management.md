# Profile Management

## Overview

The framework supports three configuration profiles to manage different environments with appropriate settings.

## Available Profiles

### Development
- Full debugging enabled
- Console capture active
- Verbose logging
- Hot reload enabled
- Local service URLs

### Staging
- Moderate debugging
- Selective console capture
- Performance monitoring
- Staging service URLs

### Production
- Minimal logging
- Error tracking only
- Optimized performance
- Production service URLs

## Profile Selection

### Priority Order
1. Environment variable `REACT_APP_PROFILE`
2. `NODE_ENV` mapping
3. `ACTIVE_PROFILE` constant

### Setting Active Profile

#### Method 1: Configuration File
```typescript
// app.profile.ts
export const ACTIVE_PROFILE: ConfigProfile = 'development';
```

#### Method 2: Environment Variable
```bash
REACT_APP_PROFILE=production npm start
```

#### Method 3: Build Time
```bash
REACT_APP_PROFILE=staging npm run build
```

## Profile Configuration

### Development Profile
```typescript
{
  storeDebug: {
    enabled: true,
    logRetentionHours: 24
  },
  consoleRedirect: {
    enabled: true,
    captureAll: true,
    batchInterval: 5000
  },
  features: {
    enableDebugMode: true,
    enableWebSocketLogs: true
  }
}
```

### Staging Profile
```typescript
{
  storeDebug: {
    enabled: false,
    logRetentionHours: 12
  },
  consoleRedirect: {
    enabled: true,
    captureError: true,
    captureWarn: true,
    batchInterval: 10000
  },
  features: {
    enableDebugMode: false,
    enablePerformanceMonitoring: true
  }
}
```

### Production Profile
```typescript
{
  storeDebug: {
    enabled: false,
    logRetentionHours: 1
  },
  consoleRedirect: {
    enabled: false
  },
  features: {
    enableDebugMode: false,
    enablePerformanceMonitoring: true
  }
}
```

## Runtime Override

### Via LocalStorage
```javascript
localStorage.setItem('appConfigOverrides', JSON.stringify({
  storeDebug: { enabled: true }
}));
window.location.reload();
```

### Via Console
```javascript
window.__DEBUG_MODE__ = true;
```

## Best Practices

1. **Never commit profile changes** - Use environment variables
2. **Use development locally** - Full debugging capabilities
3. **Test in staging** - Validate before production
4. **Monitor production** - Use appropriate logging
5. **Document overrides** - Track any runtime changes