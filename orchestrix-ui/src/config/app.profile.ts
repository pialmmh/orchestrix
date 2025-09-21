/**
 * Master configuration file for profile selection
 * This file determines which configuration profile is active
 *
 * Available profiles:
 * - development: Local development with full debugging
 * - staging: Pre-production environment with moderate debugging
 * - production: Production environment with minimal debugging
 */

export type ConfigProfile = 'development' | 'staging' | 'production';

/**
 * CHANGE THIS VALUE TO SWITCH PROFILES
 * This is the master switch for configuration profiles
 */
export const ACTIVE_PROFILE: ConfigProfile = 'development';

/**
 * Profile detection priority:
 * 1. Environment variable REACT_APP_PROFILE
 * 2. NODE_ENV mapping
 * 3. ACTIVE_PROFILE constant above
 */
export function getActiveProfile(): ConfigProfile {
  // First priority: explicit profile from environment
  const envProfile = process.env.REACT_APP_PROFILE;
  if (envProfile && ['development', 'staging', 'production'].includes(envProfile)) {
    return envProfile as ConfigProfile;
  }

  // Second priority: derive from NODE_ENV
  const nodeEnv = process.env.NODE_ENV;
  if (nodeEnv === 'production') {
    return 'production';
  } else if (nodeEnv === 'test') {
    return 'staging';
  }

  // Default: use the configured active profile
  return ACTIVE_PROFILE;
}

// Export the determined profile
export const activeProfile = getActiveProfile();

console.log(`🎯 Active Configuration Profile: ${activeProfile}`);