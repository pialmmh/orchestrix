const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext();
  const page = await context.newPage();

  // Capture console logs
  page.on('console', msg => {
    const text = msg.text();
    if (text.includes('WebSocket') || text.includes('Store') || text.includes('partner') || text.includes('infrastructure')) {
      console.log(`[Browser Console]: ${text}`);
    }
  });

  // Capture network activity
  page.on('request', request => {
    if (request.url().includes('partner') || request.url().includes('infrastructure')) {
      console.log(`[Request]: ${request.method()} ${request.url()}`);
    }
  });

  page.on('response', response => {
    if (response.url().includes('partner') || response.url().includes('infrastructure')) {
      console.log(`[Response]: ${response.status()} ${response.url()}`);
    }
  });

  // Capture WebSocket activity
  page.on('websocket', ws => {
    console.log(`[WebSocket]: Connected to ${ws.url()}`);

    ws.on('framesent', ({ payload }) => {
      try {
        const data = JSON.parse(payload);
        if (data.type === 'query' || data.entity === 'partner') {
          console.log('[WebSocket Send]:', JSON.stringify(data).substring(0, 200));
        }
      } catch (e) {}
    });

    ws.on('framereceived', ({ payload }) => {
      try {
        const data = JSON.parse(payload);
        if (data.type === 'QUERY_RESULT' || data.entity === 'partner') {
          console.log('[WebSocket Receive]:', JSON.stringify(data).substring(0, 200));
        }
      } catch (e) {}
    });
  });

  try {
    console.log('Navigating to infrastructure page...');
    await page.goto('http://localhost:3010/infrastructure/organization', { waitUntil: 'networkidle' });

    // Wait for potential data loading
    await page.waitForTimeout(5000);

    // Check page content
    const pageContent = await page.content();
    const hasLoadingText = pageContent.includes('Loading infrastructure data');
    const hasTelcobright = pageContent.includes('Telcobright') || pageContent.includes('telcobright');
    const hasPartnerData = pageContent.includes('Amazon Web Services') || pageContent.includes('Google Cloud');

    console.log('\n=== Page Status ===');
    console.log(`Still showing "Loading...": ${hasLoadingText}`);
    console.log(`Has Telcobright partner: ${hasTelcobright}`);
    console.log(`Has partner data: ${hasPartnerData}`);

    // Check for tree nodes
    const treeNodes = await page.$$('.tree-node, .ant-tree-node, [role="treeitem"]');
    console.log(`Found ${treeNodes.length} tree nodes`);

    // Take screenshot
    await page.screenshot({ path: 'infra-debug.png', fullPage: true });
    console.log('Screenshot saved as infra-debug.png');

    // Keep browser open for 5 more seconds to capture any delayed responses
    await page.waitForTimeout(5000);

  } catch (error) {
    console.error('Test failed:', error);
  } finally {
    await browser.close();
  }
})();
