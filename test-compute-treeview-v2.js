const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  // Enable console logging
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.log('Browser Error:', msg.text());
    } else if (msg.text().includes('Payload') || msg.text().includes('Response') || msg.text().includes('Saving')) {
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
    if (response.url().includes('/api/api/computes')) {
      console.log('<<< Response:', response.status(), response.url());
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

    // First, we need to expand the tree to find a suitable parent node
    // Look for "Cloud Computing" which is a resource pool
    console.log('Looking for Cloud Computing node...');
    const cloudComputingNode = await page.locator('text="Cloud Computing"').first();
    
    if (await cloudComputingNode.count() > 0) {
      console.log('Found Cloud Computing node, right-clicking...');
      await cloudComputingNode.click({ button: 'right' });
      await page.waitForTimeout(1000);

      // Look for "Add Compute" in context menu
      const addComputeOption = await page.locator('text="Add Compute"').first();
      if (await addComputeOption.count() > 0) {
        console.log('Clicking "Add Compute" option...');
        await addComputeOption.click();
        await page.waitForTimeout(2000);

        // Now the ComputeEditDialog should be open
        console.log('Filling compute form...');
        
        // Basic Info tab - should be default
        await page.fill('input[name="name"]', 'mustafa-pc');
        await page.fill('input[name="hostname"]', 'mustafa-pc-host');
        await page.fill('input[name="ipAddress"]', '192.168.1.100');
        await page.fill('input[name="macAddress"]', 'AA:BB:CC:DD:EE:FF');

        // Hardware tab
        const hardwareTab = await page.locator('button[role="tab"]:has-text("Hardware")');
        if (await hardwareTab.count() > 0) {
          console.log('Switching to Hardware tab...');
          await hardwareTab.click();
          await page.waitForTimeout(500);
          await page.fill('input[name="cpuCores"]', '8');
          await page.fill('input[name="memoryGb"]', '16');
          await page.fill('input[name="storageGb"]', '512');
        }

        // OS & Software tab
        const osTab = await page.locator('button[role="tab"]:has-text("OS & Software")');
        if (await osTab.count() > 0) {
          console.log('Switching to OS & Software tab...');
          await osTab.click();
          await page.waitForTimeout(500);
          
          // Try to select OS version
          const osSelect = await page.locator('select[name="osVersionId"]');
          if (await osSelect.count() > 0) {
            const options = await osSelect.locator('option').all();
            console.log(`Found ${options.length} OS versions`);
            if (options.length > 1) {
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
          
          // Check if dialog closed (successful save)
          const dialogTitle = await page.locator('h2:has-text("Add Compute")');
          if (await dialogTitle.count() === 0) {
            console.log('Dialog closed - compute likely saved successfully');
          } else {
            console.log('Dialog still open - there may have been an error');
          }
        }
      } else {
        console.log('Add Compute option not found in context menu');
        // List all context menu items for debugging
        const menuItems = await page.locator('[role="menuitem"]').all();
        console.log(`Found ${menuItems.length} menu items:`);
        for (const item of menuItems) {
          const text = await item.textContent();
          console.log(`  - ${text}`);
        }
      }
    } else {
      console.log('Cloud Computing node not found. Looking for other nodes...');
      // Try to find any node we can right-click on
      const treeItems = await page.locator('[role="treeitem"]').all();
      console.log(`Found ${treeItems.length} tree items`);
      for (let i = 0; i < Math.min(5, treeItems.length); i++) {
        const text = await treeItems[i].textContent();
        console.log(`  Tree item ${i}: ${text}`);
      }
    }

    console.log('Test completed. Keeping browser open for inspection...');
    await page.waitForTimeout(30000);

  } catch (error) {
    console.error('Test error:', error);
  } finally {
    await browser.close();
  }
})();