package com.telcobright.orchestrix.controller;

import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TestController {
    
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Orchestrix API");
        return response;
    }
    
    @PostMapping("/auth/logout")
    public Map<String, String> logout() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return response;
    }
    
    @PostMapping("/auth/refresh")
    public Map<String, Object> refresh(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> user = new HashMap<>();
        user.put("id", "1");
        user.put("username", "admin");
        user.put("email", "admin@orchestrix.com");
        user.put("firstName", "System");
        user.put("lastName", "Administrator");
        user.put("fullName", "System Administrator");
        user.put("role", "SUPER_ADMIN");
        user.put("status", "ACTIVE");
        user.put("permissions", java.util.Arrays.asList("ALL"));
        user.put("themePreference", "light");
        user.put("language", "en");
        
        response.put("user", user);
        response.put("token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkFkbWluIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
        response.put("refreshToken", "refresh-token-12345");
        return response;
    }
    
    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials) {
        Map<String, Object> response = new HashMap<>();
        
        if ("admin".equals(credentials.get("username")) && "admin".equals(credentials.get("password"))) {
            Map<String, Object> user = new HashMap<>();
            user.put("id", "1");
            user.put("username", "admin");
            user.put("email", "admin@orchestrix.com");
            user.put("firstName", "System");
            user.put("lastName", "Administrator");
            user.put("fullName", "System Administrator");
            user.put("role", "SUPER_ADMIN");
            user.put("status", "ACTIVE");
            user.put("permissions", java.util.Arrays.asList("ALL"));
            user.put("themePreference", "light");
            user.put("language", "en");
            
            response.put("user", user);
            response.put("token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkFkbWluIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
            response.put("refreshToken", "refresh-token-12345");
        } else {
            throw new RuntimeException("Invalid credentials");
        }
        
        return response;
    }
    
    @GetMapping("/users")
    public Map<String, Object> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> response = new HashMap<>();
        
        java.util.List<Map<String, Object>> users = new java.util.ArrayList<>();
        Map<String, Object> adminUser = new HashMap<>();
        adminUser.put("id", "1");
        adminUser.put("username", "admin");
        adminUser.put("email", "admin@orchestrix.com");
        adminUser.put("firstName", "System");
        adminUser.put("lastName", "Administrator");
        adminUser.put("fullName", "System Administrator");
        adminUser.put("role", "SUPER_ADMIN");
        adminUser.put("status", "ACTIVE");
        adminUser.put("department", "IT");
        adminUser.put("lastLoginAt", "2024-01-15T10:30:00Z");
        users.add(adminUser);
        
        // Add some sample users
        for (int i = 2; i <= 5; i++) {
            Map<String, Object> user = new HashMap<>();
            user.put("id", String.valueOf(i));
            user.put("username", "user" + i);
            user.put("email", "user" + i + "@orchestrix.com");
            user.put("firstName", "User");
            user.put("lastName", String.valueOf(i));
            user.put("fullName", "User " + i);
            user.put("role", i % 2 == 0 ? "MANAGER" : "OPERATOR");
            user.put("status", "ACTIVE");
            user.put("department", i % 2 == 0 ? "Operations" : "Support");
            user.put("lastLoginAt", "2024-01-14T15:45:00Z");
            users.add(user);
        }
        
        response.put("users", users);
        response.put("totalUsers", users.size());
        response.put("page", page);
        response.put("size", size);
        response.put("totalPages", 1);
        
        return response;
    }
    
    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody Map<String, String> userData) {
        Map<String, Object> newUser = new HashMap<>();
        newUser.put("id", String.valueOf(System.currentTimeMillis()));
        newUser.put("username", userData.get("username"));
        newUser.put("email", userData.get("email"));
        newUser.put("firstName", userData.get("firstName"));
        newUser.put("lastName", userData.get("lastName"));
        newUser.put("fullName", userData.get("firstName") + " " + userData.get("lastName"));
        newUser.put("role", userData.get("role"));
        newUser.put("status", "ACTIVE");
        newUser.put("department", userData.get("department"));
        newUser.put("createdAt", java.time.Instant.now().toString());
        
        return newUser;
    }
    
    @PutMapping("/users/{userId}/status")
    public Map<String, String> updateUserStatus(
            @PathVariable String userId,
            @RequestBody Map<String, String> statusData) {
        Map<String, String> response = new HashMap<>();
        response.put("id", userId);
        response.put("status", statusData.get("status"));
        response.put("message", "User status updated successfully");
        
        return response;
    }
    
    @DeleteMapping("/users/{userId}")
    public Map<String, String> deleteUser(@PathVariable String userId) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully");
        response.put("userId", userId);
        
        return response;
    }
    
    // Countries Endpoint
    @GetMapping("/countries")
    public Map<String, Object> getCountries() {
        Map<String, Object> response = new HashMap<>();
        java.util.List<Map<String, Object>> countries = new java.util.ArrayList<>();
        
        // Major countries with their codes and regions
        String[][] countryData = {
            {"US", "United States", "North America", "USA"},
            {"CA", "Canada", "North America", "CAN"},
            {"MX", "Mexico", "North America", "MEX"},
            {"BR", "Brazil", "South America", "BRA"},
            {"AR", "Argentina", "South America", "ARG"},
            {"GB", "United Kingdom", "Europe", "GBR"},
            {"DE", "Germany", "Europe", "DEU"},
            {"FR", "France", "Europe", "FRA"},
            {"IT", "Italy", "Europe", "ITA"},
            {"ES", "Spain", "Europe", "ESP"},
            {"NL", "Netherlands", "Europe", "NLD"},
            {"SE", "Sweden", "Europe", "SWE"},
            {"NO", "Norway", "Europe", "NOR"},
            {"FI", "Finland", "Europe", "FIN"},
            {"DK", "Denmark", "Europe", "DNK"},
            {"CH", "Switzerland", "Europe", "CHE"},
            {"AT", "Austria", "Europe", "AUT"},
            {"BE", "Belgium", "Europe", "BEL"},
            {"PL", "Poland", "Europe", "POL"},
            {"CZ", "Czech Republic", "Europe", "CZE"},
            {"RU", "Russia", "Europe/Asia", "RUS"},
            {"CN", "China", "Asia", "CHN"},
            {"JP", "Japan", "Asia", "JPN"},
            {"KR", "South Korea", "Asia", "KOR"},
            {"IN", "India", "Asia", "IND"},
            {"SG", "Singapore", "Asia", "SGP"},
            {"MY", "Malaysia", "Asia", "MYS"},
            {"TH", "Thailand", "Asia", "THA"},
            {"ID", "Indonesia", "Asia", "IDN"},
            {"PH", "Philippines", "Asia", "PHL"},
            {"VN", "Vietnam", "Asia", "VNM"},
            {"AU", "Australia", "Oceania", "AUS"},
            {"NZ", "New Zealand", "Oceania", "NZL"},
            {"ZA", "South Africa", "Africa", "ZAF"},
            {"EG", "Egypt", "Africa", "EGY"},
            {"NG", "Nigeria", "Africa", "NGA"},
            {"KE", "Kenya", "Africa", "KEN"},
            {"IL", "Israel", "Middle East", "ISR"},
            {"AE", "United Arab Emirates", "Middle East", "ARE"},
            {"SA", "Saudi Arabia", "Middle East", "SAU"},
            {"TR", "Turkey", "Europe/Asia", "TUR"},
            {"IE", "Ireland", "Europe", "IRL"},
            {"PT", "Portugal", "Europe", "PRT"},
            {"GR", "Greece", "Europe", "GRC"},
            {"HK", "Hong Kong", "Asia", "HKG"},
            {"TW", "Taiwan", "Asia", "TWN"},
            {"CL", "Chile", "South America", "CHL"},
            {"CO", "Colombia", "South America", "COL"},
            {"PE", "Peru", "South America", "PER"}
        };
        
        for (String[] country : countryData) {
            Map<String, Object> countryMap = new HashMap<>();
            countryMap.put("code", country[0]);
            countryMap.put("name", country[1]);
            countryMap.put("region", country[2]);
            countryMap.put("code3", country[3]);
            countries.add(countryMap);
        }
        
        response.put("countries", countries);
        response.put("total", countries.size());
        return response;
    }
    
    // Resource Management Endpoints
    @GetMapping("/resources/datacenters")
    public Map<String, Object> getDatacenters() {
        Map<String, Object> response = new HashMap<>();
        java.util.List<Map<String, Object>> datacenters = new java.util.ArrayList<>();
        
        // Sample datacenters
        String[][] dcData = {
            {"1", "US-East-1", "Virginia", "US", "Primary", "ACTIVE", "AWS", "40.7128,-74.0060"},
            {"2", "EU-West-1", "Frankfurt", "DE", "Secondary", "ACTIVE", "AWS", "50.1109,8.6821"},
            {"3", "AP-Southeast-1", "Singapore", "SG", "Regional", "ACTIVE", "Azure", "1.3521,103.8198"},
            {"4", "US-West-2", "California", "US", "Backup", "MAINTENANCE", "GCP", "37.7749,-122.4194"}
        };
        
        for (String[] dc : dcData) {
            Map<String, Object> datacenter = new HashMap<>();
            datacenter.put("id", dc[0]);
            datacenter.put("name", dc[1]);
            datacenter.put("location", dc[2]);
            datacenter.put("country", dc[3]);
            datacenter.put("type", dc[4]);
            datacenter.put("status", dc[5]);
            datacenter.put("provider", dc[6]);
            datacenter.put("coordinates", dc[7]);
            datacenter.put("servers", Integer.parseInt(dc[0]) * 25);
            datacenter.put("storage", Integer.parseInt(dc[0]) * 500 + "TB");
            datacenter.put("utilization", 45 + Integer.parseInt(dc[0]) * 10);
            datacenters.add(datacenter);
        }
        
        response.put("datacenters", datacenters);
        response.put("total", datacenters.size());
        return response;
    }
    
    @GetMapping("/resources/compute")
    public Map<String, Object> getComputeResources() {
        Map<String, Object> response = new HashMap<>();
        java.util.List<Map<String, Object>> resources = new java.util.ArrayList<>();
        
        // Sample compute resources
        String[][] computeData = {
            {"1", "prod-web-01", "VM", "Ubuntu 22.04", "RUNNING", "m5.2xlarge", "8", "32", "US-East-1"},
            {"2", "prod-db-01", "VM", "RHEL 8", "RUNNING", "r5.4xlarge", "16", "128", "US-East-1"},
            {"3", "dev-api-01", "Container", "Alpine Linux", "RUNNING", "t3.medium", "2", "4", "EU-West-1"},
            {"4", "test-app-01", "VM", "Windows Server 2022", "STOPPED", "t3.large", "4", "8", "US-West-2"},
            {"5", "prod-k8s-node-01", "Physical", "Ubuntu 20.04", "RUNNING", "Bare Metal", "32", "256", "US-East-1"},
            {"6", "prod-docker-01", "Container", "Docker", "RUNNING", "c5.xlarge", "4", "8", "AP-Southeast-1"}
        };
        
        for (String[] comp : computeData) {
            Map<String, Object> resource = new HashMap<>();
            resource.put("id", comp[0]);
            resource.put("name", comp[1]);
            resource.put("type", comp[2]);
            resource.put("os", comp[3]);
            resource.put("status", comp[4]);
            resource.put("instanceType", comp[5]);
            resource.put("cpu", comp[6]);
            resource.put("memory", comp[7]);
            resource.put("datacenter", comp[8]);
            resource.put("ipAddress", "10.0." + comp[0] + ".100");
            resource.put("uptime", (Integer.parseInt(comp[0]) * 24) + " hours");
            resources.add(resource);
        }
        
        response.put("resources", resources);
        response.put("total", resources.size());
        return response;
    }
    
    @GetMapping("/resources/storage")
    public Map<String, Object> getStorageResources() {
        Map<String, Object> response = new HashMap<>();
        java.util.List<Map<String, Object>> storages = new java.util.ArrayList<>();
        
        // Sample storage resources
        String[][] storageData = {
            {"1", "Google Drive - Production", "CLOUD", "Google Drive", "100TB", "45TB", "ACTIVE", "OAuth2", "drive.google.com"},
            {"2", "AWS S3 - Backups", "CLOUD", "AWS S3", "500TB", "320TB", "ACTIVE", "IAM Role", "s3.amazonaws.com"},
            {"3", "GitHub - Code Repos", "CLOUD", "GitHub", "50GB", "12GB", "ACTIVE", "Token", "github.com"},
            {"4", "FTP Server - Legacy", "NETWORK", "FTP", "10TB", "7TB", "DEPRECATED", "User/Pass", "ftp.company.com"},
            {"5", "SFTP - Partners", "NETWORK", "SFTP", "25TB", "18TB", "ACTIVE", "SSH Key", "sftp.company.com"},
            {"6", "NAS - Office", "ATTACHED", "Network Attached", "100TB", "67TB", "ACTIVE", "SMB", "192.168.1.50"},
            {"7", "Azure Blob", "CLOUD", "Azure Storage", "200TB", "140TB", "ACTIVE", "SAS Token", "blob.core.windows.net"},
            {"8", "Local SAN", "ATTACHED", "Storage Area Network", "1PB", "650TB", "ACTIVE", "iSCSI", "10.0.0.100"}
        };
        
        for (String[] stor : storageData) {
            Map<String, Object> storage = new HashMap<>();
            storage.put("id", stor[0]);
            storage.put("name", stor[1]);
            storage.put("category", stor[2]);
            storage.put("type", stor[3]);
            storage.put("capacity", stor[4]);
            storage.put("used", stor[5]);
            storage.put("status", stor[6]);
            storage.put("authType", stor[7]);
            storage.put("endpoint", stor[8]);
            storage.put("encryption", "AES-256");
            storage.put("backupEnabled", !stor[0].equals("4"));
            storage.put("lastBackup", "2024-01-15 03:00:00");
            storages.add(storage);
        }
        
        response.put("storages", storages);
        response.put("total", storages.size());
        return response;
    }
}