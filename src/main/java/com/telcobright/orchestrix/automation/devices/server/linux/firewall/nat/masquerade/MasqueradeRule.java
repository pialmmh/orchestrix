package com.telcobright.orchestrix.automation.devices.server.linux.firewall.nat.masquerade;

public class MasqueradeRule {
    private String sourceIp;
    private String sourceInterface;
    private String outInterface;
    private String destinationIp;
    private String protocol;
    private String portRange;
    private boolean persistent;
    private String comment;

    private MasqueradeRule(Builder builder) {
        this.sourceIp = builder.sourceIp;
        this.sourceInterface = builder.sourceInterface;
        this.outInterface = builder.outInterface;
        this.destinationIp = builder.destinationIp;
        this.protocol = builder.protocol;
        this.portRange = builder.portRange;
        this.persistent = builder.persistent;
        this.comment = builder.comment;
    }

    public String getSourceIp() { return sourceIp; }
    public String getSourceInterface() { return sourceInterface; }
    public String getOutInterface() { return outInterface; }
    public String getDestinationIp() { return destinationIp; }
    public String getProtocol() { return protocol; }
    public String getPortRange() { return portRange; }
    public boolean isPersistent() { return persistent; }
    public String getComment() { return comment; }

    public String toIptablesRule() {
        StringBuilder rule = new StringBuilder("iptables -t nat -A POSTROUTING");

        if (sourceIp != null && !sourceIp.isEmpty()) {
            rule.append(" -s ").append(sourceIp);
        }
        if (sourceInterface != null && !sourceInterface.isEmpty()) {
            rule.append(" -i ").append(sourceInterface);
        }
        if (outInterface != null && !outInterface.isEmpty()) {
            rule.append(" -o ").append(outInterface);
        }
        if (destinationIp != null && !destinationIp.isEmpty()) {
            rule.append(" -d ").append(destinationIp);
        }
        if (protocol != null && !protocol.isEmpty()) {
            rule.append(" -p ").append(protocol);
        }
        if (portRange != null && !portRange.isEmpty()) {
            rule.append(" --dport ").append(portRange);
        }
        if (comment != null && !comment.isEmpty()) {
            rule.append(" -m comment --comment \"").append(comment).append("\"");
        }

        rule.append(" -j MASQUERADE");
        return rule.toString();
    }

    public static class Builder {
        private String sourceIp;
        private String sourceInterface;
        private String outInterface;
        private String destinationIp;
        private String protocol;
        private String portRange;
        private boolean persistent = false;
        private String comment;

        public Builder sourceIp(String sourceIp) {
            this.sourceIp = sourceIp;
            return this;
        }

        public Builder sourceInterface(String sourceInterface) {
            this.sourceInterface = sourceInterface;
            return this;
        }

        public Builder outInterface(String outInterface) {
            this.outInterface = outInterface;
            return this;
        }

        public Builder destinationIp(String destinationIp) {
            this.destinationIp = destinationIp;
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder portRange(String portRange) {
            this.portRange = portRange;
            return this;
        }

        public Builder persistent(boolean persistent) {
            this.persistent = persistent;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public MasqueradeRule build() {
            return new MasqueradeRule(this);
        }
    }

    @Override
    public String toString() {
        return toIptablesRule();
    }
}