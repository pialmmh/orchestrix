const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  // Capture console logs
  page.on('console', msg => {
    console.log('BROWSER LOG:', msg.type(), msg.text());
  });

  // Capture errors
  page.on('pageerror', error => {
    console.log('BROWSER ERROR:', error.message);
  });

  try {
    // Navigate directly to infrastructure
    await page.goto('http://localhost:3010/infrastructure');
    await page.waitForTimeout(3000);

    // Check for errors in console
    const errors = await page.evaluate(() => {
      return window.__REACT_DEVTOOLS_GLOBAL_HOOK__ ? 'React DevTools detected' : 'No React DevTools';
    });
    console.log('React status:', errors);

    // Take screenshot
    await page.screenshot({ path: 'infra-debug.png', fullPage: true });

    // Keep browser open for observation
    await page.waitForTimeout(5000);

  } catch (error) {
    console.error('Test Error:', error);
  }

  await browser.close();
})();
