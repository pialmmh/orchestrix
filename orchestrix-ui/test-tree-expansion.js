const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  page.on('console', msg => {
    const text = msg.text();
    if (text.includes('Extracted nested data')) {
      console.log('📋', text);
    }
  });

  try {
    console.log('🚀 Navigating to infrastructure page...');
    await page.goto('http://localhost:3010/infrastructure', { waitUntil: 'networkidle' });
    
    await page.waitForTimeout(2000);
    
    // Expand tree systematically
    console.log('\n🌳 Expanding tree nodes...');
    
    // Click Telcobright
    const telco = await page.locator('[role="treeitem"]:has-text("Telcobright")').first();
    if (await telco.count() > 0) {
      await telco.click();
      console.log('✓ Expanded Telcobright');
      await page.waitForTimeout(500);
    }
    
    // Click Production Environment  
    const prod = await page.locator('[role="treeitem"]:has-text("Production Environment")').first();
    if (await prod.count() > 0) {
      await prod.click();
      console.log('✓ Expanded Production Environment');
      await page.waitForTimeout(500);
    }
    
    // Look for cloud under Production
    const prodCloud = await page.locator('[role="treeitem"]:has-text("Telcobright")').nth(1);
    if (await prodCloud.count() > 0) {
      await prodCloud.click();
      console.log('✓ Expanded Telcobright cloud');
      await page.waitForTimeout(500);
    }
    
    // Look for Virtual region
    const region = await page.locator('[role="treeitem"]:has-text("Virtual")').first();
    if (await region.count() > 0) {
      await region.click();
      console.log('✓ Expanded Virtual region');
      await page.waitForTimeout(500);
    }
    
    // Look for Moderate AZ
    const az = await page.locator('[role="treeitem"]:has-text("Moderate")').first();
    if (await az.count() > 0) {
      await az.click();
      console.log('✓ Expanded Moderate AZ');
      await page.waitForTimeout(500);
    }
    
    // Count all visible tree items
    const allItems = await page.locator('[role="treeitem"]').all();
    console.log(`\n📊 Total tree items: ${allItems.length}`);
    
    // List all visible items
    console.log('\n🌲 Tree structure:');
    for (const item of allItems) {
      const text = await item.textContent();
      const isVisible = await item.isVisible();
      if (isVisible) {
        // Try to determine depth
        const depth = await item.evaluate(el => {
          let d = 0;
          let parent = el.parentElement;
          while (parent && d < 10) {
            if (parent.getAttribute('role') === 'group') d++;
            parent = parent.parentElement;
          }
          return d;
        });
        console.log(`${'  '.repeat(depth)}└─ ${text}`);
      }
    }
    
    // Check for datacenters
    const datacenters = await page.locator('[role="treeitem"]:has-text("DC")').all();
    console.log(`\n🏢 Found ${datacenters.length} datacenters`);
    for (const dc of datacenters) {
      const text = await dc.textContent();
      console.log(`  - ${text}`);
    }
    
  } catch (error) {
    console.error('❌ Test failed:', error);
  } finally {
    await page.waitForTimeout(2000);
    await browser.close();
  }
})();