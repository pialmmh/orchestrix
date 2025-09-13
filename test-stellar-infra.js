const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ 
    headless: false,
    devtools: true 
  });
  const context = await browser.newContext();
  const page = await context.newPage();

  // Enable console logging
  page.on('console', msg => {
    console.log(`Browser console [${msg.type()}]:`, msg.text());
  });

  // Log network errors
  page.on('requestfailed', request => {
    console.log('Request failed:', request.url(), request.failure().errorText);
  });

  // Log responses
  page.on('response', response => {
    if (response.url().includes('/api/stellar') || response.url().includes('/api/partners')) {
      console.log(`API Response: ${response.url()} - Status: ${response.status()}`);
    }
  });

  try {
    console.log('Navigating to infrastructure-stellar page...');
    await page.goto('http://localhost:3010/infrastructure-stellar', { waitUntil: 'networkidle' });
    
    // Wait for the page to load
    await page.waitForTimeout(5000);
    
    // Check if tree is visible
    const treeVisible = await page.locator('.MuiTreeView-root').isVisible().catch(() => false);
    console.log('Tree visible:', treeVisible);
    
    // Check for any error messages
    const errorAlert = await page.locator('.MuiAlert-root').textContent().catch(() => null);
    if (errorAlert) {
      console.log('Error alert found:', errorAlert);
    }
    
    // Check loading state
    const loadingText = await page.locator('text=Loading infrastructure').isVisible().catch(() => false);
    console.log('Loading indicator visible:', loadingText);
    
    // Get tree content
    const treeContent = await page.locator('.MuiTreeView-root').textContent().catch(() => 'No tree content');
    console.log('Tree content:', treeContent);
    
    // Check for "No infrastructure data" message
    const noDataMsg = await page.locator('text=No infrastructure data').isVisible().catch(() => false);
    console.log('No data message visible:', noDataMsg);
    
    // Take a screenshot
    await page.screenshot({ path: 'stellar-infra-debug.png', fullPage: true });
    console.log('Screenshot saved as stellar-infra-debug.png');
    
    // Check network tab for API calls
    console.log('Checking for API calls...');
    
    // Try to trigger a refresh
    const refreshButton = await page.locator('[data-testid="RefreshIcon"]').first();
    if (await refreshButton.isVisible()) {
      console.log('Clicking refresh button...');
      await refreshButton.click();
      await page.waitForTimeout(3000);
    }
    
    // Keep browser open for inspection
    console.log('\nBrowser will stay open for debugging. Press Ctrl+C to close.');
    await page.waitForTimeout(300000); // Wait 5 minutes
    
  } catch (error) {
    console.error('Error during test:', error);
  }
})();