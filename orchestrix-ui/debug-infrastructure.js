const { chromium } = require('playwright');

async function debugInfrastructure() {
  const browser = await chromium.launch({ headless: false, slowMo: 1000 });
  const context = await browser.newContext();
  const page = await context.newPage();

  console.log('🚀 Starting infrastructure debugging...');

  try {
    // Navigate to the application
    console.log('📍 Navigating to http://localhost:3010');
    await page.goto('http://localhost:3010');
    await page.waitForTimeout(2000);

    // Check if we need to login or navigate
    const currentUrl = page.url();
    console.log('📍 Current URL:', currentUrl);

    // Look for navigation to Infrastructure
    try {
      console.log('🔍 Looking for Infrastructure navigation...');
      
      // Try different selectors for Infrastructure link
      const infraSelectors = [
        'text="Infrastructure"',
        '[href*="infrastructure"]',
        'a:has-text("Infrastructure")',
        '[data-testid*="infrastructure"]'
      ];

      let infraLink = null;
      for (const selector of infraSelectors) {
        try {
          infraLink = await page.locator(selector).first();
          if (await infraLink.isVisible()) {
            console.log(`✅ Found infrastructure link with selector: ${selector}`);
            break;
          }
        } catch (e) {
          console.log(`❌ Selector failed: ${selector}`);
        }
      }

      if (infraLink && await infraLink.isVisible()) {
        console.log('🖱️ Clicking Infrastructure link...');
        await infraLink.click();
        await page.waitForTimeout(2000);
      } else {
        // Try direct navigation
        console.log('🔄 Trying direct navigation to infrastructure page...');
        await page.goto('http://localhost:3010/infrastructure');
        await page.waitForTimeout(2000);
      }

    } catch (navError) {
      console.log('⚠️ Navigation failed, trying direct URL...');
      await page.goto('http://localhost:3010/infrastructure');
      await page.waitForTimeout(2000);
    }

    // Check page content
    console.log('📍 Current URL after navigation:', page.url());
    const title = await page.title();
    console.log('📄 Page title:', title);

    // Look for the infrastructure page elements
    console.log('🔍 Checking for infrastructure page elements...');
    
    const pageText = await page.textContent('body');
    if (pageText.includes('Manage Infrastructure')) {
      console.log('✅ Found "Manage Infrastructure" text');
    } else {
      console.log('❌ "Manage Infrastructure" text not found');
    }

    // Check for the specific error message
    if (pageText.includes('No infrastructure data available')) {
      console.log('❌ Found error: "No infrastructure data available"');
    }

    // Check console logs
    console.log('🔍 Checking browser console logs...');
    page.on('console', msg => {
      console.log(`🖥️ BROWSER: [${msg.type()}] ${msg.text()}`);
    });

    // Check network requests
    console.log('🔍 Monitoring network requests...');
    page.on('response', response => {
      if (response.url().includes('api') || response.url().includes('infrastructure')) {
        console.log(`🌐 API Response: ${response.status()} ${response.url()}`);
      }
    });

    // Wait for any network requests to complete
    await page.waitForTimeout(3000);

    // Check for tree view and data
    console.log('🔍 Checking for tree view elements...');
    
    const treeSelectors = [
      '[role="tree"]',
      '.MuiTreeView-root',
      '[data-testid*="tree"]',
      'text="ORGANIZATION"',
      'text="PARTNERS"'
    ];

    for (const selector of treeSelectors) {
      try {
        const element = page.locator(selector);
        if (await element.isVisible()) {
          console.log(`✅ Found tree element: ${selector}`);
          const text = await element.textContent();
          console.log(`📝 Tree content: ${text.substring(0, 200)}...`);
        }
      } catch (e) {
        console.log(`❌ Tree selector failed: ${selector}`);
      }
    }

    // Check API endpoints
    console.log('🔍 Testing API endpoints directly...');
    
    const apiEndpoints = [
      'http://localhost:8090/api/infrastructure/tree',
      'http://localhost:8090/api/partners',
      'http://localhost:8090/api/compute',
      'http://localhost:8090/api/environments'
    ];

    for (const endpoint of apiEndpoints) {
      try {
        const response = await page.evaluate(async (url) => {
          const res = await fetch(url);
          return {
            status: res.status,
            ok: res.ok,
            data: res.ok ? await res.text() : await res.text()
          };
        }, endpoint);
        
        console.log(`🌐 API ${endpoint}: ${response.status} ${response.ok ? 'OK' : 'FAILED'}`);
        if (!response.ok) {
          console.log(`❌ Error: ${response.data}`);
        } else {
          const dataPreview = response.data.substring(0, 200);
          console.log(`📊 Data preview: ${dataPreview}...`);
        }
      } catch (apiError) {
        console.log(`❌ API Error ${endpoint}:`, apiError.message);
      }
    }

    // Take a screenshot for debugging
    await page.screenshot({ path: 'infrastructure-debug.png', fullPage: true });
    console.log('📸 Screenshot saved as infrastructure-debug.png');

    // Wait longer to observe
    console.log('⏳ Waiting 10 seconds for observation...');
    await page.waitForTimeout(10000);

  } catch (error) {
    console.error('❌ Error during debugging:', error);
  } finally {
    console.log('🏁 Debugging completed');
    await browser.close();
  }
}

debugInfrastructure().catch(console.error);