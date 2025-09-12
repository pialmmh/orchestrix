const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  // Enable console logging
  page.on('console', msg => {
    console.log(`Browser [${msg.type()}]:`, msg.text());
  });

  // Enable network logging for API calls
  page.on('request', request => {
    if (request.url().includes('/api/')) {
      console.log('\n>>> API Request:', request.method(), request.url());
      if (request.method() === 'POST' || request.method() === 'PUT') {
        console.log('>>> Request Body:', request.postData());
      }
    }
  });

  page.on('response', async response => {
    if (response.url().includes('/api/')) {
      console.log('<<< API Response:', response.status(), response.url());
      if (response.status() >= 400) {
        try {
          const body = await response.text();
          console.log('<<< Error Response Body:', body);
        } catch (e) {}
      }
    }
  });

  try {
    console.log('Navigating to compute page...');
    await page.goto('http://localhost:3010/#/resources/compute');
    await page.waitForTimeout(5000); // Wait for page to load

    // Take a screenshot to see what's on the page
    await page.screenshot({ path: 'compute-page.png' });
    console.log('Screenshot saved as compute-page.png');

    // Look for various button selectors
    const addButtons = [
      'button:has-text("Add")',
      'button[aria-label*="Add"]',
      '[data-testid="add-button"]',
      'button.MuiButton-root:has-text("Add")',
      'button:has(svg[data-testid="AddIcon"])',
      'button:has(.MuiButton-startIcon)'
    ];

    let foundButton = false;
    for (const selector of addButtons) {
      const count = await page.locator(selector).count();
      if (count > 0) {
        console.log(`Found Add button with selector: ${selector}`);
        await page.click(selector);
        foundButton = true;
        break;
      }
    }

    if (!foundButton) {
      console.log('Add button not found. Looking for all buttons on page...');
      const buttons = await page.locator('button').all();
      for (const button of buttons) {
        const text = await button.textContent();
        console.log('Button found:', text);
      }
      process.exit(1);
    }

    console.log('Waiting for dialog to open...');
    await page.waitForTimeout(2000);

    // Take screenshot of dialog
    await page.screenshot({ path: 'compute-dialog.png' });
    console.log('Dialog screenshot saved as compute-dialog.png');

    // Fill in the basic form fields
    console.log('Filling basic info...');
    await page.fill('input[name="name"]', 'test-compute-123');
    await page.fill('input[name="hostname"]', 'test-host-123');
    await page.fill('input[name="ipAddress"]', '192.168.1.123');
    await page.fill('input[name="macAddress"]', 'AA:BB:CC:DD:EE:FF');

    // Switch to Hardware tab
    const hardwareTab = await page.locator('button[role="tab"]:has-text("Hardware")');
    if (await hardwareTab.count() > 0) {
      console.log('Clicking Hardware tab...');
      await hardwareTab.click();
      await page.waitForTimeout(500);
      
      await page.fill('input[name="cpuCores"]', '4');
      await page.fill('input[name="memoryGb"]', '8');
      await page.fill('input[name="storageGb"]', '100');
    }

    // Switch to OS & Software tab
    const osTab = await page.locator('button[role="tab"]:has-text("OS & Software")');
    if (await osTab.count() > 0) {
      console.log('Clicking OS & Software tab...');
      await osTab.click();
      await page.waitForTimeout(500);
      
      // Look for OS version select
      const selects = await page.locator('select, [role="combobox"]').all();
      console.log(`Found ${selects.length} select elements`);
    }

    // Find and click Save button
    console.log('Looking for Save button...');
    const saveButton = await page.locator('button:has-text("Save")');
    if (await saveButton.count() > 0) {
      console.log('Clicking Save button...');
      await saveButton.click();
      
      // Wait for response
      await page.waitForTimeout(5000);
      
      // Check for any error messages
      const alerts = await page.locator('[role="alert"], .MuiAlert-root').all();
      for (const alert of alerts) {
        const text = await alert.textContent();
        console.log('Alert found:', text);
      }
    }

    console.log('Test completed. Keeping browser open for 30 seconds...');
    await page.waitForTimeout(30000);

  } catch (error) {
    console.error('Test error:', error);
    await page.screenshot({ path: 'error-state.png' });
  } finally {
    await browser.close();
  }
})();