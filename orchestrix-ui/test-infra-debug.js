const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  // Enable console logging
  page.on('console', msg => {
    console.log(`[Browser Console] ${msg.type()}: ${msg.text()}`);
  });

  page.on('pageerror', err => {
    console.error(`[Page Error]`, err);
  });

  console.log('Opening Infrastructure page...');
  await page.goto('http://localhost:3010/infrastructure/organization');

  // Wait for potential loading
  await page.waitForTimeout(5000);

  // Check for debug mode indicator
  const debugIndicator = await page.locator('text=/Debug Mode/i').isVisible().catch(() => false);
  console.log(`Debug Mode Indicator Visible: ${debugIndicator}`);

  // Check for tree data
  const treeElement = await page.locator('[role="tree"], .tree-container, [data-testid="tree"]').first();
  const hasTree = await treeElement.isVisible().catch(() => false);
  console.log(`Tree Visible: ${hasTree}`);

  // Check for any error messages
  const errorText = await page.locator('text=/error|failed|no.*data/i').first();
  const hasError = await errorText.isVisible().catch(() => false);
  if (hasError) {
    const errorMessage = await errorText.textContent();
    console.log(`Error found: ${errorMessage}`);
  }

  // Take screenshot
  await page.screenshot({ path: 'infra-debug-test.png' });
  console.log('Screenshot saved as infra-debug-test.png');

  // Keep browser open for 10 seconds to observe
  console.log('Keeping browser open for observation...');
  await page.waitForTimeout(10000);

  await browser.close();
  console.log('Test completed');
})();