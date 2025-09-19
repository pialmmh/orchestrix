#!/usr/bin/env node

/**
 * Development server with auto-sync and restart
 * Watches for changes in unified-store and automatically syncs and restarts
 */

const { spawn } = require('child_process');
const chokidar = require('chokidar');
const path = require('path');
const fs = require('fs');

// Parse command line arguments
const args = process.argv.slice(2);
const useXState = args.includes('--xstate');
const serverFile = useXState ? 'serverWithXState.js' : 'server.js';

// Paths
const SOURCE_DIR = path.resolve(__dirname, '../src/unified-store');
const SYNC_SCRIPT = path.resolve(__dirname, '../scripts/sync-store.js');

// Colors for console output
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  red: '\x1b[31m',
  cyan: '\x1b[36m'
};

let serverProcess = null;
let syncTimeout = null;

// Log with color
function log(message, color = 'reset') {
  console.log(`${colors[color]}${message}${colors.reset}`);
}

// Run sync script
function runSync() {
  return new Promise((resolve, reject) => {
    log('🔄 Syncing unified-store files...', 'cyan');

    const sync = spawn('node', [SYNC_SCRIPT], {
      cwd: path.dirname(SYNC_SCRIPT),
      stdio: 'inherit'
    });

    sync.on('close', (code) => {
      if (code === 0) {
        log('✅ Sync completed!', 'green');
        resolve();
      } else {
        log(`❌ Sync failed with code ${code}`, 'red');
        reject(new Error(`Sync failed with code ${code}`));
      }
    });

    sync.on('error', (err) => {
      log(`❌ Sync error: ${err.message}`, 'red');
      reject(err);
    });
  });
}

// Start the server
function startServer() {
  return new Promise((resolve) => {
    log(`\n🚀 Starting ${serverFile}...`, 'blue');

    serverProcess = spawn('tsx', [serverFile], {
      cwd: __dirname,
      stdio: 'inherit',
      env: { ...process.env }
    });

    serverProcess.on('spawn', () => {
      log(`✨ Server started (PID: ${serverProcess.pid})`, 'green');
      resolve();
    });

    serverProcess.on('error', (err) => {
      log(`❌ Server error: ${err.message}`, 'red');
    });

    serverProcess.on('close', (code) => {
      if (code !== null) {
        log(`⚠️  Server exited with code ${code}`, 'yellow');
      }
      serverProcess = null;
    });
  });
}

// Stop the server
function stopServer() {
  return new Promise((resolve) => {
    if (!serverProcess) {
      resolve();
      return;
    }

    log('🛑 Stopping server...', 'yellow');

    const timeout = setTimeout(() => {
      log('⚠️  Force killing server...', 'red');
      serverProcess.kill('SIGKILL');
    }, 5000);

    serverProcess.once('close', () => {
      clearTimeout(timeout);
      log('✅ Server stopped', 'green');
      serverProcess = null;
      resolve();
    });

    serverProcess.kill('SIGTERM');
  });
}

// Restart server
async function restartServer() {
  await stopServer();
  await startServer();
}

// Handle file changes with debounce
function handleChange(filepath) {
  const relPath = path.relative(SOURCE_DIR, filepath);
  log(`📝 Changed: ${relPath}`, 'cyan');

  // Clear existing timeout
  if (syncTimeout) {
    clearTimeout(syncTimeout);
  }

  // Debounce sync and restart
  syncTimeout = setTimeout(async () => {
    try {
      await runSync();
      await restartServer();
    } catch (err) {
      log(`❌ Error: ${err.message}`, 'red');
    }
  }, 1000);
}

// Initialize and start
async function init() {
  console.clear();
  log('═══════════════════════════════════════════════════════', 'bright');
  log('   Orchestrix Store Development Server', 'bright');
  log(`   Mode: ${useXState ? 'XState Enhanced' : 'Basic'}`, 'bright');
  log('   Auto-sync and restart enabled', 'bright');
  log('═══════════════════════════════════════════════════════', 'bright');

  // Initial sync and start
  try {
    await runSync();
    await startServer();
  } catch (err) {
    log(`❌ Failed to start: ${err.message}`, 'red');
    process.exit(1);
  }

  // Watch for changes
  log(`\n👁️  Watching ${SOURCE_DIR}...`, 'cyan');

  const watcher = chokidar.watch(SOURCE_DIR, {
    ignored: /(^|[\/\\])\../, // ignore dotfiles
    persistent: true,
    ignoreInitial: true,
    awaitWriteFinish: {
      stabilityThreshold: 300,
      pollInterval: 100
    }
  });

  watcher
    .on('add', handleChange)
    .on('change', handleChange)
    .on('unlink', handleChange)
    .on('error', error => log(`❌ Watcher error: ${error}`, 'red'));

  log('Press Ctrl+C to stop\n', 'yellow');
}

// Handle shutdown
process.on('SIGINT', async () => {
  log('\n\n🛑 Shutting down...', 'yellow');

  if (watcher) {
    await watcher.close();
  }

  await stopServer();

  log('👋 Goodbye!', 'green');
  process.exit(0);
});

process.on('SIGTERM', async () => {
  await stopServer();
  process.exit(0);
});

// Start the development server
init().catch(err => {
  log(`❌ Fatal error: ${err.message}`, 'red');
  process.exit(1);
});