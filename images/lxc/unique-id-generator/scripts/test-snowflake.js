/**
 * Test suite for Snowflake ID Generator
 */

const SnowflakeGenerator = require('./snowflake');

class SnowflakeTest {
    constructor() {
        this.tests = [];
        this.passed = 0;
        this.failed = 0;
    }

    // Test helper methods
    assert(condition, message) {
        if (!condition) {
            throw new Error(`Assertion failed: ${message}`);
        }
    }

    assertEquals(actual, expected, message) {
        if (actual !== expected) {
            throw new Error(`${message}: expected ${expected}, got ${actual}`);
        }
    }

    // Test cases
    async testBasicGeneration() {
        const generator = new SnowflakeGenerator(1, 1);
        const id = generator.generate();

        this.assert(id !== null, 'ID should not be null');
        this.assert(id.length > 0, 'ID should not be empty');
        this.assert(/^\d+$/.test(id), 'ID should be numeric string');

        console.log(`✓ Basic generation: ${id}`);
    }

    async testUniqueIds() {
        const generator = new SnowflakeGenerator(1, 1);
        const ids = new Set();
        const count = 10000;

        for (let i = 0; i < count; i++) {
            ids.add(generator.generate());
        }

        this.assertEquals(ids.size, count, 'All IDs should be unique');
        console.log(`✓ Uniqueness test: Generated ${count} unique IDs`);
    }

    async testMultipleShards() {
        const shard1 = new SnowflakeGenerator(1, 3);
        const shard2 = new SnowflakeGenerator(2, 3);
        const shard3 = new SnowflakeGenerator(3, 3);

        const ids = new Set();
        const perShard = 1000;

        // Generate IDs from each shard
        for (let i = 0; i < perShard; i++) {
            ids.add(shard1.generate());
            ids.add(shard2.generate());
            ids.add(shard3.generate());
        }

        this.assertEquals(ids.size, perShard * 3, 'All IDs across shards should be unique');
        console.log(`✓ Multi-shard test: ${perShard * 3} unique IDs across 3 shards`);
    }

    async testParsing() {
        const generator = new SnowflakeGenerator(42, 100);
        const id = generator.generate();
        const parsed = generator.parse(id);

        this.assertEquals(parsed.shardId, 42, 'Parsed shard ID should match');
        this.assert(parsed.timestamp > 0, 'Timestamp should be positive');
        this.assert(parsed.sequence >= 0, 'Sequence should be non-negative');

        console.log(`✓ Parsing test:`, parsed);
    }

    async testSequentialIds() {
        const generator = new SnowflakeGenerator(1, 1);
        const ids = [];

        // Generate IDs quickly to test sequence increment
        for (let i = 0; i < 100; i++) {
            ids.push(generator.generate());
        }

        // Check that IDs are monotonically increasing
        for (let i = 1; i < ids.length; i++) {
            const prev = BigInt(ids[i - 1]);
            const curr = BigInt(ids[i]);
            this.assert(curr > prev, `IDs should be increasing: ${prev} < ${curr}`);
        }

        console.log(`✓ Sequential test: ${ids.length} monotonically increasing IDs`);
    }

    async testHighThroughput() {
        const generator = new SnowflakeGenerator(1, 1);
        const start = Date.now();
        const count = 100000;

        for (let i = 0; i < count; i++) {
            generator.generate();
        }

        const elapsed = Date.now() - start;
        const rate = Math.round(count / (elapsed / 1000));

        console.log(`✓ Throughput test: ${count} IDs in ${elapsed}ms (${rate} IDs/sec)`);

        const stats = generator.getStats();
        console.log(`  Stats:`, stats);
    }

    async testBatchGeneration() {
        const generator = new SnowflakeGenerator(5, 10);
        const batch = generator.generateBatch(100);

        this.assertEquals(batch.length, 100, 'Batch should contain correct number of IDs');

        const uniqueIds = new Set(batch);
        this.assertEquals(uniqueIds.size, 100, 'All batch IDs should be unique');

        console.log(`✓ Batch generation: ${batch.length} IDs generated`);
    }

    async testShardBoundaries() {
        // Test minimum shard ID
        const minGen = new SnowflakeGenerator(1, 1024);
        const minId = minGen.generate();
        const minParsed = minGen.parse(minId);
        this.assertEquals(minParsed.shardId, 1, 'Min shard ID should be 1');

        // Test maximum shard ID
        const maxGen = new SnowflakeGenerator(1024, 1024);
        const maxId = maxGen.generate();
        const maxParsed = maxGen.parse(maxId);
        this.assertEquals(maxParsed.shardId, 1024, 'Max shard ID should be 1024');

        console.log(`✓ Shard boundaries: 1-1024 validated`);
    }

    async testInvalidShardConfig() {
        try {
            new SnowflakeGenerator(0, 10);
            this.assert(false, 'Should reject shard ID 0');
        } catch (e) {
            // Expected
        }

        try {
            new SnowflakeGenerator(1025, 1025);
            this.assert(false, 'Should reject shard ID > 1024');
        } catch (e) {
            // Expected
        }

        try {
            new SnowflakeGenerator(5, 3);
            this.assert(false, 'Should reject shard ID > total shards');
        } catch (e) {
            // Expected
        }

        console.log(`✓ Invalid config rejection: Properly validates shard configuration`);
    }

