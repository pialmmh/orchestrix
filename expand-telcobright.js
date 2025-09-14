const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  // Capture console logs
  page.on('console', msg => {
    const text = msg.text();
    if (text.includes('renderTreeItems') || text.includes('Rendering node')) {
      console.log('TREE LOG:', text);
    }
  });

  try {
    console.log('Navigating to infrastructure page...');
    await page.goto('http://localhost:3010/infrastructure');
    await page.waitForTimeout(3000);

    // Try to expand Telcobright node
    console.log('Looking for Telcobright expand button...');
    const expandButton = await page.$('[role="treeitem"] [aria-label*="expand"]');
    if (expandButton) {
      console.log('Found expand button, clicking...');
      await expandButton.click();
      await page.waitForTimeout(2000);
    } else {
      console.log('No expand button found - checking if already expanded');
    }

    // Check tree items after expansion
    const treeItems = await page.$$eval('[role="treeitem"]', items => 
      items.map(item => ({
        text: item.textContent,
        level: item.getAttribute('aria-level')
      }))
    );
    
    console.log('Tree structure:');
    treeItems.forEach(item => {
      const indent = '  '.repeat(parseInt(item.level || '1') - 1);
      console.log(`${indent}- ${item.text}`);
    });

    await page.waitForTimeout(2000);
  } catch (error) {
    console.error('Script error:', error);
  }

  await browser.close();
})();
