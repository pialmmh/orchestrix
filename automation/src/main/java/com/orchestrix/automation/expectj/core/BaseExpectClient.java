package com.orchestrix.automation.expectj.core;

import com.orchestrix.automation.expectj.api.ExpectClient;
import expectj.ExpectJ;
import expectj.Spawn;
import expectj.TimeoutException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Base implementation of ExpectClient using ExpectJ library
 */
public abstract class BaseExpectClient implements ExpectClient {
    
    protected ExpectJ expectJ;
    protected Spawn spawn;
    protected boolean verbose = false;
    protected long defaultTimeoutSeconds = 30;
    protected StringBuilder allOutput = new StringBuilder();
    
    public BaseExpectClient() {
        try {
            this.expectJ = new ExpectJ();
            configureExpectJ();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize ExpectJ", e);
        }
    }
    
    /**
     * Configure ExpectJ settings - override in subclasses
     */
    protected void configureExpectJ() {
        expectJ.setDefaultTimeout(defaultTimeoutSeconds);
    }
    
    @Override
    public void spawn(String command) throws ExpectException {
        try {
            beforeSpawn(command);
            spawn = expectJ.spawn(command);
            afterSpawn();
        } catch (IOException e) {
            throw new ExpectException("Failed to spawn command: " + command, e);
        }
    }
    
    @Override
    public void spawn(String command, String[] env) throws ExpectException {
        try {
            beforeSpawn(command);
            spawn = expectJ.spawn(command, env);
            afterSpawn();
        } catch (IOException e) {
            throw new ExpectException("Failed to spawn command: " + command, e);
        }
    }
    
    /**
     * Hook called before spawning - override in subclasses
     */
    protected void beforeSpawn(String command) {
        if (verbose) {
            System.out.println("[SPAWN] " + command);
        }
    }
    
    /**
     * Hook called after spawning - override in subclasses
     */
    protected void afterSpawn() {
        // Override in subclasses
    }
    
    @Override
    public String expect(String pattern) throws ExpectException {
        return expect(pattern, defaultTimeoutSeconds, TimeUnit.SECONDS);
    }
    
    @Override
    public String expect(String pattern, long timeout, TimeUnit unit) throws ExpectException {
        if (spawn == null) {
            throw new ExpectException("No process spawned");
        }
        
        try {
            long timeoutSeconds = unit.toSeconds(timeout);
            spawn.setTimeout(timeoutSeconds);
            
            if (verbose) {
                System.out.println("[EXPECT] Pattern: " + pattern);
            }
            
            spawn.expect(pattern);
            String matched = spawn.getCurrentStandardOutContents();
            allOutput.append(matched);
            
            if (verbose) {
                System.out.println("[MATCHED] " + matched);
            }
            
            return matched;
            
        } catch (IOException | TimeoutException e) {
            throw new ExpectException("Failed to match pattern: " + pattern, e);
        }
    }
    
    @Override
    public ExpectResult expectMultiple(String... patterns) throws ExpectException {
        if (spawn == null) {
            throw new ExpectException("No process spawned");
        }
        
        if (patterns == null || patterns.length == 0) {
            throw new ExpectException("No patterns provided");
        }
        
        try {
            String beforeMatch = spawn.getCurrentStandardOutContents();
            
            for (int i = 0; i < patterns.length; i++) {
                try {
                    spawn.expect(patterns[i]);
                    String matched = spawn.getCurrentStandardOutContents();
                    allOutput.append(matched);
                    
                    return new ExpectResult(i, patterns[i], matched, beforeMatch);
                } catch (TimeoutException e) {
                    // Try next pattern
                    continue;
                }
            }
            
            throw new ExpectException("None of the patterns matched");
            
        } catch (IOException e) {
            throw new ExpectException("Failed to match patterns", e);
        }
    }
    
    @Override
    public void send(String input) throws ExpectException {
        if (spawn == null) {
            throw new ExpectException("No process spawned");
        }
        
        try {
            if (verbose) {
                System.out.println("[SEND] " + input);
            }
            spawn.send(input);
        } catch (IOException e) {
            throw new ExpectException("Failed to send input: " + input, e);
        }
    }
    
    @Override
    public void sendLine(String input) throws ExpectException {
        send(input + "\n");
    }
    
    @Override
    public void sendControl(char key) throws ExpectException {
        if (spawn == null) {
            throw new ExpectException("No process spawned");
        }
        
        try {
            char controlChar = (char) (key - 'A' + 1);
            if (verbose) {
                System.out.println("[SEND] Ctrl+" + key);
            }
            spawn.send(String.valueOf(controlChar));
        } catch (IOException e) {
            throw new ExpectException("Failed to send control character: Ctrl+" + key, e);
        }
    }
    
    @Override
    public int waitFor() throws ExpectException {
        if (spawn == null) {
            throw new ExpectException("No process spawned");
        }
        
        try {
            spawn.expectClose();
            return spawn.getExitValue();
        } catch (Exception e) {
            throw new ExpectException("Failed to wait for process", e);
        }
    }
    
    @Override
    public int waitFor(long timeout, TimeUnit unit) throws ExpectException {
        if (spawn == null) {
            throw new ExpectException("No process spawned");
        }
        
        try {
            long oldTimeout = spawn.getTimeout();
            spawn.setTimeout(unit.toSeconds(timeout));
            spawn.expectClose();
            spawn.setTimeout(oldTimeout);
            return spawn.getExitValue();
        } catch (Exception e) {
            throw new ExpectException("Process did not complete within timeout", e);
        }
    }
    
    @Override
    public boolean isAlive() {
        return spawn != null && spawn.isAlive();
    }
    
    @Override
    public String getOutput() {
        if (spawn == null) {
            return "";
        }
        return spawn.getCurrentStandardOutContents();
    }
    
    @Override
    public String getAllOutput() {
        return allOutput.toString();
    }
    
    @Override
    public void clearBuffer() {
        if (spawn != null) {
            spawn.clearBuffer();
        }
        allOutput.setLength(0);
    }
    
    @Override
    public void setDefaultTimeout(long timeout, TimeUnit unit) {
        this.defaultTimeoutSeconds = unit.toSeconds(timeout);
        if (expectJ != null) {
            expectJ.setDefaultTimeout(defaultTimeoutSeconds);
        }
    }
    
    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    @Override
    public void terminate() {
        if (spawn != null && spawn.isAlive()) {
            try {
                spawn.stop();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    @Override
    public void kill() {
        terminate();
        spawn = null;
    }
    
    @Override
    public void close() {
        kill();
    }
}