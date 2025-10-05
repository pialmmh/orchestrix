package com.telcobright.orchestrix.automation.api.container.lxc.app.goid.entities;

/**
 * Data types supported by Go ID Generator
 *
 * <p>Matches Node.js unique-id-generator API exactly
 */
public enum GoIdDataType {
    /**
     * 32-bit sequential integer with sharding support
     * Example: Shard 1 of 3 generates: 1, 4, 7, 10, 13...
     */
    INT("int"),

    /**
     * 64-bit sequential integer with sharding support
     * Example: Shard 2 of 3 generates: 2, 5, 8, 11, 14...
     */
    LONG("long"),

    /**
     * 64-bit Sonyflake distributed ID (time-ordered, globally unique)
     * Format: timestamp(39 bits) + sequence(8 bits) + machineID(16 bits)
     */
    SNOWFLAKE("snowflake"),

    /**
     * Random 8-character alphanumeric string (legacy, use SNOWFLAKE instead)
     */
    UUID8("uuid8"),

    /**
     * Random 12-character alphanumeric string (legacy, use SNOWFLAKE instead)
     */
    UUID12("uuid12"),

    /**
     * Random 16-character alphanumeric string (legacy, use SNOWFLAKE instead)
     */
    UUID16("uuid16"),

    /**
     * Random 22-character alphanumeric string (legacy, use SNOWFLAKE instead)
     */
    UUID22("uuid22");

    private final String value;

    GoIdDataType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
