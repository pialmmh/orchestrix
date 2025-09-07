const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  
  // Collect console errors
  const errors = [];
  page.on('console', msg => {
    if (msg.type() === 'error') {
      errors.push(msg.text());
    }
  });
  
  // Collect page errors
  page.on('pageerror', error => {
    errors.push(`Page error: ${error.message}`);
  });
  
  try {
    // Navigate to the app
    console.log('Navigating to http://localhost:3010...');
    await page.goto('http://localhost:3010', { waitUntil: 'networkidle', timeout: 30000 });
    
    // Wait a bit for any async errors
    await page.waitForTimeout(5000);
    
    // Check if main app element exists
    const appElement = await page.$('#root');
    if (appElement) {
      console.log('✓ App root element found');
    } else {
      console.log('✗ App root element NOT found');
    }
    
    // Try to find the Infrastructure page
    const infraLink = await page.$('text=Infrastructure');
    if (infraLink) {
      console.log('✓ Infrastructure link found');
      await infraLink.click();
      await page.waitForTimeout(2000);
    }
    
    // Report any errors
    if (errors.length > 0) {
      console.log('\n❌ Console errors found:');
      errors.forEach(err => console.log('  -', err));
    } else {
      console.log('\n✓ No console errors detected');
    }
    
    // Take a screenshot
    await page.screenshot({ path: '/tmp/orchestrix-app.png' });
    console.log('\nScreenshot saved to /tmp/orchestrix-app.png');
    
  } catch (error) {
    console.error('Navigation/Test error:', error.message);
  }
  
  await browser.close();
})();