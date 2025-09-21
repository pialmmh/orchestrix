const { chromium } = require('playwright');

async function testInfraLoad() {
  console.log('Starting infrastructure load test...');

  const browser = await chromium.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-blink-features=AutomationControlled']
  });

  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
    // Bypass cache
    bypassCSP: true,
    ignoreHTTPSErrors: true,
    offline: false,
    httpCredentials: undefined,
    // Force no cache
    extraHTTPHeaders: {
      'Cache-Control': 'no-cache, no-store, must-revalidate',
      'Pragma': 'no-cache',
      'Expires': '0'
    }
  });

  const page = await context.newPage();

  // Log console messages
  let hasAxiosError = false;
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.log(`[Browser Error] ${msg.text()}`);
      if (msg.text().includes('axios')) {
        hasAxiosError = true;
      }
    } else if (msg.text().includes('WebSocket') || msg.text().includes('Store')) {
      console.log(`[Browser] ${msg.text()}`);
    }
  });

  // Also catch uncaught exceptions
  page.on('pageerror', error => {
    console.log(`[Page Error] ${error.message}`);
    if (error.message.includes('axios')) {
      hasAxiosError = true;
    }
  });

  try {
    console.log('Navigating to infrastructure page...');
    // Add timestamp to bypass cache and clear cache
    await page.evaluate(() => {
      if ('caches' in window) {
        caches.keys().then(names => {
          names.forEach(name => caches.delete(name));
        });
      }
    });

    const timestamp = Date.now();
    await page.goto(`http://localhost:3010/infrastructure?t=${timestamp}&nocache=true`, {
      waitUntil: 'networkidle',
      timeout: 30000
    });

    // Wait for page to load
    await page.waitForTimeout(3000);

    // Check for Telcobright partner
    const hasTelcobright = await page.locator('text=Telcobright').isVisible();
    console.log(`Telcobright partner visible: ${hasTelcobright}`);

    // Check for tree nodes
    const treeNodes = await page.locator('.ant-tree-node-content-wrapper').count();
    console.log(`Tree nodes found: ${treeNodes}`);

    // Take screenshot
    await page.screenshot({
      path: 'infra-load-test.png',
      fullPage: true
    });
    console.log('Screenshot saved: infra-load-test.png');

    // Wait a bit more to let data load
    await page.waitForTimeout(2000);

  } catch (error) {
    console.error('Test failed:', error);
  } finally {
    await browser.close();
    console.log('Browser closed');
  }
}

testInfraLoad().catch(console.error);