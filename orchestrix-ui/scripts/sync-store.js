#!/usr/bin/env node

/**
 * Sync store files from frontend to backend store-server
 * This runs as a post-build task to ensure both stores behave identically
 */

const fs = require('fs');
const path = require('path');

const SOURCE_DIR = path.resolve(__dirname, '../src/unified-store');
const DEST_DIR = path.resolve(__dirname, '../store-server/unified-store');

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

// Copy file and transform imports if needed
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
    let content = fs.readFileSync(src, 'utf-8');

    // Transform TypeScript to JavaScript compatible code for Node.js
    if (src.endsWith('.ts') || src.endsWith('.tsx')) {
      // Basic transformations for Node.js compatibility
      content = transformForNode(content, src);
      dest = dest.replace(/\.tsx?$/, '.js');
    }

    fs.writeFileSync(dest, content);
    console.log(`  ✓ Synced: ${path.relative(SOURCE_DIR, src)}`);
  }
}

// Transform TypeScript code for Node.js
function transformForNode(content, filepath) {
  // Skip if it's already JavaScript
  if (!filepath.endsWith('.ts') && !filepath.endsWith('.tsx')) {
    return content;
  }

  // Remove TypeScript type annotations (basic transformation)
  let transformed = content;

  // Convert ES6 imports to CommonJS for Node.js
  transformed = transformed
    // import { X } from 'Y' -> const { X } = require('Y')
    .replace(/^import\s+\{([^}]+)\}\s+from\s+['"]([^'"]+)['"]/gm,
             "const {$1} = require('$2')")
    // import X from 'Y' -> const X = require('Y')
    .replace(/^import\s+(\w+)\s+from\s+['"]([^'"]+)['"]/gm,
             "const $1 = require('$2')")
    // export default -> module.exports =
    .replace(/^export\s+default\s+/gm, 'module.exports = ')
    // export { -> module.exports = {
    .replace(/^export\s+\{/gm, 'module.exports = {')
    // export const/let/var -> exports.
    .replace(/^export\s+(const|let|var)\s+(\w+)/gm, 'exports.$2')
    // Remove type imports
    .replace(/^import\s+type\s+.*$/gm, '')
    // Remove interface/type definitions
    .replace(/^(export\s+)?(interface|type)\s+\w+\s*=?\s*{[^}]*}/gm, '')
    // Remove type annotations from parameters
    .replace(/:\s*[\w\[\]<>,\s|]+(?=[,\)])/g, '')
    // Remove generic type parameters
    .replace(/<[\w\s,]+>/g, '');

  return `// Auto-generated from TypeScript source
// Original: ${path.relative(SOURCE_DIR, filepath)}
// Generated: ${new Date().toISOString()}

${transformed}`;
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

// Create main index file for Node.js
function createNodeIndex() {
  const indexPath = path.join(DEST_DIR, 'index.js');
  const indexContent = `// Auto-generated index file for Node.js store
// This file exports all stores and services for use in the backend

const path = require('path');

// Export all stores
module.exports = {
  // Base stores
  ...requireIfExists('./base/StellarStore'),

  // Infrastructure stores
  ...requireIfExists('./infrastructure/OrganizationInfraStore'),

  // Event system
  ...requireIfExists('./events/EventBus'),
  ...requireIfExists('./events/LocalStore'),

  // Services
  ...requireIfExists('./services/QueryService'),
  ...requireIfExists('./services/MutationService'),

  // Helpers
  ...requireIfExists('./helpers')
};

function requireIfExists(modulePath) {
  try {
    return require(modulePath);
  } catch (e) {
    console.log(\`Module not found: \${modulePath}\`);
    return {};
  }
}
`;

  fs.writeFileSync(indexPath, indexContent);
  console.log('  ✓ Created index.js for Node.js');
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
      const dest = filepath.replace(SOURCE_DIR, DEST_DIR).replace(/\.tsx?$/, '.js');
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