const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  // Capture console logs
  const consoleLogs = [];
  page.on('console', msg => {
    const text = msg.text();
    consoleLogs.push(text);
    
    // Log important messages immediately
    if (text.includes('API Data fetched') || 
        text.includes('Built tree') || 
        text.includes('buildTenantTree') ||
        text.includes('buildEnvironmentTree') ||
        text.includes('Extracting regions') ||
        text.includes('selfPartners') ||
        text.includes('- clouds:') ||
        text.includes('- regions:') ||
        text.includes('- azs:') ||
        text.includes('- datacenters:')) {
      console.log('📋 Console:', text);
    }
  });

  page.on('pageerror', error => {
    console.log('❌ Page Error:', error.message);
  });

  try {
    console.log('🚀 Navigating to infrastructure page...');
    await page.goto('http://localhost:3010/infrastructure', { waitUntil: 'networkidle' });
    
    await page.waitForTimeout(3000);
    
    // Check API responses
    console.log('\n📡 Checking API responses...');
    
    // Wait for tree to potentially render
    await page.waitForTimeout(2000);
    
    // Check tree structure
    const treeItems = await page.locator('[role="treeitem"]').all();
    console.log(`\n🌳 Found ${treeItems.length} tree items in DOM`);
    
    // List all tree items
    for (const item of treeItems) {
      const text = await item.textContent();
      const visible = await item.isVisible();
      if (visible) {
        console.log(`  ✓ ${text}`);
      }
    }
    
    // Check for "No infrastructure data" message
    const noDataMessage = await page.locator('text=No infrastructure data').count();
    if (noDataMessage > 0) {
      console.log('\n⚠️ "No infrastructure data" message is displayed');
    }
    
    // Try to expand Telcobright if it exists
    const telcoNode = await page.locator('[role="treeitem"]:has-text("Telcobright")').first();
    if (await telcoNode.count() > 0) {
      console.log('\n🔍 Found Telcobright node, clicking to expand...');
      await telcoNode.click();
      await page.waitForTimeout(1000);
      
      // Check for environments
      const envNodes = await page.locator('[role="treeitem"]:has-text("Environment")').all();
      console.log(`📁 Found ${envNodes.length} environment nodes`);
      
      // Try to expand Production Environment
      const prodEnv = await page.locator('[role="treeitem"]:has-text("Production Environment")').first();
      if (await prodEnv.count() > 0) {
        console.log('🔍 Clicking Production Environment...');
        await prodEnv.click();
        await page.waitForTimeout(1000);
        
        // Check for cloud nodes
        const cloudNodes = await page.locator('[role="treeitem"]:has-text("Telcobright")').all();
        console.log(`☁️ Found ${cloudNodes.length} nodes with "Telcobright" text`);
      }
    }
    
    // Log relevant console messages
    console.log('\n📝 Key Console Logs:');
    consoleLogs.filter(log => 
      log.includes('clouds:') || 
      log.includes('regions:') || 
      log.includes('azs:') || 
      log.includes('datacenters:') ||
      log.includes('Built tree:') ||
      log.includes('Extracting')
    ).forEach(log => console.log('  ', log));
    
    // Check React DevTools if available
    const reactTree = await page.evaluate(() => {
      const rootEl = document.querySelector('#root');
      if (rootEl && rootEl._reactRootContainer) {
        // Try to find the tree data in React fiber
        return 'React root found';
      }
      return 'React root not accessible';
    });
    console.log('\n⚛️ React status:', reactTree);
    
  } catch (error) {
    console.error('❌ Test failed:', error);
  } finally {
    await page.waitForTimeout(2000);
    await browser.close();
  }
})();