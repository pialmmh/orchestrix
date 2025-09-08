const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  // Listen to all network requests to see API calls
  page.on('response', async response => {
    if (response.url().includes('/api/')) {
      console.log(`📡 API Response: ${response.status()} ${response.url()}`);
      if (response.url().includes('/api/clouds')) {
        try {
          const data = await response.json();
          console.log('☁️ Clouds API Response:', JSON.stringify(data, null, 2));
        } catch (e) {
          console.log('❌ Failed to parse clouds response:', e.message);
        }
      }
    }
  });
  
  page.on('console', msg => {
    const text = msg.text();
    if (text.includes('buildTenantTree') || text.includes('🏢') || text.includes('⚠️') || text.includes('🔍')) {
      console.log('🌳 Tree Building:', text);
    } else if (text.includes('Error') || text.includes('error')) {
      console.log('❌ Error:', text);
    } else {
      console.log('🖥️ Browser Console:', text);
    }
  });
  
  try {
    console.log('🚀 Navigating to infrastructure page...');
    await page.goto('http://localhost:3010/infrastructure');
    
    // Wait for the page to load and data to process
    await page.waitForTimeout(8000);
    
    // Check for tree items
    console.log('🌳 Looking for tree items...');
    const treeItems = await page.locator('[role="treeitem"]').all();
    console.log(`Found ${treeItems.length} tree items`);
    
    for (let i = 0; i < Math.min(treeItems.length, 10); i++) {
      const text = await treeItems[i].textContent();
      console.log(`  ${i}: ${text}`);
    }
    
    // Also check for any tree view content
    console.log('🌳 Looking for tree view content...');
    const treeView = await page.locator('.MuiTreeView-root, [data-testid="tree-view"], .tree-view').all();
    console.log(`Found ${treeView.length} tree view containers`);
    
    // Check for any "No infrastructure" or error messages
    console.log('🔍 Looking for status messages...');
    const statusMessages = await page.locator('text="No infrastructure data available", text="Loading", text="Error"').all();
    console.log(`Found ${statusMessages.length} status messages`);
    
    for (let i = 0; i < statusMessages.length; i++) {
      const text = await statusMessages[i].textContent();
      console.log(`  Status ${i}: ${text}`);
    }
    
    // Look for specific cloud-related content
    console.log('☁️ Looking for cloud data...');
    const cloudElements = await page.locator('text=/cloud|Cloud/i').all();
    console.log(`Found ${cloudElements.length} cloud-related elements`);
    
    for (let i = 0; i < cloudElements.length; i++) {
      const text = await cloudElements[i].textContent();
      console.log(`  Cloud element ${i}: ${text}`);
    }
    
    // Check if there are any error messages
    const errorElements = await page.locator('text=/error|Error|failed|Failed/i').all();
    if (errorElements.length > 0) {
      console.log('❌ Found error messages:');
      for (let i = 0; i < errorElements.length; i++) {
        const text = await errorElements[i].textContent();
        console.log(`  Error ${i}: ${text}`);
      }
    }
    
    // Wait a bit more to see if data loads
    await page.waitForTimeout(2000);
    
  } catch (error) {
    console.error('❌ Test failed:', error);
  } finally {
    await browser.close();
  }
})();