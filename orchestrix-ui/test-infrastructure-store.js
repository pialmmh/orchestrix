const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

(async () => {
  let browser;
  let context;
  let page;

  try {
    console.log('🚀 Starting Playwright test for infrastructure store...');

    // Launch browser
    browser = await chromium.launch({
      headless: false,
      slowMo: 500
    });

    context = await browser.newContext();
    page = await context.newPage();

    // Add console logging
    page.on('console', msg => {
      if (msg.type() === 'error') {
        console.error('Browser console error:', msg.text());
      }
    });

    page.on('pageerror', err => {
      console.error('Page error:', err.message);
    });

    // Navigate to the infrastructure page
    console.log('📍 Navigating to http://localhost:3026/infrastructure');
    await page.goto('http://localhost:3026/infrastructure', {
      waitUntil: 'networkidle'
    });

    // Wait for the page to load
    console.log('⏳ Waiting for infrastructure page to load...');
    await page.waitForTimeout(5000);

    // Check for loading state
    const loadingElement = await page.$('text=Loading infrastructure data...');
    if (loadingElement) {
      console.log('⏳ Infrastructure data is loading...');
      // Wait for loading to complete
      await page.waitForSelector('text=Loading infrastructure data...', {
        state: 'hidden',
        timeout: 30000
      }).catch(() => console.log('Loading indicator did not disappear'));
    }

    // Take screenshot
    await page.screenshot({ path: 'infrastructure-page.png', fullPage: true });
    console.log('📸 Screenshot saved as infrastructure-page.png');

    // Check if tree data loaded
    console.log('🔍 Checking for tree data...');

    // Look for tree nodes or data elements
    const treeNodes = await page.$$('.tree-node, [class*="tree"], [class*="node"]');
    console.log(`Found ${treeNodes.length} potential tree elements`);

    // Check for any visible data
    const visibleText = await page.evaluate(() => {
      return document.body.innerText;
    });

    // Save the page content
    fs.writeFileSync('infrastructure-page-content.txt', visibleText);
    console.log('📝 Page content saved to infrastructure-page-content.txt');

    // Now check the backend store data
    console.log('\n📂 Checking backend store data...');

    // Read the current store state
    const storeDebugPath = path.join(__dirname, 'store-server', 'store-debug', 'infrastructure');

    if (fs.existsSync(storeDebugPath)) {
      // Read current state
      const currentStatePath = path.join(storeDebugPath, 'current.json');
      if (fs.existsSync(currentStatePath)) {
        const currentState = JSON.parse(fs.readFileSync(currentStatePath, 'utf8'));
        console.log('\n✅ Backend store current state found:');
        console.log(`  - Timestamp: ${new Date(currentState.timestamp).toISOString()}`);
        console.log(`  - Has state: ${currentState.state !== null}`);

        // Check store contents
        if (currentState.state) {
          console.log(`  - Tree data items: ${currentState.state.treeData?.length || 0}`);
          console.log(`  - Partners: ${currentState.state.partners?.length || 0}`);
          console.log(`  - Environments: ${currentState.state.environments?.length || 0}`);
          console.log(`  - Loading: ${currentState.state.loading}`);
          console.log(`  - Error: ${currentState.state.error || 'none'}`);
        }

        // Save store state to file
        fs.writeFileSync('backend-store-state.json', JSON.stringify(currentState, null, 2));
        console.log('\n📝 Backend store state saved to backend-store-state.json');
      }

      // Read history
      const historyPath = path.join(storeDebugPath, 'history.jsonl');
      if (fs.existsSync(historyPath)) {
        const historyLines = fs.readFileSync(historyPath, 'utf8').trim().split('\n');
        console.log(`\n📜 Store history: ${historyLines.length} events`);

        // Parse last few events
        const recentEvents = historyLines.slice(-5).map(line => {
          try {
            const event = JSON.parse(line);
            return `  - ${new Date(event.timestamp).toISOString()}: ${event.event}`;
          } catch (e) {
            return null;
          }
        }).filter(Boolean);

        if (recentEvents.length > 0) {
          console.log('Recent events:');
          recentEvents.forEach(e => console.log(e));
        }
      }
    } else {
      console.log('❌ Backend store debug directory not found');
    }

    // Check for errors on the page
    const errorElements = await page.$$('text=/error/i');
    if (errorElements.length > 0) {
      console.log(`⚠️ Found ${errorElements.length} error messages on the page`);
    }

    console.log('\n✅ Test completed successfully');

  } catch (error) {
    console.error('❌ Test failed:', error);

    // Take error screenshot
    if (page) {
      await page.screenshot({ path: 'infrastructure-error.png', fullPage: true });
      console.log('📸 Error screenshot saved as infrastructure-error.png');
    }

  } finally {
    // Close browser
    if (browser) {
      await browser.close();
    }
  }
})();