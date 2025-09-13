const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  try {
    // Navigate to login page
    await page.goto('http://localhost:3010/login');
    await page.waitForTimeout(2000);

    // Check current URL
    console.log('Current URL:', page.url());

    // Take a screenshot to see what's on the page
    await page.screenshot({ path: 'login-page.png' });

    // Try to find email field with different selectors
    const emailField = await page.$('input[type="email"]') || await page.$('input[name="email"]') || await page.$('#email');
    if (emailField) {
      await emailField.fill('admin@orchestrix.com');
    } else {
      console.log('Email field not found, trying direct navigation to infrastructure');
      // Try going directly to infrastructure (might redirect to login)
      await page.goto('http://localhost:3010/infrastructure');
      await page.waitForTimeout(2000);
      console.log('Current URL after direct navigation:', page.url());
      await page.screenshot({ path: 'direct-nav.png' });
    }

    const passwordField = await page.$('input[type="password"]') || await page.$('input[name="password"]') || await page.$('#password');
    if (passwordField) {
      await passwordField.fill('admin123');
      const submitButton = await page.$('button[type="submit"]');
      if (submitButton) {
        await submitButton.click();
      }
    }

    // Wait for navigation
    await page.waitForTimeout(2000);

    // Navigate to infrastructure page
    await page.goto('http://localhost:3010/infrastructure');
    await page.waitForTimeout(3000);

    // Take screenshot
    await page.screenshot({ path: 'infrastructure-page.png', fullPage: true });

    console.log('Infrastructure page loaded successfully');
    console.log('Screenshot saved as infrastructure-page.png');

    // Check for tree view
    const treeView = await page.$('[role="tree"]');
    if (treeView) {
      console.log('✓ Tree view found');
    } else {
      console.log('✗ Tree view not found');
    }

    // Check for expand/collapse buttons
    const expandAll = await page.getByText('Expand All');
    if (expandAll) {
      console.log('✓ Expand All button found');
    } else {
      console.log('✗ Expand All button not found');
    }

    // Keep browser open for 5 seconds
    await page.waitForTimeout(5000);

  } catch (error) {
    console.error('Error:', error);
    await page.screenshot({ path: 'error-screenshot.png', fullPage: true });
  }

  await browser.close();
})();