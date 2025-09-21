import axios from 'axios';

console.log('[TEST] axios:', axios);
console.log('[TEST] typeof axios:', typeof axios);
console.log('[TEST] axios.create:', axios.create);
console.log('[TEST] axios.default:', axios.default);

// Make axios available globally for Stellar library - MUST be done before any imports that use it
if (typeof window !== 'undefined') {
  window.axios = axios;
  console.log('[TEST] window.axios set successfully');
}

export default axios;