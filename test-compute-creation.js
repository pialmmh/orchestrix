const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext();
  const page = await context.newPage();

  // Enable console logging
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.log('Browser Console Error:', msg.text());
    } else {
      console.log('Browser Console:', msg.text());
    }
  });

  // Enable network logging
  page.on('request', request => {
    if (request.url().includes('/api/')) {
      console.log('Request:', request.method(), request.url());
      if (request.method() === 'POST' || request.method() === 'PUT') {
        console.log('Request Body:', request.postData());
      }
    }
  });

  page.on('response', response => {
    if (response.url().includes('/api/') && response.status() >= 400) {
      console.log('Response Error:', response.status(), response.url());
      response.text().then(body => console.log('Response Body:', body));
    }
  });

  try {
    // Navigate to the compute page
    await page.goto('http://localhost:3010/#/resources/compute');
    await page.waitForTimeout(3000);

    // Click the Add button
    console.log('Clicking Add button...');
    await page.click('button:has-text("Add")');
    await page.waitForTimeout(1000);

    // Fill in the form
    console.log('Filling form...');
    await page.fill('input[name="name"]', 'mustafa-pc-test');
    await page.fill('input[name="hostname"]', 'mustafa-pc');
    await page.fill('input[name="ipAddress"]', '192.168.1.100');
    await page.fill('input[name="macAddress"]', '00:11:22:33:44:55');
    
    // Hardware tab
    await page.click('button[role="tab"]:has-text("Hardware")');
    await page.waitForTimeout(500);
    await page.fill('input[name="cpuCores"]', '4');
    await page.fill('input[name="memoryGb"]', '8');
    await page.fill('input[name="storageGb"]', '100');

    // OS & Software tab
    await page.click('button[role="tab"]:has-text("OS & Software")');
    await page.waitForTimeout(500);
    
    // Check if OS dropdown exists and select first option
    const osDropdown = await page.locator('select[name="osVersionId"], [data-testid="os-version-select"], #osVersionId, [name="osVersion"]');
    if (await osDropdown.count() > 0) {
      console.log('OS dropdown found, selecting first option...');
      const options = await osDropdown.locator('option').all();
      if (options.length > 1) {
        await osDropdown.selectOption({ index: 1 });
      }
    } else {
      console.log('OS dropdown not found, checking for MUI Select...');
      const muiSelect = await page.locator('[id*="osVersion"], [aria-label*="OS Version"]').first();
      if (await muiSelect.count() > 0) {
        await muiSelect.click();
        await page.waitForTimeout(500);
        const firstOption = await page.locator('[role="option"]').first();
        if (await firstOption.count() > 0) {
          await firstOption.click();
        }
      }
    }

    // Submit the form
    console.log('Submitting form...');
    await page.click('button:has-text("Save")');
    
    // Wait for response
    await page.waitForTimeout(5000);

    // Check for error messages
    const errorToast = await page.locator('.MuiAlert-message, [role="alert"]');
    if (await errorToast.count() > 0) {
      console.log('Error message:', await errorToast.textContent());
    }

    console.log('Test completed');
    await page.waitForTimeout(10000);

  } catch (error) {
    console.error('Test failed:', error);
  } finally {
    await browser.close();
  }
})();