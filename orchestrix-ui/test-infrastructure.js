const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext();
  const page = await context.newPage();

  console.log('Opening infrastructure page...');
  await page.goto('http://localhost:3026/infrastructure/organization');

  // Wait for the page to load
  await page.waitForTimeout(5000);

  // Take a screenshot
  await page.screenshot({ path: 'infrastructure-test.png' });

  // Check for any errors in console
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.error('Console error:', msg.text());
    }
  });

  // Check if loading message is visible
  const loadingText = await page.locator('text=Loading infrastructure data').count();
  if (loadingText > 0) {
    console.log('⚠️ Still showing loading message');
  }

  // Check if error message is visible
  const errorText = await page.locator('text=No infrastructure data available').count();
  if (errorText > 0) {
    console.log('❌ Error: No infrastructure data available');
  }

  // Check if tree nodes are visible
  const treeNodes = await page.locator('[role="treeitem"]').count();
  console.log(`✓ Found ${treeNodes} tree nodes`);

  // Wait a bit more to see the result
  await page.waitForTimeout(3000);

  await browser.close();
  console.log('Test completed');
})();