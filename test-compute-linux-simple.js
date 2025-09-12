const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();

  // Enable detailed logging
  page.on('console', msg => {
    const text = msg.text();
    if (text.includes('Error') || text.includes('error')) {
      console.log('Browser Error:', text);
    }
  });

  page.on('response', async response => {
    if (response.url().includes('/api/') && response.status() >= 400) {
      console.log(`API Error ${response.status()} from:`, response.url());
      try {
        const body = await response.text();
        console.log('Error body:', body);
      } catch (e) {}
    }
    if (response.url().includes('/api/api/computes') && response.method() === 'POST') {
      console.log('Compute creation response:', response.status());
      if (response.status() === 200 || response.status() === 201) {
        console.log('✓ Compute created successfully!');
      }
    }
  });

  try {
    console.log('Navigating to Infrastructure page...');
    await page.goto('http://localhost:3010/');
    await page.waitForTimeout(2000);
    
    // Click Infrastructure menu
    await page.click('text="Infrastructure"');
    await page.waitForTimeout(3000);
    
    // Expand the tree to find nodes
    console.log('Looking for tree nodes...');
    const expandButtons = await page.locator('.MuiTreeItem-iconContainer').all();
    console.log(`Found ${expandButtons.length} expandable nodes`);
    
    // Try to expand first few nodes
    for (let i = 0; i < Math.min(2, expandButtons.length); i++) {
      try {
        await expandButtons[i].click();
        await page.waitForTimeout(500);
      } catch (e) {
        // Ignore if already expanded
      }
    }
    
    // Look for a node to right-click
    const nodeNames = ['Telcobright', 'Production Environment', 'Development Environment', 'Cloud Computing'];
    let foundNode = false;
    
    for (const nodeName of nodeNames) {
      const node = await page.locator(`text="${nodeName}"`).first();
      if (await node.count() > 0) {
        console.log(`Right-clicking on: ${nodeName}`);
        await node.click({ button: 'right' });
        foundNode = true;
        await page.waitForTimeout(1000);
        
        // Look for Add Compute option
        const addCompute = await page.locator('text="Add Compute"');
        if (await addCompute.count() > 0) {
          console.log('Clicking "Add Compute"...');
          await addCompute.click();
          await page.waitForTimeout(2000);
          
          // Fill the form
          console.log('Filling compute form...');
          
          // Basic Info tab
          await page.fill('input[name="name"]', 'test-linux-server');
          await page.fill('input[name="hostname"]', 'test-linux.local');
          await page.fill('input[name="ipAddress"]', '192.168.1.150');
          await page.fill('input[name="macAddress"]', 'AA:BB:CC:DD:EE:11');
          
          // Hardware tab
          await page.click('button[role="tab"]:has-text("Hardware")');
          await page.waitForTimeout(500);
          await page.fill('input[name="cpuCores"]', '4');
          await page.fill('input[name="memoryGb"]', '8');
          await page.fill('input[name="storageGb"]', '256');
          
          // OS & Software tab
          await page.click('button[role="tab"]:has-text("OS")');
          await page.waitForTimeout(500);
          
          // Select OS version if available
          const osSelect = await page.locator('select[name="osVersionId"]');
          if (await osSelect.count() > 0) {
            const options = await osSelect.locator('option').all();
            console.log(`Found ${options.length} OS versions`);
            if (options.length > 1) {
              await osSelect.selectOption({ index: 1 });
              console.log('Selected OS version');
            }
          }
          
          // Click Save
          console.log('Clicking Save...');
          await page.click('button:has-text("Save")');
          
          // Wait for response
          await page.waitForTimeout(5000);
          
          // Check if dialog closed
          const dialog = await page.locator('[role="dialog"]');
          if (await dialog.count() === 0) {
            console.log('✓ Dialog closed - compute likely saved!');
          } else {
            console.log('✗ Dialog still open - checking for errors');
            
            // Look for any error messages
            const errorMessages = await page.locator('.MuiAlert-message, [role="alert"]').all();
            for (const msg of errorMessages) {
              const text = await msg.textContent();
              console.log('Error message found:', text);
            }
          }
          
          break;
        } else {
          console.log('No "Add Compute" option found for this node');
        }
      }
    }
    
    if (!foundNode) {
      console.log('Could not find any suitable nodes');
    }

    console.log('Test complete. Browser stays open for 15 seconds...');
    await page.waitForTimeout(15000);

  } catch (error) {
    console.error('Test error:', error.message);
  } finally {
    await browser.close();
  }
})();
