const { test, expect, chromium } = require('@playwright/test');

test('Infrastructure page should load data', async () => {
  const browser = await chromium.launch({
    headless: false,
    devtools: true
  });
  const context = await browser.newContext();
  const page = await context.newPage();

  // Enable console logging to see what's happening
  page.on('console', msg => {
    console.log(`Browser console [${msg.type()}]: ${msg.text()}`);
  });

  page.on('pageerror', err => {
    console.error('Page error:', err.message);
  });

  // Navigate to infrastructure page
  await page.goto('http://localhost:3010/infrastructure');

  // Wait for the page to be loaded
  await page.waitForLoadState('networkidle');

  // Wait up to 30 seconds for data to load
  console.log('Waiting for infrastructure data to load...');

  try {
    // Wait for either "Loading" text to disappear or data to appear
    await page.waitForFunction(() => {
      const loadingText = document.body.innerText.includes('Loading infrastructure data');
      const hasData = document.querySelector('.node-item') ||
                     document.querySelector('[data-testid="infrastructure-node"]') ||
                     document.body.innerText.includes('Telcobright') ||
                     document.querySelector('svg'); // D3 visualization
      return !loadingText || hasData;
    }, { timeout: 30000 });

    // Additional wait to ensure rendering completes
    await page.waitForTimeout(2000);

    // Check what's visible on the page
    const pageContent = await page.innerText('body');
    console.log('Page content snippet:', pageContent.substring(0, 500));

    // Check if "Loading" text is still present
    const hasLoadingText = pageContent.includes('Loading infrastructure data');
    console.log('Has loading text:', hasLoadingText);

    // Check for any data elements
    const hasOrganizationTab = pageContent.includes('Organization');
    const hasPartnersTab = pageContent.includes('Partners');
    const hasTelcobright = pageContent.includes('Telcobright');
    const hasSelectNode = pageContent.includes('Select a node');

    console.log('UI State:');
    console.log('- Organization tab:', hasOrganizationTab);
    console.log('- Partners tab:', hasPartnersTab);
    console.log('- Has Telcobright:', hasTelcobright);
    console.log('- Has "Select a node":', hasSelectNode);

    // Take a screenshot for debugging
    await page.screenshot({ path: 'infrastructure-test-result.png', fullPage: true });
    console.log('Screenshot saved to infrastructure-test-result.png');

    // Test assertions
    expect(hasLoadingText, 'Loading text should not be present').toBe(false);
    expect(hasOrganizationTab || hasPartnersTab, 'Should have Organization or Partners tab').toBe(true);

    console.log('✓ Infrastructure data loaded successfully');

  } catch (error) {
    console.error('Test failed:', error.message);
    await page.screenshot({ path: 'infrastructure-test-error.png', fullPage: true });
    throw error;
  } finally {
    await browser.close();
  }
});