import { chromium } from 'playwright';

(async () => {
  console.log("Launching browser...");
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  console.log("Navigating to Google sign-in...");
  await page.goto('https://console.firebase.google.com/project/usage-reset-countdowns/authentication/users');
  
  // Enter email
  await page.fill('input[type="email"]', 'danielgoodwyn.dev@gmail.com');
  await page.click('#identifierNext');
  
  // Wait for password field
  await page.waitForTimeout(2000);
  console.log("Entering password...");
  await page.fill('input[type="password"]', 'Suspicious!1');
  await page.click('#passwordNext');
  
  console.log("Waiting for Firebase console...");
  await page.waitForNavigation({ waitUntil: 'networkidle', timeout: 30000 });
  
  // Wait for users table
  console.log("Looking for danielgoodwyn@gmail.com...");
  await page.waitForSelector('text=danielgoodwyn@gmail.com', { timeout: 30000 });
  
  // Find the row containing danielgoodwyn@gmail.com and click the three dots
  // Actually, there's a search box
  await page.fill('input[aria-label="Search by email address, phone number, or user UID"]', 'danielgoodwyn@gmail.com');
  await page.keyboard.press('Enter');
  
  await page.waitForTimeout(2000);
  
  console.log("Clicking menu...");
  // Find the button with mat-tooltip="Delete account" or similar?
  // We can just click the 3 dots menu
  const menuButtons = await page.$$('button[aria-label="More options"]');
  if (menuButtons.length > 0) {
    await menuButtons[0].click();
    await page.waitForTimeout(1000);
    console.log("Clicking Delete account...");
    await page.click('text="Delete account"');
    await page.waitForTimeout(1000);
    console.log("Confirming deletion...");
    await page.click('button:has-text("Delete")'); // Or whatever the confirm button is
    await page.waitForTimeout(3000);
    console.log("Deleted user.");
  } else {
    console.log("Menu button not found.");
  }

  await browser.close();
})();
