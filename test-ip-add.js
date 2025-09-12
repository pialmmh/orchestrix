const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ 
    headless: false,
    devtools: true
  });
  const context = await browser.newContext();
  const page = await context.newPage();

  // Enable console logging
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.log('Browser Console Error:', msg.text());
    }
  });

  // Enable request/response logging
  page.on('requestfailed', request => {
    console.log('Request failed:', request.url(), request.failure().errorText);
  });

  page.on('response', response => {
    if (response.status() >= 400) {
      console.log('Error response:', response.url(), response.status());
    }
  });

  try {
    console.log('Navigating to computes page...');
    await page.goto('http://localhost:3005/computes');
    await page.waitForTimeout(3000);

    // Check if there are any computes
    const computeRows = await page.locator('table tbody tr').count();
    if (computeRows === 0) {
      console.log('No computes found. Creating a test compute first...');
      
      // Click Add Compute button
      const addButton = page.locator('button:has-text("Add Compute")');
      await addButton.click();
      await page.waitForTimeout(1000);
      
      // Fill in basic compute details
      await page.fill('input[name="name"]', 'TestCompute');
      await page.fill('input[name="hostname"]', 'test-compute');
      
      // Save the compute
      const saveButton = page.locator('button:has-text("Save")');
      await saveButton.click();
      await page.waitForTimeout(2000);
    }

    // Click on the first compute to edit it
    console.log('Opening first compute for editing...');
    const editButton = page.locator('button[title="Edit"]:visible').first();
    await editButton.click();
    await page.waitForTimeout(2000);

    // Click on Network tab
    console.log('Clicking on Network tab...');
    const networkTab = page.locator('button[role="tab"]:has-text("Network")');
    await networkTab.click();
    await page.waitForTimeout(2000);

    // Look for Add IP Address button
    console.log('Looking for Add IP Address button...');
    const addIpButton = page.locator('button:has-text("Add IP Address")');
    const isVisible = await addIpButton.isVisible();
    console.log('Add IP Address button visible:', isVisible);

    if (isVisible) {
      console.log('Clicking Add IP Address button...');
      await addIpButton.click();
      await page.waitForTimeout(2000);

      // Check if dialog opened
      const dialog = page.locator('div[role="dialog"]');
      const dialogVisible = await dialog.isVisible();
      console.log('Dialog opened:', dialogVisible);

      if (dialogVisible) {
        // Fill in IP address form
        console.log('Filling IP address form...');
        await page.fill('input[label="IP Address"], input[placeholder="192.168.1.100"]', '192.168.1.200');
        
        // Check if Add button in dialog is clickable
        const dialogAddButton = dialog.locator('button:has-text("Add")');
        const isEnabled = await dialogAddButton.isEnabled();
        console.log('Dialog Add button enabled:', isEnabled);
        
        if (isEnabled) {
          console.log('Clicking dialog Add button...');
          
          // Set up response listener for the API call
          const responsePromise = page.waitForResponse(response => 
            response.url().includes('/api/ip-addresses') && response.request().method() === 'POST'
          );
          
          await dialogAddButton.click();
          
          try {
            const response = await responsePromise;
            console.log('API Response status:', response.status());
            const responseBody = await response.json().catch(() => null);
            console.log('API Response body:', responseBody);
          } catch (error) {
            console.log('No API call detected or timeout waiting for response');
          }
        } else {
          console.log('Dialog Add button is disabled');
        }
      } else {
        console.log('Dialog did not open');
      }
    } else {
      console.log('Add IP Address button not found or not visible');
    }

    // Take a screenshot for debugging
    await page.screenshot({ path: 'ip-add-debug.png', fullPage: true });
    console.log('Screenshot saved as ip-add-debug.png');

    // Wait a bit to observe
    await page.waitForTimeout(5000);

  } catch (error) {
    console.error('Error during test:', error);
    await page.screenshot({ path: 'error-screenshot.png', fullPage: true });
  }

  await browser.close();
})();