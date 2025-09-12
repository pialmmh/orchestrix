const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext();
  // Clear cache for fresh start
  await context.clearCookies();
  const page = await context.newPage();

  // Enable console logging
  page.on('console', msg => {
    if (msg.text().includes('API URL:')) {
      console.log('Config:', msg.text());
    }
  });

  page.on('request', request => {
    if (request.url().includes('/api/')) {
      console.log('API Request:', request.method(), request.url());
    }
  });

  page.on('response', async response => {
    if (response.url().includes('/api/api/computes') && response.method() === 'POST') {
      console.log('Compute Creation Response:', response.status());
      if (response.status() >= 400) {
        const body = await response.text();
        console.log('Error:', body);
      }
    }
  });

  try {
    // Navigate with cache bypass
    console.log('Navigating to main page...');
    await page.goto('http://localhost:3010/', { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);
    
    // Hard refresh to ensure new config is loaded
    await page.reload({ waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);

    // Click Infrastructure
    console.log('Navigating to Infrastructure...');
    await page.click('text="Infrastructure"');
    await page.waitForTimeout(3000);

    // Expand the tree to find nodes
    console.log('Expanding tree nodes...');
    const expandButtons = await page.locator('[data-testid*="Expand"], .MuiTreeItem-iconContainer').all();
    for (let i = 0; i < Math.min(3, expandButtons.length); i++) {
      try {
        await expandButtons[i].click();
        await page.waitForTimeout(500);
      } catch (e) {
        // Ignore if already expanded
      }
    }

    // Try to find a node where we can add compute
    const targetNodes = [
      'Cloud Computing',
      'Virtual', 
      'Physical',
      'TestDC-with-groups',
      'Moderate',
      'Development Environment',
      'Production Environment'
    ];

    let nodeFound = false;
    for (const nodeName of targetNodes) {
      const node = await page.locator(`text="${nodeName}"`).first();
      if (await node.count() > 0) {
        console.log(`Found node: ${nodeName}, right-clicking...`);
        await node.click({ button: 'right' });
        nodeFound = true;
        await page.waitForTimeout(1000);
        
        // Check if Add Compute is available
        const addCompute = await page.locator('text="Add Compute"');
        if (await addCompute.count() > 0) {
          console.log('Found "Add Compute" option, clicking...');
          await addCompute.click();
          await page.waitForTimeout(2000);
          
          // Fill the form
          console.log('Filling compute form...');
          await page.fill('input[name="name"]', 'mustafa-pc');
          await page.fill('input[name="hostname"]', 'mustafa-pc.local');
          await page.fill('input[name="ipAddress"]', '192.168.1.100');
          await page.fill('input[name="macAddress"]', 'AA:BB:CC:DD:EE:FF');
          
          // Hardware tab
          await page.click('button[role="tab"]:has-text("Hardware")');
          await page.waitForTimeout(500);
          await page.fill('input[name="cpuCores"]', '8');
          await page.fill('input[name="memoryGb"]', '16');
          await page.fill('input[name="storageGb"]', '512');
          
          // OS tab
          await page.click('button[role="tab"]:has-text("OS")');
          await page.waitForTimeout(500);
          const osSelect = await page.locator('select[name="osVersionId"]');
          if (await osSelect.count() > 0) {
            await osSelect.selectOption({ index: 1 });
          }
          
          // Save
          console.log('Saving compute...');
          await page.click('button:has-text("Save")');
          await page.waitForTimeout(5000);
          
          // Check result
          const dialog = await page.locator('h2:has-text("Compute")');
          if (await dialog.count() === 0) {
            console.log('✓ Compute saved successfully!');
          } else {
            console.log('✗ Dialog still open - check for errors');
          }
          
          break;
        }
      }
    }
    
    if (!nodeFound) {
      console.log('Could not find any suitable node for adding compute');
    }

    console.log('Test complete. Browser stays open for 20 seconds...');
    await page.waitForTimeout(20000);

  } catch (error) {
    console.error('Test error:', error);
  } finally {
    await browser.close();
  }
})();