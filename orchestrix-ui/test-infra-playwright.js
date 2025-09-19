const { chromium } = require('playwright');

(async () => {
  console.log('🎭 Starting Playwright test for infrastructure tree loading...');

  const browser = await chromium.launch({
    headless: false,
    slowMo: 500
  });

  const page = await browser.newPage();

  // Add console listener to see frontend logs
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.log('❌ Console error:', msg.text());
    } else if (msg.text().includes('EventBus') || msg.text().includes('WebSocket')) {
      console.log('📡', msg.text());
    }
  });

  // Navigate to infrastructure page
  console.log('🌐 Navigating to infrastructure page...');
  await page.goto('http://localhost:3010/infrastructure/organization');

  // Wait for page to load
  await page.waitForTimeout(3000);

  // Check for debug mode indicator
  const debugIndicator = await page.locator('.MuiAlert-standardWarning').first();
  if (await debugIndicator.isVisible()) {
    console.log('✅ Debug mode is active');
    const debugText = await debugIndicator.textContent();
    console.log('   Debug text:', debugText);
  } else {
    console.log('⚠️ Debug mode indicator not found');
  }

  // Wait for tree to load
  console.log('⏳ Waiting for infrastructure tree to load...');

  try {
    // Wait for tree container
    await page.waitForSelector('.MuiTreeView-root', { timeout: 10000 });
    console.log('✅ Tree container found');

    // Wait for tree items
    await page.waitForSelector('.MuiTreeItem-root', { timeout: 10000 });
    console.log('✅ Tree items found');

    // Count tree items
    const treeItems = await page.locator('.MuiTreeItem-label').all();
    console.log(`✅ Found ${treeItems.length} tree nodes`);

    // Get first few node labels
    if (treeItems.length > 0) {
      console.log('📋 Tree nodes:');
      for (let i = 0; i < Math.min(5, treeItems.length); i++) {
        const label = await treeItems[i].textContent();
        console.log(`   ${i + 1}. ${label}`);
      }
    }

    // Check for "No infrastructure data" message
    const noDataMessage = await page.locator('text=No infrastructure data').first();
    if (await noDataMessage.isVisible()) {
      console.log('❌ "No infrastructure data" message is visible');
    } else {
      console.log('✅ Infrastructure data loaded successfully');
    }

    // Take screenshot
    await page.screenshot({ path: 'infra-tree-loaded.png', fullPage: true });
    console.log('📸 Screenshot saved: infra-tree-loaded.png');

  } catch (error) {
    console.log('❌ Error waiting for tree:', error.message);

    // Check for error messages
    const errorAlert = await page.locator('.MuiAlert-standardError').first();
    if (await errorAlert.isVisible()) {
      const errorText = await errorAlert.textContent();
      console.log('❌ Error alert found:', errorText);
    }

    // Take error screenshot
    await page.screenshot({ path: 'infra-tree-error.png', fullPage: true });
    console.log('📸 Error screenshot saved: infra-tree-error.png');
  }

  // Wait a bit to observe
  await page.waitForTimeout(3000);

  await browser.close();
  console.log('✅ Test completed');
})();