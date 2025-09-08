const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  // Capture all console messages
  page.on('console', msg => {
    const text = msg.text();
    if (text.includes('Built tree') || 
        text.includes('selfPartners') || 
        text.includes('Organization mode') ||
        text.includes('clouds:') ||
        text.includes('regions:') ||
        text.includes('buildEnvironmentTree')) {
      console.log('🖥️ Console:', text);
    }
  });

  try {
    console.log('🚀 Navigating to infrastructure page...');
    await page.goto('http://localhost:3010/infrastructure', { waitUntil: 'networkidle' });
    
    await page.waitForTimeout(3000);
    
    // Check tree structure
    const treeItems = await page.locator('[role="treeitem"]').all();
    console.log(`\n📊 Found ${treeItems.length} tree items`);
    
    for (const item of treeItems) {
      const text = await item.textContent();
      const level = await item.evaluate(el => {
        let depth = 0;
        let parent = el.parentElement;
        while (parent) {
          if (parent.getAttribute('role') === 'group') depth++;
          parent = parent.parentElement;
        }
        return depth;
      });
      console.log(`${'  '.repeat(level)}└─ ${text}`);
    }
    
    // Try to expand Telcobright node
    const telcoNode = await page.locator('[role="treeitem"]:has-text("Telcobright")').first();
    if (telcoNode) {
      console.log('\n🔍 Clicking Telcobright to expand...');
      await telcoNode.click();
      await page.waitForTimeout(1000);
      
      // Check for Production Environment
      const prodEnv = await page.locator('[role="treeitem"]:has-text("Production Environment")').first();
      if (prodEnv) {
        console.log('🔍 Clicking Production Environment...');
        await prodEnv.click();
        await page.waitForTimeout(1000);
      }
    }
    
    // Re-check tree after expansion
    const expandedItems = await page.locator('[role="treeitem"]').all();
    console.log(`\n📊 After expansion: ${expandedItems.length} tree items`);
    
    for (const item of expandedItems) {
      const text = await item.textContent();
      const visible = await item.isVisible();
      if (visible) {
        console.log(`  - ${text}`);
      }
    }
    
  } catch (error) {
    console.error('❌ Test failed:', error);
  } finally {
    await page.waitForTimeout(2000);
    await browser.close();
  }
})();