# Stellar Query System Integration Guide

## Overview
The Stellar Query System is a powerful SQL query builder and executor that enables hierarchical data fetching with support for lazy loading, nested queries, and CRUD operations. This guide will help you integrate it into the Orchestrix project.

## Files Already Copied

### Backend Files (Spring Boot)
Location: `/home/mustafa/telcobright-projects/orchestrix/src/main/java/com/orchestrix/stellar/`

- **model/** - Data models (QueryNode, Criteria, Page, etc.)
- **json/** - JSON parser for converting frontend queries
- **sql/** - SQL query builders
- **exec/** - Query executor
- **result/** - Result mapping classes
- **schema/** - Database schema definitions
- **controller/** - REST controllers
- **service/** - Business services

### Frontend Files (React)
Location: `/home/mustafa/telcobright-projects/orchestrix/orchestrix-ui/src/hooks/stellar/`

- **LazyQueryObservable.ts** - MobX observable for lazy loading
- **StellarQueryHooks.tsx** - React hooks for queries
- **CustomerExample.tsx** - Example components

## Backend Integration Steps

### Step 1: Add Dependencies to pom.xml

Add these dependencies to `/home/mustafa/telcobright-projects/orchestrix/pom.xml`:

```xml
<!-- Add to dependencies section -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>

<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

### Step 2: Configure Database Connection

Add to `application.properties` or `application.yml`:

```properties
# MySQL Configuration
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/northwind?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# HikariCP settings
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
```

### Step 3: Enable CORS for React Frontend

Create or update CORS configuration:

```java
package com.orchestrix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        config.addAllowedOrigin("http://localhost:3000"); // React dev server
        config.addAllowedOrigin("http://localhost:3001"); // Alternative port
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

### Step 4: Fix Import Issues

The copied files need these adjustments:

1. **Update model imports** in EntityModificationService.java:
```java
import com.orchestrix.stellar.model.*;
```

2. **Remove or update Spring package references**:
   - Change `com.telcobright.stellar.spring.model` to `com.orchestrix.stellar.model`
   - Change `com.telcobright.stellar.spring.service` to `com.orchestrix.stellar.service`

### Step 5: Compile and Test Backend

```bash
cd /home/mustafa/telcobright-projects/orchestrix
mvn clean compile
mvn spring-boot:run
```

Test the endpoint:
```bash
curl http://localhost:8080/api/health
```

## Frontend Integration Steps

### Step 1: Install Dependencies

```bash
cd /home/mustafa/telcobright-projects/orchestrix/orchestrix-ui
npm install mobx mobx-react-lite axios
```

### Step 2: Update TypeScript Configuration

Ensure `tsconfig.json` includes:

```json
{
  "compilerOptions": {
    "experimentalDecorators": true,
    "useDefineForClassFields": true
  }
}
```

### Step 3: Configure API URL

Create `.env` file in orchestrix-ui:

```env
REACT_APP_API_URL=http://localhost:8080
```

### Step 4: Update Hook Configuration

Edit `/orchestrix-ui/src/hooks/stellar/StellarQueryHooks.tsx`:

```typescript
// Change the default URL
const SPRING_BOOT_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';
```

### Step 5: Use in Your Components

```typescript
import { useStellarQuery } from './hooks/stellar/StellarQueryHooks';

function YourComponent() {
  const { data, loading, error } = useStellarQuery({
    kind: 'customer',
    page: { limit: 20 }
  });

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;
  
  return (
    <div>
      {data.map(item => (
        <div key={item.id}>{item.name}</div>
      ))}
    </div>
  );
}
```

## API Endpoints

The system provides these endpoints:

### Query Endpoint
- **POST** `/api/query` - Execute queries with nested includes
- **POST** `/api/{kind}` - Query specific entity type

### Modification Endpoint
- **POST** `/api/modify` - Insert/Update/Delete entities

### Cache Management
- **GET** `/api/cache/stats` - View cache statistics
- **DELETE** `/api/cache/clear` - Clear entity cache

### Lazy Loading (if enabled)
- **POST** `/api/lazy/query` - Execute query with lazy placeholders
- **POST** `/api/lazy/load/{key}` - Load lazy data

## Query Examples

### Simple Query
```javascript
{
  "kind": "customer",
  "page": { "limit": 10 }
}
```

### Query with Criteria
```javascript
{
  "kind": "product",
  "criteria": { 
    "CategoryID": [1, 2, 3],
    "UnitsInStock": [">10"]
  },
  "page": { "limit": 20 }
}
```

### Nested Query
```javascript
{
  "kind": "customer",
  "page": { "limit": 5 },
  "include": [{
    "kind": "salesorder",
    "page": { "limit": 10 },
    "include": [{
      "kind": "orderdetail",
      "page": { "limit": 5 }
    }]
  }]
}
```

### Modification Request
```javascript
{
  "entityName": "product",
  "operation": "INSERT",
  "data": {
    "ProductName": "New Product",
    "CategoryID": 1,
    "UnitPrice": 19.99,
    "UnitsInStock": 100
  }
}
```

## Database Schema Required

The system expects a Northwind-like schema with these tables:
- category (CategoryID, CategoryName, Description)
- product (ProductID, ProductName, CategoryID, SupplierID, UnitPrice, UnitsInStock)
- customer (CustomerID, CompanyName, ContactName, City)
- salesorder (OrderID, CustomerID, EmployeeID, OrderDate)
- orderdetail (OrderID, ProductID, Quantity, UnitPrice)
- employee (EmployeeID, FirstName, LastName)
- supplier (SupplierID, CompanyName)
- shipper (ShipperID, CompanyName)

## Troubleshooting

### Common Issues and Solutions

1. **CORS Error**
   - Ensure Spring Boot CORS is configured
   - Check that frontend URL is whitelisted

2. **Database Connection Failed**
   - Verify MySQL is running: `mysql -h 127.0.0.1 -u root -p`
   - Check credentials in application.properties

3. **Compilation Errors**
   - Run: `find . -name "*.java" -exec grep -l "telcobright" {} \;`
   - Replace remaining `telcobright` references with `orchestrix`

4. **Frontend Module Not Found**
   - Ensure all npm dependencies are installed
   - Check import paths are relative: `./hooks/stellar/`

5. **API Returns 404**
   - Verify controller is annotated with `@RestController`
   - Check `@RequestMapping` path matches frontend calls

## Testing the Integration

### Backend Test
```bash
# Test query endpoint
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"kind":"customer","page":{"limit":5}}'

# Test health endpoint
curl http://localhost:8080/api/health
```

### Frontend Test
1. Start backend: `mvn spring-boot:run`
2. Start frontend: `npm start`
3. Open browser: http://localhost:3000
4. Check console for errors
5. Verify data loads in components

## Performance Tips

1. **Use pagination** - Always include `page: { limit: X }`
2. **Lazy loading** - Mark nested includes as `lazy: true` for large datasets
3. **Caching** - Entity hierarchies are cached automatically
4. **Indexing** - Ensure database columns used in criteria are indexed

## Security Considerations

1. **Input validation** - The system validates entity relationships
2. **SQL injection** - Uses prepared statements
3. **CORS** - Configure for production domains only
4. **Authentication** - Add Spring Security for production

## Next Steps

1. Customize the schema definitions in `NorthwindSchema.java` for your database
2. Add authentication/authorization if needed
3. Implement custom business logic in services
4. Add logging and monitoring
5. Configure for production deployment

## Support Files Location

- Backend source: `/home/mustafa/telcobright-projects/orchestrix/src/main/java/com/orchestrix/stellar/`
- Frontend hooks: `/home/mustafa/telcobright-projects/orchestrix/orchestrix-ui/src/hooks/stellar/`
- This guide: `/home/mustafa/telcobright-projects/orchestrix/STELLAR_INTEGRATION_GUIDE.md`

## Contact for Issues

If you encounter issues not covered here:
1. Check the original implementation in `/home/mustafa/telcobright-projects/routesphere/stellar/`
2. Review test files in `/home/mustafa/telcobright-projects/routesphere/stellar-node-tester/`
3. Ensure all dependencies are properly installed