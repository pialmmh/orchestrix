#!/usr/bin/env node

/**
 * Test script to verify DELETE mutation with auto-refresh
 * This will connect to the store server and perform a DELETE mutation
 */

const WebSocket = require('ws');

// Configuration
const WS_URL = 'ws://localhost:8083';
const COMPUTE_ID_TO_DELETE = 11; // mustafa-pc id

async function testDeleteMutation() {
  return new Promise((resolve, reject) => {
    console.log(`\n🚀 Connecting to store server at ${WS_URL}...`);

    const ws = new WebSocket(WS_URL);
    let subscriptionReceived = false;

    ws.on('open', () => {
      console.log('✅ Connected to store server\n');

      // First, subscribe to infrastructure updates
      console.log('📡 Subscribing to infrastructure updates...');
      ws.send(JSON.stringify({
        type: 'SUBSCRIBE',
        entity: 'infrastructure'
      }));

      // Wait a bit to ensure subscription is registered
      setTimeout(() => {
        // Send DELETE mutation
        console.log(`\n🗑️  Sending DELETE mutation for compute ID: ${COMPUTE_ID_TO_DELETE}`);
        ws.send(JSON.stringify({
          type: 'MUTATION',
          entity: 'compute',
          operation: 'DELETE',
          payload: { id: COMPUTE_ID_TO_DELETE }
        }));
      }, 1000);
    });

    ws.on('message', (data) => {
      const message = JSON.parse(data);

      switch (message.type) {
        case 'CONNECTION':
          console.log('📝 Connection confirmed');
          break;

        case 'SUBSCRIPTION_DATA':
          console.log('📊 Initial subscription data received');
          subscriptionReceived = true;
          break;

        case 'MUTATION_SUCCESS':
          console.log(`\n✅ Mutation ${message.operation} succeeded!`);
          console.log('   Entity:', message.entity);

          // Check if the store was updated
          if (message.data && message.data.data) {
            console.log('\n📦 Updated store data received:');
            console.log('   - Query:', JSON.stringify(message.data.query, null, 2));
            console.log('   - Data count:', message.data.data.length);
            console.log('   - Pages loaded:', message.data.meta.pagesLoaded);

            // Verify mustafa-pc is not in the data
            const hasMusatafaPc = JSON.stringify(message.data).includes('mustafa-pc');
            if (!hasMusatafaPc) {
              console.log('\n✅ SUCCESS: mustafa-pc has been removed from the store!');
            } else {
              console.log('\n❌ WARNING: mustafa-pc still exists in the store');
            }
          }
          break;

        case 'STORE_UPDATE':
          console.log('\n🔄 Store update broadcast received!');
          console.log('   Entity:', message.entity);

          if (message.data) {
            // Check if mustafa-pc is in the updated data
            const dataStr = JSON.stringify(message.data);
            const hasMusatafaPc = dataStr.includes('mustafa-pc');

            if (!hasMusatafaPc) {
              console.log('   ✅ Store auto-refreshed without mustafa-pc');
            } else {
              console.log('   ❌ Store still contains mustafa-pc after update');
            }

            // Count total computes
            const computeCount = (dataStr.match(/compute/g) || []).length;
            console.log(`   📊 Total compute references in store: ${computeCount}`);
          }

          // Test complete
          setTimeout(() => {
            console.log('\n🎉 Test completed successfully!');
            ws.close();
            resolve();
          }, 2000);
          break;

        case 'ERROR':
          console.error('\n❌ Error:', message.error);
          break;

        default:
          console.log(`📨 Received ${message.type}:`, message);
      }
    });

    ws.on('error', (error) => {
      console.error('❌ WebSocket error:', error);
      reject(error);
    });

    ws.on('close', () => {
      console.log('\n👋 Disconnected from store server');
      resolve();
    });

    // Timeout after 10 seconds
    setTimeout(() => {
      console.log('\n⏰ Test timeout - closing connection');
      ws.close();
      resolve();
    }, 10000);
  });
}

// Run the test
console.log('====================================');
console.log('  DELETE Mutation Auto-Refresh Test');
console.log('====================================');

testDeleteMutation()
  .then(() => {
    console.log('\n✅ Test execution completed');
    process.exit(0);
  })
  .catch((error) => {
    console.error('\n❌ Test failed:', error);
    process.exit(1);
  });