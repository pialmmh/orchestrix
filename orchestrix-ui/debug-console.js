const { chromium } = require('playwright');

(async () => {
  console.log('Starting console debugging...');
  
  const browser = await chromium.launch({ headless: false, slowMo: 1000 });
  const context = await browser.newContext();
  const page = await context.newPage();
  
  // Capture console logs
  page.on('console', (message) => {
    console.log(`[BROWSER] ${message.type()}: ${message.text()}`);
  });
  
  // Capture network requests
  page.on('request', (request) => {
    console.log(`[REQUEST] ${request.method()} ${request.url()}`);
  });
  
  // Capture network responses
  page.on('response', (response) => {
    console.log(`[RESPONSE] ${response.status()} ${response.url()}`);
  });
  
  try {
    console.log('Navigating to application...');
    await page.goto('http://localhost:3010');
    
    await page.waitForTimeout(2000);
    
    console.log('Clicking Infrastructure navigation...');
    await page.click('text=Infrastructure');
    
    console.log('Waiting for page to load and API calls to complete...');
    await page.waitForTimeout(10000); // Wait longer for API calls
    
    console.log('Checking for Partners switch...');
    const partnersButton = await page.locator('text=Partners').first();
    if (await partnersButton.isVisible()) {
      console.log('Clicking Partners button to trigger data fetch...');
      await partnersButton.click();
      await page.waitForTimeout(3000);
    }
    
    console.log('Checking for Organization switch...');
    const organizationButton = await page.locator('text=Organization').first();  
    if (await organizationButton.isVisible()) {
      console.log('Clicking Organization button to trigger data fetch...');
      await organizationButton.click();
      await page.waitForTimeout(5000);
    }
    
    // Take final screenshot
    await page.screenshot({ path: 'console-debug-final.png', fullPage: true });
    console.log('Final screenshot saved');
    
  } catch (error) {
    console.error('Debug script failed:', error.message);
  } finally {
    await browser.close();
    console.log('Debug completed');
  }
})();