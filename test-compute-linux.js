const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ 
    headless: false,
    slowMo: 500 // Slow down for visibility
  });
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    console.log('Navigating to application...');
    await page.goto('http://localhost:3010/resources/compute');
    
    // Wait for page to load
    await page.waitForTimeout(2000);
    
    console.log('Clicking Add Resource button...');
    // Click Add Resource button
    await page.click('button:has-text("Add Resource")');
    
    // Wait for dialog to open
    await page.waitForTimeout(1000);
    
    console.log('Filling Basic Info...');
    // Fill Basic Info tab
    await page.fill('input[name="name"], label:has-text("Name") + * input', 'Ubuntu-Test-Server');
    await page.fill('input[name="hostname"], label:has-text("Hostname") + * input', 'ubuntu-test.example.com');
    await page.fill('label:has-text("IP Address") + * input', '192.168.100.50');
    
    // Select Node Type
    await page.click('label:has-text("Node Type") + * [role="button"]');
    await page.click('li[role="option"]:has-text("Dedicated Server")');
    
    console.log('Switching to OS & Software tab...');
    // Click OS & Software tab
    await page.click('button[role="tab"]:has-text("OS & Software")');
    await page.waitForTimeout(500);
    
    console.log('Selecting Linux OS...');
    // Select Operating System
    await page.click('label:has-text("Operating System") + * [role="button"]');
    await page.waitForTimeout(500);
    
    // Look for Ubuntu Server option
    const ubuntuOption = await page.locator('li[role="option"]:has-text("Ubuntu Server 22.04 LTS amd64")').first();
    if (await ubuntuOption.isVisible()) {
      await ubuntuOption.click();
      console.log('Selected Ubuntu Server 22.04 LTS amd64');
    } else {
      // Try alternative selection
      await page.click('li[role="option"]:has-text("Ubuntu")').first();
      console.log('Selected first Ubuntu option');
    }
    
    // Fill kernel version
    await page.fill('label:has-text("Kernel Version") + * input', '5.15.0-76-generic');
    
    console.log('Switching to Hardware tab...');
    // Click Hardware tab
    await page.click('button[role="tab"]:has-text("Hardware")');
    await page.waitForTimeout(500);
    
    // Fill hardware info
    await page.fill('label:has-text("CPU Cores") + * input', '8');
    await page.fill('label:has-text("Memory (GB)") + * input', '16');
    await page.fill('label:has-text("Disk (GB)") + * input', '500');
    
    console.log('Saving compute...');
    // Click Save button
    await page.click('button:has-text("Save")');
    
    // Wait for success
    await page.waitForTimeout(2000);
    
    console.log('Compute created successfully!');
    
    // Verify the compute was created
    const computeRow = await page.locator('tr:has-text("Ubuntu-Test-Server")').first();
    if (await computeRow.isVisible()) {
      console.log('✓ Compute appears in the list');
      
      // Get the OS info from the row
      const cells = await computeRow.locator('td').all();
      for (let i = 0; i < cells.length; i++) {
        const text = await cells[i].textContent();
        console.log(`Cell ${i}: ${text}`);
      }
    } else {
      console.log('✗ Compute not found in list');
    }
    
  } catch (error) {
    console.error('Error during test:', error);
    
    // Take screenshot on error
    await page.screenshot({ path: 'error-compute-linux.png' });
    console.log('Screenshot saved as error-compute-linux.png');
  }
  
  // Keep browser open for inspection
  console.log('Test complete. Browser will stay open for 5 seconds...');
  await page.waitForTimeout(5000);
  
  await browser.close();
})();