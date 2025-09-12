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
    console.log('Navigating to main page...');
    await page.goto('http://localhost:3010/');
    await page.waitForTimeout(3000);

    // First expand Resources menu
    console.log('Looking for Resources menu item...');
    const resourcesMenu = await page.locator('text=Resources').first();
    if (await resourcesMenu.count() > 0) {
      console.log('Clicking Resources menu...');
      await resourcesMenu.click();
      await page.waitForTimeout(1000);
    }

    // Now click on Compute under Resources
    console.log('Looking for Compute menu item...');
    const computeMenu = await page.locator('text=Compute').first();
    if (await computeMenu.count() > 0) {
      console.log('Clicking Compute menu...');
      await computeMenu.click();
      await page.waitForTimeout(3000);
    }

    // Take a screenshot to see if we're on the right page
    await page.screenshot({ path: 'compute-page-nav.png' });
    console.log('Screenshot saved as compute-page-nav.png');

    // Now look for the Add button
    console.log('Looking for Add button...');
    
    // Try different selectors
    const addSelectors = [
      'button:has-text("Add")',
      'button:has-text("ADD")',
      '[aria-label*="add" i]',
      'button:has(svg)',
      '.MuiFab-root',
      '[data-testid*="add" i]'
    ];

    let foundAdd = false;
    for (const selector of addSelectors) {
      const count = await page.locator(selector).count();
      console.log(`Selector "${selector}": found ${count} elements`);
      if (count > 0 && !foundAdd) {
        const elem = await page.locator(selector).first();
        const text = await elem.textContent().catch(() => '');
        const ariaLabel = await elem.getAttribute('aria-label').catch(() => '');
        console.log(`  Text: "${text}", Aria-label: "${ariaLabel}"`);
        
        // Check if this is likely an Add button
        if (text.includes('Add') || text.includes('ADD') || ariaLabel?.includes('add')) {
          console.log('Clicking this Add button...');
          await elem.click();
          foundAdd = true;
          await page.waitForTimeout(2000);
          break;
        }
      }
    }

    if (!foundAdd) {
      // Look for FAB (Floating Action Button)
      const fabButton = await page.locator('.MuiFab-root, [aria-label*="Add" i]').first();
      if (await fabButton.count() > 0) {
        console.log('Found FAB button, clicking...');
        await fabButton.click();
        foundAdd = true;
        await page.waitForTimeout(2000);
      }
    }

    if (foundAdd) {
      // Take screenshot of dialog
      await page.screenshot({ path: 'compute-dialog-open.png' });
      console.log('Dialog screenshot saved as compute-dialog-open.png');

      // Fill in the form
      console.log('Filling form fields...');
      
      // Basic info
      await page.fill('input[name="name"]', 'mustafa-pc');
      await page.fill('input[name="hostname"]', 'mustafa-pc-host');
      await page.fill('input[name="ipAddress"]', '192.168.1.100');
      await page.fill('input[name="macAddress"]', 'AA:BB:CC:DD:EE:FF');

      // Hardware tab
      const hardwareTab = await page.locator('button[role="tab"]:has-text("Hardware")');
      if (await hardwareTab.count() > 0) {
        await hardwareTab.click();
        await page.waitForTimeout(500);
        await page.fill('input[name="cpuCores"]', '4');
        await page.fill('input[name="memoryGb"]', '8');
        await page.fill('input[name="storageGb"]', '256');
      }

      // OS & Software tab  
      const osTab = await page.locator('button[role="tab"]:has-text("OS & Software")');
      if (await osTab.count() > 0) {
        await osTab.click();
        await page.waitForTimeout(500);
        
        // Try to select OS version
        const osSelect = await page.locator('select[name="osVersionId"]').first();
        if (await osSelect.count() > 0) {
          const options = await osSelect.locator('option').all();
          if (options.length > 1) {
            await osSelect.selectOption({ index: 1 });
            console.log('Selected OS version');
          }
        }
      }

      // Save
      console.log('Looking for Save button...');
      const saveButton = await page.locator('button:has-text("Save")').first();
      if (await saveButton.count() > 0) {
        console.log('Clicking Save...');
        await saveButton.click();
        
        // Wait for API response
        await page.waitForTimeout(5000);
        
        // Check for alerts/errors
        const alerts = await page.locator('[role="alert"], .MuiAlert-root, .MuiSnackbar-root').all();
        for (const alert of alerts) {
          const text = await alert.textContent();
          console.log('Alert/Error message:', text);
        }
        
        // Take final screenshot
        await page.screenshot({ path: 'compute-after-save.png' });
      }
    }

    console.log('Test completed. Keeping browser open for inspection...');
    await page.waitForTimeout(30000);

  } catch (error) {
    console.error('Test error:', error);
    await page.screenshot({ path: 'error-state.png' });
  } finally {
    await browser.close();
  }
})();