const { test, expect } = require('@playwright/test');

test('Check WebSocket data reception', async ({ page }) => {
  // Set up console event listener to capture logs
  const consoleLogs = [];
  page.on('console', msg => {
    const text = msg.text();
    if (text.includes('[StoreWebSocket]') || text.includes('RECEIVED DATA') || text.includes('CLOUDS DATA') || text.includes('infrastructure')) {
      consoleLogs.push(text);
      console.log('UI Console:', text);
    }
  });

  // Navigate to the infrastructure page
  await page.goto('http://localhost:3010/infrastructure/organization');

  // Wait for the page to load
  await page.waitForTimeout(5000);

  // Print all WebSocket related logs
  console.log('\n=== WEBSOCKET LOGS FROM UI ===\n');
  consoleLogs.forEach(log => {
    console.log(log);
  });

  // Check what data is in the DOM
  const telcobrightNode = await page.locator('text="Telcobright"').first();
  if (await telcobrightNode.isVisible()) {
    console.log('\nTelcobright node is visible');

    // Try to expand it
    await telcobrightNode.click();
    await page.waitForTimeout(2000);

    // Check for any cloud/datacenter elements
    const cloudElements = await page.locator('text=/aws|google|azure|cloud|Cloud/i').all();
    console.log(`\nFound ${cloudElements.length} cloud-related elements`);

    for (let elem of cloudElements.slice(0, 5)) {
      const text = await elem.textContent();
      console.log(`  Cloud element: "${text}"`);
    }
  }

  // Check the actual component state using React DevTools
  const infrastructureData = await page.evaluate(() => {
    // Try to access the store through window
    if (window.__STORES__) {
      const orgStore = window.__STORES__.organizationInfraStore;
      if (orgStore) {
        return {
          isLoading: orgStore.isLoading,
          partners: orgStore.partners ? orgStore.partners.length : 0,
          firstPartner: orgStore.partners && orgStore.partners[0] ? {
            name: orgStore.partners[0].name,
            hasInfra: !!orgStore.partners[0].infrastructure,
            hasClouds: !!orgStore.partners[0].clouds,
            cloudsCount: orgStore.partners[0].clouds ? orgStore.partners[0].clouds.length : 0
          } : null
        };
      }
    }
    return null;
  });

  console.log('\n=== STORE STATE ===');
  console.log(JSON.stringify(infrastructureData, null, 2));
});