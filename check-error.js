const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  // Capture console messages
  page.on('console', msg => {
    if (msg.type() === 'error' || msg.text().includes('Error')) {
      console.log('❌ Error:', msg.text());
    }
  });

  page.on('pageerror', error => {
    console.log('❌ Page Error:', error.message);
    console.log('Stack:', error.stack);
  });

  try {
    console.log('🚀 Navigating to infrastructure page...');
    await page.goto('http://localhost:3010/infrastructure', { waitUntil: 'domcontentloaded' });
    
    // Wait a bit to see errors
    await page.waitForTimeout(3000);
    
    // Check for React error boundaries
    const errorText = await page.locator('.error-boundary, [class*="error"], [class*="Error"]').allTextContents();
    if (errorText.length > 0) {
      console.log('React Error Boundary text:', errorText);
    }
    
  } catch (error) {
    console.error('❌ Test failed:', error);
  } finally {
    await browser.close();
  }
})();