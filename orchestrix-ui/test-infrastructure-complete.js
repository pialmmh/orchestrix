const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

async function testInfrastructure() {
  console.log('='.repeat(80));
  console.log('Starting comprehensive infrastructure test');
  console.log('='.repeat(80));

  const browser = await chromium.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });

  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 }
  });

  const page = await context.newPage();

  // Log console messages
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.log(`[Browser Error] ${msg.text()}`);
    }
  });

  // Log network requests
  page.on('request', request => {
    if (request.url().includes('/api/') || request.url().includes('ws://')) {
      console.log(`[Network] ${request.method()} ${request.url()}`);
    }
  });

  page.on('response', response => {
    if (response.url().includes('/api/')) {
      console.log(`[Response] ${response.status()} ${response.url()}`);
    }
  });

  try {
    console.log('\n1. Navigating to infrastructure page...');
    await page.goto('http://localhost:3026/infrastructure', {
      waitUntil: 'networkidle',
      timeout: 30000
    });

    // Wait for initial load
    await page.waitForTimeout(3000);

    console.log('\n2. Checking page content...');
    const pageTitle = await page.title();
    console.log(`   Page title: ${pageTitle}`);

    // Check for infrastructure elements
    const hasInfraHeader = await page.locator('text=Infrastructure').first().isVisible();
    console.log(`   Infrastructure header visible: ${hasInfraHeader}`);

    // Check for partner tabs
    const partnerTabs = await page.locator('.ant-tabs-tab').count();
    console.log(`   Partner tabs found: ${partnerTabs}`);

    // Check for tree structure
    const treeNodes = await page.locator('.ant-tree-node-content-wrapper').count();
    console.log(`   Tree nodes found: ${treeNodes}`);

    // Check for Telcobright partner
    const hasTelcobright = await page.locator('text=Telcobright').isVisible();
    console.log(`   Telcobright partner visible: ${hasTelcobright}`);

    // Take screenshot
    await page.screenshot({
      path: 'infrastructure-test-screenshot.png',
      fullPage: true
    });
    console.log('   Screenshot saved: infrastructure-test-screenshot.png');

    // Extract visible text content
    const textContent = await page.evaluate(() => document.body.innerText);
    fs.writeFileSync('infrastructure-page-text.txt', textContent);
    console.log('   Page text saved: infrastructure-page-text.txt');

    console.log('\n3. Checking backend store files...');
    const storeDebugPath = path.join(__dirname, 'store-server', 'store-debug', 'infrastructure');

    if (fs.existsSync(storeDebugPath)) {
      console.log(`   Store debug directory exists: ${storeDebugPath}`);

      // Check current state file
      const currentStatePath = path.join(storeDebugPath, 'current.json');
      if (fs.existsSync(currentStatePath)) {
        const currentState = JSON.parse(fs.readFileSync(currentStatePath, 'utf8'));
        console.log('   ✓ current.json exists');
        console.log(`     - Queries: ${Object.keys(currentState.queries || {}).length}`);
        console.log(`     - Subscriptions: ${Object.keys(currentState.subscriptions || {}).length}`);
        console.log(`     - Cache entries: ${Object.keys(currentState.cache || {}).length}`);

        // Check for specific data
        if (currentState.queries) {
          const queryKeys = Object.keys(currentState.queries);
          console.log(`     - Query types: ${queryKeys.join(', ')}`);

          // Check for partner data
          const partnerQuery = queryKeys.find(k => k.includes('partner'));
          if (partnerQuery) {
            const partnerData = currentState.queries[partnerQuery];
            if (partnerData?.data?.result) {
              console.log(`     - Partners found: ${partnerData.data.result.length}`);
              if (partnerData.data.result.length > 0) {
                console.log(`     - First partner: ${partnerData.data.result[0].Name}`);
              }
            }
          }

          // Check for environment data
          const envQuery = queryKeys.find(k => k.includes('environment'));
          if (envQuery) {
            const envData = currentState.queries[envQuery];
            if (envData?.data?.result) {
              console.log(`     - Environments found: ${envData.data.result.length}`);
            }
          }
        }
      } else {
        console.log('   ✗ current.json not found');
      }

      // Check history file
      const historyPath = path.join(storeDebugPath, 'history.jsonl');
      if (fs.existsSync(historyPath)) {
        const historyLines = fs.readFileSync(historyPath, 'utf8').trim().split('\n').filter(l => l);
        console.log(`   ✓ history.jsonl exists (${historyLines.length} events)`);

        // Show last few events
        const recentEvents = historyLines.slice(-5).map(line => {
          const event = JSON.parse(line);
          return `     - ${event.timestamp}: ${event.event} (${event.changes ? event.changes.length + ' changes' : 'initialized'})`;
        });
        console.log('   Recent events:');
        recentEvents.forEach(e => console.log(e));
      } else {
        console.log('   ✗ history.jsonl not found');
      }

      // List all files in store-debug directory
      const debugFiles = fs.readdirSync(storeDebugPath);
      console.log(`\n   Files in store-debug/infrastructure: ${debugFiles.join(', ')}`);

    } else {
      console.log(`   ✗ Store debug directory not found: ${storeDebugPath}`);
    }

    console.log('\n4. Waiting for data to load...');
    await page.waitForTimeout(5000);

    // Re-check tree nodes after wait
    const treeNodesAfterWait = await page.locator('.ant-tree-node-content-wrapper').count();
    console.log(`   Tree nodes after wait: ${treeNodesAfterWait}`);

    // Check for any error messages
    const errorMessages = await page.locator('.ant-message-error').count();
    console.log(`   Error messages: ${errorMessages}`);

    // Check WebSocket connection
    const wsConnected = await page.evaluate(() => {
      return window.WebSocketManager && window.WebSocketManager.isConnected;
    });
    console.log(`   WebSocket connected: ${wsConnected || 'Unable to determine'}`);

    console.log('\n5. Test Summary:');
    console.log(`   ✓ Page loaded successfully`);
    console.log(`   ${hasInfraHeader ? '✓' : '✗'} Infrastructure header visible`);
    console.log(`   ${hasTelcobright ? '✓' : '✗'} Telcobright partner visible`);
    console.log(`   ${treeNodesAfterWait > 0 ? '✓' : '✗'} Tree nodes rendered (${treeNodesAfterWait} found)`);
    console.log(`   ${fs.existsSync(path.join(storeDebugPath, 'current.json')) ? '✓' : '✗'} Backend store current state saved`);
    console.log(`   ${fs.existsSync(path.join(storeDebugPath, 'history.jsonl')) ? '✓' : '✗'} Backend store history saved`);

  } catch (error) {
    console.error('Test failed:', error);
  } finally {
    await browser.close();
    console.log('\n' + '='.repeat(80));
    console.log('Test completed');
    console.log('='.repeat(80));
  }
}

testInfrastructure().catch(console.error);