package com.orchestrix.builder;

import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.util.*;

/**
 * Build configuration loaded from YAML file
 */
public class BuildConfig {
    private String containerName;
    private int version = 1;
    private String baseImage = "debian/12";
    private int timeout = 300; // seconds
    private boolean forceMode = false;
    private Map<String, Object> customSettings = new HashMap<>();
    
    // Package groups for installation
    private List<String> corePackages = Arrays.asList(
        "curl", "wget", "git", "vim", "jq"
    );
    
    private List<String> buildTools = Arrays.asList(
        "make", "gcc", "build-essential"
    );
    
    private List<String> additionalPackages = new ArrayList<>();
    
    public static BuildConfig fromYaml(String yamlFile) {
        try (InputStream input = new FileInputStream(yamlFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(input);
            
            BuildConfig config = new BuildConfig();
            config.containerName = (String) data.get("name");
            config.version = (Integer) data.getOrDefault("version", 1);
            config.baseImage = (String) data.getOrDefault("base_image", "debian/12");
            config.timeout = (Integer) data.getOrDefault("timeout", 300);
            
            // Load package lists
            Map<String, List<String>> packages = (Map<String, List<String>>) data.get("packages");
            if (packages != null) {
                if (packages.containsKey("core")) {
                    config.corePackages = packages.get("core");
                }
                if (packages.containsKey("build")) {
                    config.buildTools = packages.get("build");
                }
                if (packages.containsKey("additional")) {
                    config.additionalPackages = packages.get("additional");
                }
            }
            
            // Custom settings for specific builders
            if (data.containsKey("custom")) {
                config.customSettings = (Map<String, Object>) data.get("custom");
            }
            
            return config;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config: " + e.getMessage());
        }
    }
    
    // Getters and setters
    public String getContainerName() { return containerName; }
    public void setContainerName(String name) { this.containerName = name; }
    
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    
    public String getBaseImage() { return baseImage; }
    public void setBaseImage(String image) { this.baseImage = image; }
    
    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
    
    public boolean isForceMode() { return forceMode; }
    public void setForceMode(boolean force) { this.forceMode = force; }
    
    public List<String> getCorePackages() { return corePackages; }
    public List<String> getBuildPackages() { return buildTools; }
    public List<String> getBuildTools() { return buildTools; } // Alias for compatibility
    public List<String> getAdditionalPackages() { return additionalPackages; }
    
    public Object getCustomSetting(String key) {
        return customSettings.get(key);
    }
    
    public String getCustomString(String key, String defaultValue) {
        return customSettings.getOrDefault(key, defaultValue).toString();
    }
    
    public boolean getCustomBoolean(String key, boolean defaultValue) {
        return (boolean) customSettings.getOrDefault(key, defaultValue);
    }
}