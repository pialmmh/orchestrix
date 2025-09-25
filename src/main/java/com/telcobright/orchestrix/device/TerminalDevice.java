package com.telcobright.orchestrix.device;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface TerminalDevice {
    
    CompletableFuture<Boolean> connect(String hostname, int port, String username, String password) throws IOException;
    
    CompletableFuture<String> send(String command) throws IOException;
    
    CompletableFuture<String> receive() throws IOException;
    
    CompletableFuture<String> sendAndReceive(String command) throws IOException;
    
    boolean isConnected();
    
    void disconnect() throws IOException;
    
    String getDeviceId();
    
    String getHostname();
    
    DeviceType getDeviceType();
    
    DeviceStatus getStatus();
    
    enum DeviceType {
        SSH, TELNET, SERIAL, LOCAL_SHELL
    }
    
    enum DeviceStatus {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR, TIMEOUT
    }
}