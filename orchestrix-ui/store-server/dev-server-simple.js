#!/usr/bin/env node

const { spawn } = require('child_process');
const chokidar = require('chokidar');
const path = require('path');
const fs = require('fs');
const chalk = require('chalk');

// Configuration
const WATCH_PATHS = [
  path.join(__dirname, 'unified-store'),
  path.join(__dirname, '..', 'src', 'unified-store'),
  path.join(__dirname, '..', 'src', 'store'),
];

const SERVER_FILE = path.join(__dirname, 'server-simple.js');

let serverProcess = null;
let syncTimeout = null;
let restartTimeout = null;

console.log(chalk.bold.cyan('🚀 Simple Store Dev Server'));
console.log(chalk.gray('━'.repeat(50)));
console.log(chalk.cyan('📁 Watching for changes...'));

// Function to run sync
async function runSync() {
  console.log(chalk.yellow('🔄 Syncing store files...'));

  return new Promise((resolve, reject) => {
    const sync = spawn('node', ['../scripts/sync-store.js'], {
      cwd: __dirname,
      stdio: 'inherit'
    });

    sync.on('close', (code) => {
      if (code === 0) {
        console.log(chalk.green('✅ Store files synced successfully'));
        resolve();
      } else {
        console.error(chalk.red('❌ Sync failed'));
        reject(new Error(`Sync exited with code ${code}`));
      }
    });

    sync.on('error', (err) => {
      console.error(chalk.red('❌ Failed to run sync:'), err);
      reject(err);
    });
  });
}

// Function to start server
function startServer() {
  console.log(chalk.cyan('🚀 Starting simple store server...'));

  serverProcess = spawn('node', [SERVER_FILE], {
    cwd: __dirname,
    stdio: 'inherit',
    env: { ...process.env, NODE_ENV: 'development' }
  });

  serverProcess.on('close', (code) => {
    if (code !== null && code !== 0) {
      console.error(chalk.red(`❌ Server exited with code ${code}`));
    }
    serverProcess = null;
  });

  serverProcess.on('error', (err) => {
    console.error(chalk.red('❌ Failed to start server:'), err);
    serverProcess = null;
  });
}

// Function to stop server
function stopServer() {
  if (serverProcess) {
    console.log(chalk.yellow('🛑 Stopping server...'));
    serverProcess.kill('SIGINT');
    serverProcess = null;
  }
}

// Function to restart server
async function restartServer() {
  stopServer();

  // Wait a bit for the port to be released
  await new Promise(resolve => setTimeout(resolve, 1000));

  startServer();
}

// Handle file changes
function handleFileChange(filePath) {
  const relativePath = path.relative(process.cwd(), filePath);
  console.log(chalk.gray(`📝 File changed: ${relativePath}`));

  // Clear existing timeouts
  if (syncTimeout) clearTimeout(syncTimeout);
  if (restartTimeout) clearTimeout(restartTimeout);

  // Check if this is a frontend store file
  const isFrontendStore = filePath.includes(path.join('src', 'unified-store')) ||
                          filePath.includes(path.join('src', 'store'));

  if (isFrontendStore) {
    // Frontend store changed - sync then restart
    console.log(chalk.yellow('🔄 Frontend store changed, syncing...'));

    syncTimeout = setTimeout(async () => {
      try {
        await runSync();
        await restartServer();
      } catch (error) {
        console.error(chalk.red('❌ Failed to sync and restart:'), error);
      }
    }, 1000);
  } else {
    // Backend store changed - just restart
    console.log(chalk.yellow('🔄 Backend store changed, restarting...'));

    restartTimeout = setTimeout(async () => {
      await restartServer();
    }, 1000);
  }
}

// Set up file watcher
const watcher = chokidar.watch(WATCH_PATHS, {
  ignored: [
    /node_modules/,
    /\.git/,
    /store-snapshots/,
    /\.log$/,
    /\.jsonl$/,
    /\.gz$/
  ],
  persistent: true,
  ignoreInitial: true
});

watcher
  .on('change', handleFileChange)
  .on('add', handleFileChange)
  .on('unlink', handleFileChange);

// Initial sync and start
(async () => {
  try {
    await runSync();
    startServer();

    console.log(chalk.gray('━'.repeat(50)));
    console.log(chalk.green('✓ Dev server ready'));
    console.log(chalk.gray('  • Auto-sync enabled'));
    console.log(chalk.gray('  • Auto-restart enabled'));
    console.log(chalk.gray('  • Store snapshots enabled'));
    console.log(chalk.gray('━'.repeat(50)));
  } catch (error) {
    console.error(chalk.red('❌ Failed to start:'), error);
    process.exit(1);
  }
})();

// Handle process termination
process.on('SIGINT', () => {
  console.log(chalk.yellow('\n🛑 Shutting down dev server...'));

  stopServer();
  watcher.close();

  console.log(chalk.green('✓ Dev server stopped'));
  process.exit(0);
});

process.on('SIGTERM', () => {
  stopServer();
  watcher.close();
  process.exit(0);
});