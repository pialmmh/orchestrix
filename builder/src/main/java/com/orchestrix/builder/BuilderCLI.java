package com.orchestrix.builder;

import com.orchestrix.builder.containers.*;
import java.util.Scanner;

/**
 * Command-line interface for container builders
 */
public class BuilderCLI {
    
    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                printUsage();
                System.exit(1);
            }
            
            String containerType = args[0];
            String configFile = args.length > 1 ? args[1] : "build.yaml";
            boolean forceMode = args.length > 2 && args[2].equals("--force");
            
            // Load configuration
            BuildConfig config = BuildConfig.fromYaml(configFile);
            config.setForceMode(forceMode);
            
            // Version is handled by shell script wrapper
            // The shell script updates the YAML before calling this
            
            // Create appropriate builder
            LXCBuilder builder = createBuilder(containerType, config);
            
            // Run build
            builder.build();
            
        } catch (Exception e) {
            System.err.println("Build failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static LXCBuilder createBuilder(String type, BuildConfig config) {
        switch (type.toLowerCase()) {
            case "dev-env":
                return new DevEnvBuilder(config);
            // Add more container types here
            // case "jenkins-agent":
            //     return new JenkinsAgentBuilder(config);
            // case "database":
            //     return new DatabaseBuilder(config);
            default:
                throw new IllegalArgumentException("Unknown container type: " + type);
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: java -jar container-builder.jar <container-type> [config-file] [--force]");
        System.out.println();
        System.out.println("Container types:");
        System.out.println("  dev-env       - Development environment with Jenkins agent");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  config-file   - Path to YAML configuration (default: <type>/build.yaml)");
        System.out.println("  --force       - Skip prompts and use defaults");
    }
}