package com.orchestrix.automation.expectj.api;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ExpectJ Client interface for interactive command automation
 */
public interface ExpectClient extends AutoCloseable {
    
    /**
     * Start a process
     */
    void spawn(String command) throws ExpectException;
    
    /**
     * Start a process with environment variables
     */
    void spawn(String command, String[] env) throws ExpectException;
    
    /**
     * Expect a pattern and return matched content
     */
    String expect(String pattern) throws ExpectException;
    
    /**
     * Expect a pattern with timeout
     */
    String expect(String pattern, long timeout, TimeUnit unit) throws ExpectException;
    
    /**
     * Expect multiple patterns and return which one matched
     */
    ExpectResult expectMultiple(String... patterns) throws ExpectException;
    
    /**
     * Send input to the process
     */
    void send(String input) throws ExpectException;
    
    /**
     * Send input followed by newline
     */
    void sendLine(String input) throws ExpectException;
    
    /**
     * Send special key (e.g., Ctrl+C)
     */
    void sendControl(char key) throws ExpectException;
    
    /**
     * Wait for process to complete
     */
    int waitFor() throws ExpectException;
    
    /**
     * Wait for process with timeout
     */
    int waitFor(long timeout, TimeUnit unit) throws ExpectException;
    
    /**
     * Check if process is still running
     */
    boolean isAlive();
    
    /**
     * Get all output since last expect
     */
    String getOutput();
    
    /**
     * Get all accumulated output
     */
    String getAllOutput();
    
    /**
     * Clear output buffer
     */
    void clearBuffer();
    
    /**
     * Set default timeout
     */
    void setDefaultTimeout(long timeout, TimeUnit unit);
    
    /**
     * Enable/disable verbose logging
     */
    void setVerbose(boolean verbose);
    
    /**
     * Terminate the process
     */
    void terminate();
    
    /**
     * Force kill the process
     */
    void kill();
    
    /**
     * Result of expecting multiple patterns
     */
    class ExpectResult {
        private final int matchedIndex;
        private final String matchedPattern;
        private final String matchedText;
        private final String beforeMatch;
        
        public ExpectResult(int matchedIndex, String matchedPattern, String matchedText, String beforeMatch) {
            this.matchedIndex = matchedIndex;
            this.matchedPattern = matchedPattern;
            this.matchedText = matchedText;
            this.beforeMatch = beforeMatch;
        }
        
        public int getMatchedIndex() { return matchedIndex; }
        public String getMatchedPattern() { return matchedPattern; }
        public String getMatchedText() { return matchedText; }
        public String getBeforeMatch() { return beforeMatch; }
        public boolean matched(String pattern) { return pattern.equals(matchedPattern); }
    }
    
    /**
     * Expect Exception
     */
    class ExpectException extends Exception {
        public ExpectException(String message) { super(message); }
        public ExpectException(String message, Throwable cause) { super(message, cause); }
    }
}