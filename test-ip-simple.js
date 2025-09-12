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
    console.log('Console:', msg.type(), msg.text());
  });

  // Monitor network failures
  page.on('requestfailed', request => {
    if (!request.url().includes('ws://')) { // Ignore WebSocket errors
      console.log('Request failed:', request.url(), request.failure().errorText);
    }
  });

  try {
    // First, let's create a compute via API
    console.log('Creating test compute via API...');
    const response = await fetch('http://localhost:8090/api/api/computes', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: 'TestCompute',
        hostname: 'test-compute',
        status: 'ACTIVE',
        type: 'PHYSICAL'
      })
    });
    
    const compute = await response.json();
    console.log('Created compute with ID:', compute.id);

    // Now navigate to the compute edit page
    console.log('Navigating to compute edit page...');
    await page.goto(`http://localhost:3005/resources/compute`);
    await page.waitForTimeout(2000);

    // Click edit on the first compute
    console.log('Clicking edit button...');
    const editButton = page.locator('button[title="Edit"]').first();
    await editButton.waitFor({ state: 'visible', timeout: 5000 });
    await editButton.click();
    await page.waitForTimeout(2000);

    // Click on Network tab
    console.log('Clicking Network tab...');
    const networkTab = page.locator('button[role="tab"]:has-text("Network")');
    await networkTab.waitFor({ state: 'visible', timeout: 5000 });
    await networkTab.click();
    await page.waitForTimeout(2000);

    // Look for Add IP Address button
    console.log('Looking for Add IP Address button...');
    const addIpButton = page.locator('button:has-text("Add IP Address")');
    const isVisible = await addIpButton.isVisible();
    console.log('Add IP Address button visible:', isVisible);

    if (isVisible) {
      // Take a screenshot before clicking
      await page.screenshot({ path: 'before-click.png' });
      
      console.log('Clicking Add IP Address button...');
      
      // Try to click and catch any errors
      try {
        await addIpButton.click();
        console.log('Click succeeded');
      } catch (clickError) {
        console.log('Click failed:', clickError.message);
        
        // Try JavaScript click as fallback
        console.log('Trying JavaScript click...');
        await page.evaluate(() => {
          const button = document.querySelector('button:has-text("Add IP Address")') || 
                        Array.from(document.querySelectorAll('button')).find(b => b.textContent.includes('Add IP Address'));
          if (button) {
            button.click();
            console.log('JS click executed');
          } else {
            console.log('Button not found via JS');
          }
        });
      }
      
      await page.waitForTimeout(2000);

      // Check if dialog opened
      const dialog = page.locator('div[role="dialog"]');
      const dialogVisible = await dialog.isVisible();
      console.log('Dialog visible after click:', dialogVisible);

      // Take screenshot after click
      await page.screenshot({ path: 'after-click.png' });

      if (!dialogVisible) {
        // Check for any error messages on the page
        const alerts = await page.locator('.MuiAlert-message').allTextContents();
        if (alerts.length > 0) {
          console.log('Alert messages found:', alerts);
        }
        
        // Check button state
        const isDisabled = await addIpButton.isDisabled();
        console.log('Button disabled:', isDisabled);
        
        // Get button HTML for debugging
        const buttonHtml = await addIpButton.evaluate(el => el.outerHTML);
        console.log('Button HTML:', buttonHtml);
      }
    }

    // Wait a bit to observe
    await page.waitForTimeout(3000);

  } catch (error) {
    console.error('Error during test:', error);
    await page.screenshot({ path: 'error-screenshot.png' });
  }

  await browser.close();
})();