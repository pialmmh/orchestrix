const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  // Enable console and network logging
  page.on('console', msg => {
    if (msg.type() === 'error' || msg.text().includes('Error')) {
      console.log('Browser:', msg.text());
    }
  });

  page.on('request', request => {
    if (request.url().includes('/api/api/computes') && request.method() === 'POST') {
      console.log('\n>>> POST to Computes API');
      console.log('>>> URL:', request.url());
      console.log('>>> Body:', request.postData());
    }
  });

  page.on('response', async response => {
    if (response.url().includes('/api/api/computes') && response.status() >= 400) {
      console.log('\n<<< Error Response:', response.status());
      try {
        const body = await response.text();
        console.log('<<< Body:', body);
      } catch (e) {}
    }
  });

  try {
    // Navigate to infrastructure page
    console.log('Navigating to Infrastructure page...');
    await page.goto('http://localhost:3010/#/infrastructure');
    await page.waitForTimeout(5000); // Wait for tree to load

    // Take a screenshot to see what's loaded
    await page.screenshot({ path: 'infrastructure-page.png' });
    console.log('Screenshot saved as infrastructure-page.png');

    // Try to find any datacenter or pool node and right-click it
    // First, let's expand some nodes to find a suitable parent
    console.log('Looking for expandable nodes...');
    
    // Look for the expand icons and click them
    const expandIcons = await page.locator('[data-testid="TreeViewExpandIcon"], svg[data-testid="ExpandMoreIcon"]').all();
    console.log(`Found ${expandIcons.length} expand icons`);
    
    // Click the first few to expand the tree
    for (let i = 0; i < Math.min(3, expandIcons.length); i++) {
      await expandIcons[i].click();
      await page.waitForTimeout(500);
    }

    // Now look for a datacenter or any node we can add compute to
    const nodeTexts = ['Dhaka-DC1', 'TestDC', 'Cloud Computing', 'Virtual', 'Physical'];
    let foundNode = false;
    
    for (const nodeText of nodeTexts) {
      const node = await page.locator(`text="${nodeText}"`).first();
      if (await node.count() > 0) {
        console.log(`Found node: ${nodeText}, right-clicking...`);
        await node.click({ button: 'right' });
        foundNode = true;
        await page.waitForTimeout(1000);
        break;
      }
    }

    if (!foundNode) {
      // If no specific nodes found, try to right-click any treeitem
      const treeItem = await page.locator('[role="treeitem"]').first();
      if (await treeItem.count() > 0) {
        const text = await treeItem.textContent();
        console.log(`Right-clicking first tree item: ${text}`);
        await treeItem.click({ button: 'right' });
        await page.waitForTimeout(1000);
      }
    }

    // Look for Add Compute option
    const addComputeMenuItem = await page.locator('text="Add Compute"');
    if (await addComputeMenuItem.count() > 0) {
      console.log('Found "Add Compute" menu item, clicking...');
      await addComputeMenuItem.click();
      await page.waitForTimeout(2000);

      // Fill the form
      console.log('Filling compute form...');
      
      // Basic Info
      await page.fill('input[name="name"]', 'mustafa-pc');
      await page.fill('input[name="hostname"]', 'mustafa-pc.local');
      await page.fill('input[name="ipAddress"]', '192.168.1.200');
      await page.fill('input[name="macAddress"]', '00:11:22:33:44:55');

      // Hardware tab
      const hardwareTab = await page.locator('button[role="tab"]:has-text("Hardware")');
      await hardwareTab.click();
      await page.waitForTimeout(500);
      
      await page.fill('input[name="cpuCores"]', '8');
      await page.fill('input[name="memoryGb"]', '16'); 
      await page.fill('input[name="storageGb"]', '512');

      // OS tab
      const osTab = await page.locator('button[role="tab"]:has-text("OS")');
      await osTab.click();
      await page.waitForTimeout(500);
      
      // Select OS if dropdown exists
      const osSelect = await page.locator('select[name="osVersionId"]');
      if (await osSelect.count() > 0) {
        const options = await osSelect.locator('option').all();
        if (options.length > 1) {
          await osSelect.selectOption({ index: 1 });
          console.log('Selected OS version');
        }
      }

      // Save
      console.log('Saving compute...');
      const saveButton = await page.locator('button:has-text("Save")');
      await saveButton.click();
      
      // Wait for response
      await page.waitForTimeout(5000);
      
      // Check for errors
      const errorAlert = await page.locator('[role="alert"], .MuiAlert-message').first();
      if (await errorAlert.count() > 0) {
        const errorText = await errorAlert.textContent();
        console.log('Error/Alert:', errorText);
      }
      
      // Check if dialog is still open
      const dialogTitle = await page.locator('h2:has-text("Compute")');
      if (await dialogTitle.count() === 0) {
        console.log('✓ Dialog closed - compute likely saved successfully');
      } else {
        console.log('✗ Dialog still open - there may be an error');
      }
      
    } else {
      console.log('Could not find "Add Compute" menu option');
      
      // Debug: show what menu items are available
      const menuItems = await page.locator('[role="menuitem"]').all();
      console.log('Available menu items:');
      for (const item of menuItems) {
        const text = await item.textContent();
        console.log(`  - ${text}`);
      }
    }

    console.log('\nTest complete. Browser will stay open for 30 seconds...');
    await page.waitForTimeout(30000);

  } catch (error) {
    console.error('Test error:', error);
  } finally {
    await browser.close();
  }
})();