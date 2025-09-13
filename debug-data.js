const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  // Capture console logs
  page.on('console', msg => {
    console.log('LOG:', msg.text());
  });

  try {
    // Navigate directly to infrastructure
    await page.goto('http://localhost:3010/infrastructure');
    await page.waitForTimeout(3000);

    // Don't click Partners, keep Organization view
    // await page.click('button[value="partners"]');
    // await page.waitForTimeout(2000);

    // Execute in browser context to check the store
    const storeData = await page.evaluate(() => {
      // Try to access the store data
      const rootEl = document.querySelector('#root');
      const reactKey = Object.keys(rootEl).find(key => key.startsWith('__reactContainer'));
      if (reactKey) {
        console.log('Found React container');
      }
      
      // Log current tree structure
      const treeItems = document.querySelectorAll('[role="treeitem"]');
      console.log(`Found ${treeItems.length} tree items`);
      
      return {
        treeItemCount: treeItems.length,
        firstItems: Array.from(treeItems).slice(0, 5).map(item => item.textContent)
      };
    });

    console.log('Store data:', storeData);
    
    await page.waitForTimeout(3000);
  } catch (error) {
    console.error('Error:', error);
  }

  await browser.close();
})();
