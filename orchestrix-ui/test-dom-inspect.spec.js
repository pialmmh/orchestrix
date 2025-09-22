const { test, expect } = require('@playwright/test');

test('Inspect Infrastructure DOM and data', async ({ page }) => {
  // Navigate to the infrastructure page
  await page.goto('http://localhost:3010/infrastructure');

  // Wait for the page to load
  await page.waitForTimeout(3000);

  // Click on Organization tab if not already selected
  const orgTab = page.locator('button:has-text("Organization")');
  if (await orgTab.isVisible()) {
    await orgTab.click();
    await page.waitForTimeout(2000);
  }

  // Get all the DOM content
  console.log('\n=== DOM INSPECTION ===\n');

  // Check what's in the main content area
  const mainContent = await page.locator('.MuiBox-root').filter({ hasText: 'Infrastructure' }).first();
  const contentHTML = await mainContent.innerHTML();
  console.log('Main content HTML structure (first 1000 chars):', contentHTML.substring(0, 1000));

  // Look for any tree or list elements
  const treeElements = await page.locator('[role="tree"], [role="treeitem"], .tree-node, .MuiTreeView-root, .MuiTreeItem-root').all();
  console.log(`\nFound ${treeElements.length} tree-related elements`);

  // Look for any text containing "Telcobright"
  const telcobrightElements = await page.locator('*:has-text("Telcobright")').all();
  console.log(`\nFound ${telcobrightElements.length} elements containing "Telcobright"`);

  for (let i = 0; i < Math.min(3, telcobrightElements.length); i++) {
    const elem = telcobrightElements[i];
    const tagName = await elem.evaluate(el => el.tagName);
    const className = await elem.evaluate(el => el.className);
    const text = await elem.textContent();
    console.log(`  Element ${i}: <${tagName} class="${className}"> - Text: "${text}"`);
  }

  // Check if there's a loading indicator
  const loadingElements = await page.locator('*:has-text("Loading"), *:has-text("loading"), .MuiCircularProgress-root').all();
  console.log(`\nFound ${loadingElements.length} loading indicators`);

  if (loadingElements.length > 0) {
    for (let elem of loadingElements) {
      const text = await elem.textContent();
      console.log(`  Loading element text: "${text}"`);
    }
  }

  // Check what's in the store by evaluating JavaScript
  console.log('\n=== CHECKING STORE DATA ===\n');

  const storeData = await page.evaluate(() => {
    // Try to access the store from window or React DevTools
    const result = {};

    // Check if there's a global store
    if (window.__STORE__) {
      result.globalStore = JSON.stringify(window.__STORE__, null, 2);
    }

    // Try to find React Fiber and extract props/state
    const findReactFiber = (dom) => {
      const key = Object.keys(dom).find(key => key.startsWith('__reactFiber'));
      return dom[key];
    };

    const infraElements = document.querySelectorAll('[class*="Infrastructure"], [class*="infra"]');
    if (infraElements.length > 0) {
      const fiber = findReactFiber(infraElements[0]);
      if (fiber && fiber.memoizedProps) {
        result.componentProps = JSON.stringify(fiber.memoizedProps, null, 2);
      }
      if (fiber && fiber.memoizedState) {
        result.componentState = JSON.stringify(fiber.memoizedState, null, 2);
      }
    }

    // Check localStorage for any stored data
    result.localStorage = {};
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key.includes('infra') || key.includes('partner') || key.includes('store')) {
        result.localStorage[key] = localStorage.getItem(key);
      }
    }

    return result;
  });

  if (storeData.globalStore) {
    console.log('Global store data:', storeData.globalStore.substring(0, 500));
  }
  if (storeData.componentProps) {
    console.log('Component props:', storeData.componentProps.substring(0, 500));
  }
  if (storeData.componentState) {
    console.log('Component state:', storeData.componentState.substring(0, 500));
  }
  if (Object.keys(storeData.localStorage).length > 0) {
    console.log('LocalStorage data:', JSON.stringify(storeData.localStorage, null, 2));
  }

  // Look for any data attributes
  console.log('\n=== DATA ATTRIBUTES ===\n');
  const elementsWithData = await page.locator('[data-partner], [data-cloud], [data-region], [data-infrastructure]').all();
  console.log(`Found ${elementsWithData.length} elements with data attributes`);

  // Check network requests to see if data was fetched
  console.log('\n=== CHECKING NETWORK ACTIVITY ===\n');

  // Listen for WebSocket messages
  page.on('websocket', ws => {
    console.log('WebSocket created:', ws.url());
    ws.on('framesent', event => console.log('WS sent:', event.payload));
    ws.on('framereceived', event => console.log('WS received:', event.payload?.substring(0, 200)));
  });

  // Take a screenshot for visual inspection
  await page.screenshot({ path: 'infrastructure-dom-inspect.png', fullPage: true });
  console.log('\nScreenshot saved as infrastructure-dom-inspect.png');

  // Try clicking on Telcobright if visible to expand it
  const telcobrightNode = page.locator('text="Telcobright"').first();
  if (await telcobrightNode.isVisible()) {
    console.log('\n=== CLICKING TELCOBRIGHT NODE ===\n');
    await telcobrightNode.click();
    await page.waitForTimeout(2000);

    // Check if any new elements appeared
    const afterClickContent = await mainContent.innerHTML();
    console.log('Content after click (first 1000 chars):', afterClickContent.substring(0, 1000));

    // Look for cloud, region, datacenter elements
    const infraElements = await page.locator('*:has-text("Cloud"), *:has-text("Region"), *:has-text("Datacenter"), *:has-text("aws")').all();
    console.log(`\nFound ${infraElements.length} infrastructure elements after click`);

    for (let elem of infraElements.slice(0, 5)) {
      const text = await elem.textContent();
      console.log(`  Infrastructure element: "${text}"`);
    }
  }
});