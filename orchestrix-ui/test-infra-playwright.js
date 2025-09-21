const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({
    headless: false,
    devtools: true
  });

  const page = await browser.newPage();

  // Enable console logging
  page.on('console', msg => {
    console.log(`Browser console [${msg.type()}]:`, msg.text());
  });

  // Capture errors
  page.on('pageerror', error => {
    console.error('Page error:', error.message);
  });

  // Capture request failures
  page.on('requestfailed', request => {
    console.error('Request failed:', request.url(), request.failure().errorText);
  });

  try {
    console.log('Navigating to login page...');
    await page.goto('http://localhost:3010/login');

    // Wait for page to load
    await page.waitForTimeout(2000);

    // Login
    console.log('Logging in...');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');

    // Wait for navigation
    await page.waitForTimeout(3000);

    // Navigate to infrastructure page
    console.log('Navigating to infrastructure page...');
    await page.goto('http://localhost:3010/infrastructure');

    // Wait for page to render
    console.log('Waiting for page to render...');
    await page.waitForTimeout(5000);

    // Check for error messages
    const errorElement = await page.$('text=/No infrastructure data available/i');
    if (errorElement) {
      console.error('ERROR: "No infrastructure data available" message found!');

      // Take a screenshot
      await page.screenshot({ path: 'infra-error.png', fullPage: true });
      console.log('Screenshot saved as infra-error.png');
    } else {
      console.log('SUCCESS: No error message found');

      // Check if tree structure is present
      const treeElement = await page.$('[class*="MuiTreeView"]');
      if (treeElement) {
        console.log('SUCCESS: Tree view component found');

        // Check for tree items
        const treeItems = await page.$$('[role="treeitem"]');
        console.log(`Found ${treeItems.length} tree items`);

        if (treeItems.length > 0) {
          // Get text content of first few items
          for (let i = 0; i < Math.min(5, treeItems.length); i++) {
            const text = await treeItems[i].textContent();
            console.log(`  Tree item ${i + 1}: ${text}`);
          }
        }
      } else {
        console.log('WARNING: Tree view component not found');
      }

      // Take a success screenshot
      await page.screenshot({ path: 'infra-load-test.png', fullPage: true });
      console.log('Screenshot saved as infra-load-test.png');
    }

  } catch (error) {
    console.error('Test failed:', error);
    await page.screenshot({ path: 'error-screenshot.png', fullPage: true });
  }

  // Keep browser open for inspection
  console.log('\nTest complete. Browser will remain open for 10 seconds...');
  await page.waitForTimeout(10000);

  await browser.close();
})();
