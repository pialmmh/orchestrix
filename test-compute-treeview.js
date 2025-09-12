const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  // Enable console logging
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.log('Browser Error:', msg.text());
    } else if (msg.type() === 'log' && (msg.text().includes('Payload') || msg.text().includes('Response'))) {
      console.log('Browser Log:', msg.text());
    }
  });

  // Enable network logging for API calls
  page.on('request', request => {
    if (request.url().includes('/api/api/computes') && request.method() === 'POST') {
      console.log('\n>>> Creating Compute:', request.url());
      console.log('>>> Request Body:', request.postData());
    }
  });

  page.on('response', async response => {
    if (response.url().includes('/api/api/computes') && response.method() === 'POST') {
      console.log('<<< Response Status:', response.status());
      if (response.status() >= 400) {
        try {
          const body = await response.text();
          console.log('<<< Error Response:', body);
        } catch (e) {}
      }
    }
  });

  try {
    console.log('Navigating to Infrastructure page...');
    await page.goto('http://localhost:3000/#/infrastructure');
    await page.waitForTimeout(3000);

    // Look for "Compute" node in the treeview
    console.log('Looking for Compute node in treeview...');
    const computeNode = await page.locator('text="Compute"').first();
    
    if (await computeNode.count() > 0) {
      console.log('Found Compute node, right-clicking...');
      await computeNode.click({ button: 'right' });
      await page.waitForTimeout(1000);

      // Look for "Add Compute" in context menu
      const addComputeOption = await page.locator('text="Add Compute"').first();
      if (await addComputeOption.count() > 0) {
        console.log('Clicking "Add Compute" option...');
        await addComputeOption.click();
        await page.waitForTimeout(2000);

        // Fill in the form
        console.log('Filling compute form...');
        
        // Basic Info tab
        await page.fill('input[name="name"]', 'mustafa-pc');
        await page.fill('input[name="hostname"]', 'mustafa-pc-host');
        await page.fill('input[name="ipAddress"]', '192.168.1.100');
        await page.fill('input[name="macAddress"]', 'AA:BB:CC:DD:EE:FF');

        // Hardware tab
        const hardwareTab = await page.locator('button[role="tab"]:has-text("Hardware")');
        if (await hardwareTab.count() > 0) {
          await hardwareTab.click();
          await page.waitForTimeout(500);
          await page.fill('input[name="cpuCores"]', '8');
          await page.fill('input[name="memoryGb"]', '16');
          await page.fill('input[name="storageGb"]', '512');
        }

        // OS & Software tab
        const osTab = await page.locator('button[role="tab"]:has-text("OS & Software")');
        if (await osTab.count() > 0) {
          await osTab.click();
          await page.waitForTimeout(500);
          
          // Select OS version
          const osSelect = await page.locator('select[name="osVersionId"]');
          if (await osSelect.count() > 0) {
            // Get all options
            const options = await osSelect.locator('option').all();
            console.log(`Found ${options.length} OS versions`);
            if (options.length > 1) {
              // Select the first non-empty option (usually index 1)
              await osSelect.selectOption({ index: 1 });
              console.log('Selected OS version');
            }
          }
        }

        // Save the compute
        console.log('Clicking Save button...');
        const saveButton = await page.locator('button:has-text("Save")');
        if (await saveButton.count() > 0) {
          await saveButton.click();
          
          // Wait for response
          await page.waitForTimeout(5000);
          
          // Check for success or error messages
          const alerts = await page.locator('[role="alert"], .MuiAlert-root, .MuiSnackbar-root').all();
          for (const alert of alerts) {
            const text = await alert.textContent();
            console.log('Alert message:', text);
          }
        }
      } else {
        console.log('Add Compute option not found in context menu');
      }
    } else {
      console.log('Compute node not found in treeview');
    }

    console.log('Test completed. Keeping browser open for inspection...');
    await page.waitForTimeout(30000);

  } catch (error) {
    console.error('Test error:', error);
  } finally {
    await browser.close();
  }
})();