const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  // Capture console logs
  page.on('console', msg => {
    const text = msg.text();
    if (text.includes('error') || text.includes('Error') || text.includes('ERROR')) {
      console.log('ERROR LOG:', text);
    } else if (text.includes('fetchInfrastructureData') || text.includes('Stellar')) {
      console.log('DATA LOG:', text);
    }
  });

  // Capture page errors
  page.on('pageerror', error => {
    console.log('PAGE ERROR:', error.message);
  });

  try {
    console.log('Navigating to infrastructure page...');
    await page.goto('http://localhost:3010/infrastructure');
    await page.waitForTimeout(5000);

    // Check if tree items are rendered
    const treeItems = await page.$$eval('[role="treeitem"]', items => 
      items.map(item => item.textContent)
    );
    
    console.log('Tree items found:', treeItems.length);
    console.log('Tree content:', treeItems);

    // Check for error messages
    const errorMessages = await page.$$eval('.MuiAlert-message', items => 
      items.map(item => item.textContent)
    );
    if (errorMessages.length > 0) {
      console.log('Error messages on page:', errorMessages);
    }

    // Check if loading indicator is stuck
    const loadingIndicator = await page.$('[role="progressbar"]');
    if (loadingIndicator) {
      console.log('Loading indicator is still visible');
    }

    await page.waitForTimeout(2000);
  } catch (error) {
    console.error('Script error:', error);
  }

  await browser.close();
})();
