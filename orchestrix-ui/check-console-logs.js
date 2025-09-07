const { chromium } = require('playwright');

async function testConsole() {
  const browser = await chromium.launch({
    headless: false,
    slowMo: 2000
  });
  
  const context = await browser.newContext();
  const page = await context.newPage();
  
  // Capture console logs
  const logs = [];
  page.on('console', msg => {
    const logEntry = `[${msg.type().toUpperCase()}] ${msg.text()}`;
    console.log(logEntry);
    logs.push(logEntry);
  });
  
  // Also capture errors
  page.on('pageerror', error => {
    console.log(`[PAGE ERROR] ${error.message}`);
    logs.push(`[PAGE ERROR] ${error.message}`);
  });
  
  console.log('Navigating to app...');
  await page.goto('http://localhost:3010');
  
  console.log('Waiting for page to load...');
  await page.waitForTimeout(3000);
  
  console.log('Clicking Infrastructure navigation...');
  await page.click('[href="/manage-infrastructure"]');
  
  console.log('Waiting for Infrastructure page to load and API calls...');
  await page.waitForTimeout(5000);
  
  console.log('\n=== CAPTURED LOGS ===');
  logs.forEach(log => console.log(log));
  
  await browser.close();
}

testConsole().catch(console.error);