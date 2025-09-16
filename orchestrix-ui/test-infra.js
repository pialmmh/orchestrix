const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  // Listen to console messages
  page.on('console', msg => {
    console.log(`Browser console [${msg.type()}]: ${msg.text()}`);
  });

  // Listen to errors
  page.on('pageerror', error => {
    console.error('Browser error:', error.message);
  });

  try {
    console.log('Navigating to infrastructure page...');
    await page.goto('http://localhost:3010/infrastructure');

    // Wait for the page to load
    await page.waitForTimeout(5000);

    // Check if tree is visible
    const treeVisible = await page.isVisible('.ant-tree');
    console.log('Tree visible:', treeVisible);

    // Get tree content
    const treeContent = await page.evaluate(() => {
      const tree = document.querySelector('.ant-tree');
      return tree ? tree.textContent : 'No tree found';
    });
    console.log('Tree content:', treeContent);

    // Check for no data message
    const noDataVisible = await page.isVisible('text="No data"');
    console.log('No data message visible:', noDataVisible);

    // Take a screenshot
    await page.screenshot({ path: 'infrastructure-page.png' });
    console.log('Screenshot saved to infrastructure-page.png');

    // Keep browser open for 10 seconds to inspect
    console.log('Keeping browser open for inspection...');
    await page.waitForTimeout(10000);

  } catch (error) {
    console.error('Test error:', error);
  } finally {
    await browser.close();
  }
})();