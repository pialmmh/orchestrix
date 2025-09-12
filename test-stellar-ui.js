const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext();
  const page = await context.newPage();

  // Listen for console messages
  page.on('console', msg => {
    console.log('Browser console:', msg.type(), msg.text());
  });

  // Listen for errors
  page.on('pageerror', error => {
    console.error('Browser error:', error);
  });

  try {
    console.log('Navigating to Infrastructure page...');
    await page.goto('http://localhost:3010/infrastructure');
    
    // Wait for the page to load
    await page.waitForTimeout(3000);
    
    console.log('Looking for Test Stellar button...');
    // Click the Test Stellar button
    const testStellarButton = await page.locator('button:has-text("Test Stellar")');
    
    if (await testStellarButton.isVisible()) {
      console.log('Found Test Stellar button, clicking...');
      await testStellarButton.click();
      
      // Wait for the Stellar query to complete
      await page.waitForTimeout(5000);
      
      // Check if the button changed color (indicating active state)
      const buttonColor = await testStellarButton.evaluate(el => window.getComputedStyle(el).backgroundColor);
      console.log('Button background color after click:', buttonColor);
      
      // Look for any tree items that might have loaded
      const treeItems = await page.locator('[role="treeitem"]').count();
      console.log('Number of tree items found:', treeItems);
      
      // Take a screenshot
      await page.screenshot({ path: 'stellar-test-result.png', fullPage: true });
      console.log('Screenshot saved as stellar-test-result.png');
    } else {
      console.log('Test Stellar button not found!');
    }
    
  } catch (error) {
    console.error('Error during test:', error);
  }

  // Keep browser open for manual inspection
  console.log('Test complete. Browser will remain open for inspection.');
  await page.waitForTimeout(30000);
  
  await browser.close();
})();