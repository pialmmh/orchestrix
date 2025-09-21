#!/usr/bin/env node

/**
 * Sync store files from frontend to backend debug-store
 * This runs as a post-build task to ensure both stores behave identically
 */

const fs = require('fs');
const path = require('path');

const SOURCE_DIR = path.resolve(__dirname, '../src/unified-store');
const DEST_DIR = path.resolve(__dirname, '../debug-store/unified-store');

// Files/folders to exclude from sync
const EXCLUDE = [
  'node_modules',
  '.git',
  '*.test.ts',
  '*.test.js',
  '*.spec.ts',
  '*.spec.js'
];

// Create destination directory if it doesn't exist
function ensureDir(dir) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

// Check if file should be excluded
function shouldExclude(file) {
  return EXCLUDE.some(pattern => {
    if (pattern.includes('*')) {
      const regex = new RegExp(pattern.replace('*', '.*'));
      return regex.test(file);
    }
    return file.includes(pattern);
  });
}

// Copy file without transformation
function syncFile(src, dest) {
  if (shouldExclude(src)) return;

  const stats = fs.statSync(src);

  if (stats.isDirectory()) {
    ensureDir(dest);
    const files = fs.readdirSync(src);

    files.forEach(file => {
      syncFile(path.join(src, file), path.join(dest, file));
    });
  } else if (stats.isFile()) {
    // Copy TypeScript files as-is without transformation
    const content = fs.readFileSync(src, 'utf-8');
    fs.writeFileSync(dest, content);
    console.log(`  ✓ Synced: ${path.relative(SOURCE_DIR, src)}`);
  }
}


// Main sync function
function syncStore() {
  console.log('\n🔄 Syncing store files...\n');
  console.log(`  Source: ${SOURCE_DIR}`);
  console.log(`  Destination: ${DEST_DIR}\n`);

  // Ensure destination exists
  ensureDir(DEST_DIR);

  // Check if source exists
  if (!fs.existsSync(SOURCE_DIR)) {
    console.error(`❌ Source directory not found: ${SOURCE_DIR}`);
    console.log('  Creating source directory structure...');

    // Create the unified-store structure
    const dirs = ['base', 'infrastructure', 'entities', 'events', 'services', 'helpers'];
    dirs.forEach(dir => {
      ensureDir(path.join(SOURCE_DIR, dir));
    });

    console.log('  ✓ Created unified-store structure');
    return;
  }

  // Sync all files
  syncFile(SOURCE_DIR, DEST_DIR);

  // Create index file for Node.js
  createNodeIndex();

  console.log('\n✅ Store sync completed!\n');
}

// Create main index file for TypeScript Node.js
function createNodeIndex() {
  const indexPath = path.join(DEST_DIR, 'index.ts');
  const indexContent = `// Auto-generated index file for TypeScript Node.js store
// This file exports all stores and services for use in the backend

// Base stores
export * from './base/StellarStore';
export * from './base/UnifiedStore';

// Infrastructure stores
export * from './infrastructure/OrganizationInfraStore';

// Root store
export * from './RootStore';

// Event system
export * from './events/EventBus';
export * from './events/LocalStore';
export * from './events/StoreEvent';

// Services
export { default as QueryService } from './services/QueryService';
export { default as MutationService } from './services/MutationService';

// React components and hooks (may not be used in backend)
export * from './base/StoreProvider';
export * from './base/useStores';
`;

  fs.writeFileSync(indexPath, indexContent);
  console.log('  ✓ Created index.ts for TypeScript Node.js');
}

// Watch mode for development
function watchStore() {
  console.log('👁️  Watching for store changes...\n');

  const chokidar = require('chokidar');
  const watcher = chokidar.watch(SOURCE_DIR, {
    ignored: EXCLUDE,
    persistent: true
  });

  watcher
    .on('change', filepath => {
      console.log(`  📝 Changed: ${path.relative(SOURCE_DIR, filepath)}`);
      const dest = filepath.replace(SOURCE_DIR, DEST_DIR);
      syncFile(filepath, dest);
    })
    .on('add', filepath => {
      console.log(`  ➕ Added: ${path.relative(SOURCE_DIR, filepath)}`);
      const dest = filepath.replace(SOURCE_DIR, DEST_DIR);
      syncFile(filepath, dest);
    })
    .on('unlink', filepath => {
      console.log(`  ➖ Removed: ${path.relative(SOURCE_DIR, filepath)}`);
      const dest = filepath.replace(SOURCE_DIR, DEST_DIR);
      if (fs.existsSync(dest)) {
        fs.unlinkSync(dest);
      }
    });
}

// Parse command line arguments
const args = process.argv.slice(2);
const isWatch = args.includes('--watch') || args.includes('-w');

// Run sync
syncStore();

// Start watch mode if requested
if (isWatch) {
  watchStore();
}