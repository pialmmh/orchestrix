const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    console.log('Navigating to infrastructure page...');
    await page.goto('http://localhost:3010/infrastructure/organization');

    // Wait a bit for the page to load
    await page.waitForTimeout(5000);

    // Check if we're redirected to login
    const currentUrl = page.url();
    console.log('Current URL:', currentUrl);

    if (currentUrl.includes('/login')) {
      console.log('Redirected to login page, logging in...');

      // Login
      await page.fill('input[name="email"]', 'admin@telcobright.com');
      await page.fill('input[name="password"]', 'admin123');
      await page.click('button[type="submit"]');

      // Wait for navigation
      await page.waitForTimeout(3000);

      // Navigate to infrastructure page again
      await page.goto('http://localhost:3010/infrastructure/organization');
      await page.waitForTimeout(5000);
    }

    // Check for loading or data
    const pageContent = await page.content();

    // Check if we have the loading message
    if (pageContent.includes('Loading infrastructure data')) {
      console.log('❌ Still showing "Loading infrastructure data..."');
    }

    // Check if we have Telcobright partner
    if (pageContent.includes('Telcobright') || pageContent.includes('telcobright')) {
      console.log('✅ Telcobright partner found in the page!');
    }

    // Check for any partners
    const partnersText = await page.textContent('body');
    if (partnersText.includes('Amazon Web Services') ||
        partnersText.includes('Google Cloud') ||
        partnersText.includes('Microsoft Azure')) {
      console.log('✅ Partner data is being displayed!');
    }

    // Take a screenshot
    await page.screenshot({ path: 'infrastructure-page.png', fullPage: true });
    console.log('Screenshot saved as infrastructure-page.png');

    // Check console errors
    page.on('console', msg => {
      if (msg.type() === 'error') {
        console.log('Console error:', msg.text());
      }
    });

    // Wait a bit more to see any delayed loading
    await page.waitForTimeout(3000);

  } catch (error) {
    console.error('Test failed:', error);
  } finally {
    await browser.close();
  }
})();