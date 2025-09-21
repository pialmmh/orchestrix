/**
 * Test file to trigger console redirect
 */

// Test console redirect
const testConsoleRedirect = () => {
  console.log('[TestConsoleRedirect] Testing console redirect feature');
  console.info('[TestConsoleRedirect] This is an info message');
  console.warn('[TestConsoleRedirect] This is a warning message');
  console.error('[TestConsoleRedirect] This is an error message');
  console.debug('[TestConsoleRedirect] This is a debug message');

  // Test with objects
  console.log('[TestConsoleRedirect] Object test:', {
    feature: 'Console Redirect',
    profile: 'development',
    timestamp: new Date().toISOString()
  });

  // Test with arrays
  console.log('[TestConsoleRedirect] Array test:', [1, 2, 3, 'test', { nested: true }]);
};

// Run test immediately when module loads
setTimeout(() => {
  testConsoleRedirect();
  console.log('[TestConsoleRedirect] Console redirect test completed');
}, 2000);

export default testConsoleRedirect;