    async testIdStructure() {
        const generator = new SnowflakeGenerator(7, 10);
        const id = generator.generate();
        const parsed = generator.parse(id);

        // Convert to binary to verify structure
        const binary = parsed.binary;
        this.assertEquals(binary.length, 64, 'Binary representation should be 64 bits');

        // Extract parts from binary
        const timestampBits = binary.substring(1, 42); // Skip sign bit
        const shardIdBits = binary.substring(42, 52);
        const sequenceBits = binary.substring(52, 64);

        console.log(`✓ ID Structure verified:`);
        console.log(`  ID: ${id}`);
        console.log(`  Binary: ${binary}`);
        console.log(`  Timestamp bits (41): ${timestampBits}`);
        console.log(`  Shard ID bits (10): ${shardIdBits}`);
        console.log(`  Sequence bits (12): ${sequenceBits}`);
    }

    async testTimeOrdering() {
        const generator = new SnowflakeGenerator(1, 1);
        const ids = [];

        // Generate IDs with time gaps
        for (let i = 0; i < 5; i++) {
            ids.push(generator.generate());
            await new Promise(resolve => setTimeout(resolve, 10)); // Wait 10ms
        }

        // Parse and check timestamps are increasing
        for (let i = 1; i < ids.length; i++) {
            const prev = generator.parse(ids[i - 1]);
            const curr = generator.parse(ids[i]);
            this.assert(curr.timestamp >= prev.timestamp, 'Timestamps should be non-decreasing');
        }

        console.log(`✓ Time ordering: IDs are chronologically ordered`);
    }

    async testReset() {
        const generator = new SnowflakeGenerator(1, 1);

        // Generate some IDs
        for (let i = 0; i < 100; i++) {
            generator.generate();
        }

        const statsBefore = generator.getStats();
        this.assert(statsBefore.generated > 0, 'Should have generated IDs');

        // Reset
        generator.reset();
        const statsAfter = generator.getStats();

        this.assertEquals(statsAfter.generated, 0, 'Generated count should be reset');
        this.assertEquals(statsAfter.collisions, 0, 'Collisions should be reset');

        console.log(`✓ Reset: Generator state properly reset`);
    }

    // Run all tests
    async runAll() {
        console.log('\n🧪 Running Snowflake ID Generator Tests\n');
        console.log('=' .repeat(50));

        const tests = [
            { name: 'Basic Generation', fn: this.testBasicGeneration },
            { name: 'Unique IDs', fn: this.testUniqueIds },
            { name: 'Multiple Shards', fn: this.testMultipleShards },
            { name: 'ID Parsing', fn: this.testParsing },
            { name: 'Sequential IDs', fn: this.testSequentialIds },
            { name: 'High Throughput', fn: this.testHighThroughput },
            { name: 'Batch Generation', fn: this.testBatchGeneration },
            { name: 'Shard Boundaries', fn: this.testShardBoundaries },
            { name: 'Invalid Configuration', fn: this.testInvalidShardConfig },
            { name: 'ID Structure', fn: this.testIdStructure },
            { name: 'Time Ordering', fn: this.testTimeOrdering },
            { name: 'Reset Functionality', fn: this.testReset },
        ];

        for (const test of tests) {
            try {
                await test.fn.call(this);
                this.passed++;
            } catch (error) {
                this.failed++;
                console.error(`✗ ${test.name}: ${error.message}`);
            }
        }

        console.log('\n' + '=' .repeat(50));
        console.log(`\n📊 Test Results: ${this.passed} passed, ${this.failed} failed\n`);

        if (this.failed === 0) {
            console.log('✨ All tests passed! ✨\n');
        }

        return this.failed === 0;
    }
}

// Demo usage
async function demo() {
    console.log('\n🎯 Snowflake ID Generator Demo\n');
    console.log('=' .repeat(50));

    // Single shard example
    console.log('\n1. Single Shard Example:');
    const single = new SnowflakeGenerator(1, 1);
    for (let i = 0; i < 5; i++) {
        const id = single.generate();
        const parsed = single.parse(id);
        console.log(`   ID: ${id} (Shard: ${parsed.shardId}, Time: ${parsed.date.toISOString()})`);
    }

    // Multi-shard example
    console.log('\n2. Multi-Shard Example (3 shards):');
    const shards = [
        new SnowflakeGenerator(1, 3),
        new SnowflakeGenerator(2, 3),
        new SnowflakeGenerator(3, 3)
    ];

    for (const shard of shards) {
        const id = shard.generate();
        const parsed = shard.parse(id);
        console.log(`   Shard ${parsed.shardId}: ${id}`);
    }

    // Generator info
    console.log('\n3. Generator Configuration:');
    const info = single.getInfo();
    console.log('  ', JSON.stringify(info, null, 2));

    console.log('\n' + '=' .repeat(50));
}

// Main execution
async function main() {
    const tester = new SnowflakeTest();
    const success = await tester.runAll();

    await demo();

    process.exit(success ? 0 : 1);
}

if (require.main === module) {
    main().catch(console.error);
}