const { chromium } = require('playwright');

(async () => {
  console.log('Starting Playwright test...');
  
  // Launch browser
  const browser = await chromium.launch({ headless: false, slowMo: 1000 });
  const context = await browser.newContext();
  const page = await context.newPage();
  
  try {
    console.log('Navigating to application...');
    await page.goto('http://localhost:3010');
    
    // Wait for the page to load
    await page.waitForTimeout(3000);
    
    console.log('Looking for navigation to Infrastructure...');
    // Try to find and click on Infrastructure link
    try {
      await page.click('text=Infrastructure', { timeout: 5000 });
      console.log('Clicked Infrastructure navigation');
    } catch (e) {
      console.log('Infrastructure navigation not found, trying alternative selectors...');
      await page.click('[href*="infrastructure"]', { timeout: 5000 });
    }
    
    // Wait for the infrastructure page to load
    await page.waitForTimeout(3000);
    
    console.log('Checking page title and content...');
    const pageTitle = await page.textContent('h1, h2, h3, [data-testid="page-title"]').catch(() => 'No title found');
    console.log('Page title:', pageTitle);
    
    // Check for Organization/Partners toggle
    console.log('Looking for tenant toggle buttons...');
    const orgButton = await page.locator('text=Organization').first().isVisible().catch(() => false);
    const partnersButton = await page.locator('text=Partners').first().isVisible().catch(() => false);
    console.log('Organization button visible:', orgButton);
    console.log('Partners button visible:', partnersButton);
    
    // Check for tree structure
    console.log('Looking for tree view...');
    const treeView = await page.locator('[role="tree"], .MuiTreeView-root, [class*="tree"]').first().isVisible().catch(() => false);
    console.log('Tree view visible:', treeView);
    
    // Look for tree items
    const treeItems = await page.locator('[role="treeitem"], .MuiTreeItem-root, [class*="tree-item"]').count().catch(() => 0);
    console.log('Number of tree items found:', treeItems);
    
    // Check for specific hierarchy items
    const hierarchyItems = [
      'Telcobright',
      'Production Environment', 
      'Development Environment',
      'Virtual',
      'Moderate'
    ];
    
    console.log('Checking for hierarchy items:');
    for (const item of hierarchyItems) {
      const found = await page.locator(`text=${item}`).first().isVisible().catch(() => false);
      console.log(`- ${item}: ${found ? '✓ Found' : '✗ Not found'}`);
    }
    
    // Try to expand tree nodes if they exist
    console.log('Attempting to expand tree nodes...');
    try {
      // Click on expand buttons
      const expandButtons = await page.locator('[aria-label*="expand"], [data-testid*="expand"], .MuiTreeItem-iconContainer').all();
      console.log(`Found ${expandButtons.length} expand buttons`);
      
      for (let i = 0; i < Math.min(expandButtons.length, 5); i++) {
        try {
          await expandButtons[i].click();
          await page.waitForTimeout(1000);
          console.log(`Expanded node ${i + 1}`);
        } catch (e) {
          console.log(`Failed to expand node ${i + 1}:`, e.message);
        }
      }
    } catch (e) {
      console.log('No expandable nodes found or error expanding:', e.message);
    }
    
    // Check if data stops at Virtual -> Moderate level
    console.log('Checking data hierarchy depth after expansion...');
    await page.waitForTimeout(2000);
    
    const finalTreeItems = await page.locator('[role="treeitem"], .MuiTreeItem-root, [class*="tree-item"]').count().catch(() => 0);
    console.log('Total tree items after expansion:', finalTreeItems);
    
    // Take a screenshot for investigation
    await page.screenshot({ path: 'infrastructure-page-test.png', fullPage: true });
    console.log('Screenshot saved as infrastructure-page-test.png');
    
    // Check for CRUD buttons on the right panel
    console.log('Looking for CRUD buttons and right panel...');
    const rightPanel = await page.locator('[data-testid="right-panel"], .right-panel, [class*="right"], [class*="details"]').first().isVisible().catch(() => false);
    console.log('Right panel visible:', rightPanel);
    
    const addButton = await page.locator('button:has-text("Add"), [aria-label*="add"], [data-testid*="add"]').first().isVisible().catch(() => false);
    const editButton = await page.locator('button:has-text("Edit"), [aria-label*="edit"], [data-testid*="edit"]').first().isVisible().catch(() => false);
    const deleteButton = await page.locator('button:has-text("Delete"), [aria-label*="delete"], [data-testid*="delete"]').first().isVisible().catch(() => false);
    
    console.log('CRUD buttons:');
    console.log('- Add button:', addButton ? '✓ Found' : '✗ Not found');
    console.log('- Edit button:', editButton ? '✓ Found' : '✗ Not found'); 
    console.log('- Delete button:', deleteButton ? '✓ Found' : '✗ Not found');
    
    console.log('Test completed successfully!');
    
  } catch (error) {
    console.error('Test failed:', error.message);
    await page.screenshot({ path: 'infrastructure-page-error.png', fullPage: true });
    console.log('Error screenshot saved as infrastructure-page-error.png');
  } finally {
    await browser.close();
  }
})();