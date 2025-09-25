package com.telcobright.orchestrix.model;

/**
 * MySQL configuration - highly reusable across orchestrix
 */
public class MySqlConfig {
    private String host;
    private int port = 3306;
    private String username;
    private String password;
    private String database;
    private String rootPassword;
    private String characterSet = "utf8mb4";
    private String collation = "utf8mb4_unicode_ci";
    private Integer maxConnections = 100;
    private Long maxAllowedPacket = 64L * 1024 * 1024; // 64MB
    private boolean enableBinlog = false;

    // Constructors
    public MySqlConfig() {}

    public MySqlConfig(String host, String username, String password, String database) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.database = database;
    }

    // Connection string builder
    public String getConnectionUrl() {
        return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=%s",
            host, port, database != null ? database : "", characterSet);
    }

    public String getMyCnfContent() {
        StringBuilder cnf = new StringBuilder();
        cnf.append("[mysqld]\n");
        cnf.append("character-set-server=").append(characterSet).append("\n");
        cnf.append("collation-server=").append(collation).append("\n");
        cnf.append("max_connections=").append(maxConnections).append("\n");
        cnf.append("max_allowed_packet=").append(maxAllowedPacket).append("\n");
        if (enableBinlog) {
            cnf.append("log_bin=mysql-bin\n");
            cnf.append("binlog_format=ROW\n");
        }
        return cnf.toString();
    }

    // Getters and Setters
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String getRootPassword() { return rootPassword; }
    public void setRootPassword(String rootPassword) { this.rootPassword = rootPassword; }

    public String getCharacterSet() { return characterSet; }
    public void setCharacterSet(String characterSet) { this.characterSet = characterSet; }

    public String getCollation() { return collation; }
    public void setCollation(String collation) { this.collation = collation; }

    public Integer getMaxConnections() { return maxConnections; }
    public void setMaxConnections(Integer maxConnections) { this.maxConnections = maxConnections; }

    public Long getMaxAllowedPacket() { return maxAllowedPacket; }
    public void setMaxAllowedPacket(Long maxAllowedPacket) { this.maxAllowedPacket = maxAllowedPacket; }

    public boolean isEnableBinlog() { return enableBinlog; }
    public void setEnableBinlog(boolean enableBinlog) { this.enableBinlog = enableBinlog; }
}