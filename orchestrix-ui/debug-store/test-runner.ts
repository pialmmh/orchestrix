#!/usr/bin/env tsx

import { getStoreManager } from './unified-store/core/StoreManager';
import chalk from 'chalk';
import * as fs from 'fs';
import * as path from 'path';

// Test runner for stores without UI
class StoreTestRunner {
  private storeManager = getStoreManager();
  private testResults: Array<{
    name: string;
    success: boolean;
    error?: string;
    duration: number;
  }> = [];

  async runTest(name: string, testFn: () => Promise<void>) {
    console.log(chalk.cyan(`\n🧪 Running test: ${name}`));
    console.log(chalk.gray('━'.repeat(50)));

    const startTime = Date.now();

    try {
      await testFn();
      const duration = Date.now() - startTime;

      this.testResults.push({ name, success: true, duration });
      console.log(chalk.green(`✅ PASSED`) + chalk.gray(` (${duration}ms)`));

    } catch (error: any) {
      const duration = Date.now() - startTime;

      this.testResults.push({ name, success: false, error: error.message, duration });
      console.log(chalk.red(`❌ FAILED`) + chalk.gray(` (${duration}ms)`));
      console.error(chalk.red(`   Error: ${error.message}`));
    }
  }

  async runAllTests() {
    console.log(chalk.bold.cyan('\n🚀 Store Test Runner'));
    console.log(chalk.gray('═'.repeat(50)));

    // Test 1: Infrastructure Store - Load tree
    await this.runTest('Load Infrastructure Tree', async () => {
      const infraStore = this.storeManager.getStore<any>('infrastructure');
      await infraStore.loadInfrastructureTree('self');

      if (!infraStore.treeData || infraStore.treeData.length === 0) {
        throw new Error('Tree data is empty');
      }

      console.log(chalk.gray(`   • Loaded ${infraStore.treeData.length} root nodes`));
    });

    // Test 2: Infrastructure Store - Select node
    await this.runTest('Select Tree Node', async () => {
      const infraStore = this.storeManager.getStore<any>('infrastructure');

      if (infraStore.treeData.length > 0) {
        const firstNode = infraStore.treeData[0];
        infraStore.setSelectedNode(firstNode);

        if (!infraStore.selectedNode) {
          throw new Error('Failed to select node');
        }

        console.log(chalk.gray(`   • Selected node: ${firstNode.label}`));
      }
    });

    // Test 3: Infrastructure Store - Query execution
    await this.runTest('Execute Direct Query', async () => {
      const infraStore = this.storeManager.getStore<any>('infrastructure');

      const query = {
        kind: 'partner',
        criteria: { name: 'telcobright' },
        page: { limit: 10, offset: 0 }
      };

      const result = await infraStore.executeQuery(query);

      if (!result) {
        throw new Error('Query returned no result');
      }

      console.log(chalk.gray(`   • Query returned ${Array.isArray(result) ? result.length : 1} items`));
    });

    // Test 4: Store snapshots
    await this.runTest('Store Snapshot Creation', async () => {
      let snapshot = this.storeManager.loadSnapshot('infrastructure');

      if (!snapshot) {
        // Try to wait a bit for snapshot to be created
        await new Promise(resolve => setTimeout(resolve, 2000));

        snapshot = this.storeManager.loadSnapshot('infrastructure');
        if (!snapshot) {
          throw new Error('No snapshot found');
        }
      }

      console.log(chalk.gray(`   • Snapshot exists with timestamp: ${new Date(snapshot?.timestamp).toISOString()}`));
    });

    // Test 5: Environment filtering
    await this.runTest('Environment Filter', async () => {
      const infraStore = this.storeManager.getStore<any>('infrastructure');

      // Set environment filter
      infraStore.setEnvironmentFilter('production');

      // Check if filter was applied
      if (infraStore.selectedEnvironmentFilter !== 'production') {
        throw new Error('Environment filter not set');
      }

      console.log(chalk.gray(`   • Filter applied: production`));

      // Clear filter
      infraStore.setEnvironmentFilter(null);
    });

    // Test 6: Node expansion
    await this.runTest('Node Expansion', async () => {
      const infraStore = this.storeManager.getStore<any>('infrastructure');

      if (infraStore.treeData.length > 0) {
        const nodeId = infraStore.treeData[0].id;

        // Expand node
        infraStore.expandNode(nodeId);

        if (!infraStore.isNodeExpanded(nodeId)) {
          throw new Error('Node not expanded');
        }

        console.log(chalk.gray(`   • Expanded node: ${nodeId}`));

        // Collapse node
        infraStore.collapseNode(nodeId);

        if (infraStore.isNodeExpanded(nodeId)) {
          throw new Error('Node not collapsed');
        }

        console.log(chalk.gray(`   • Collapsed node: ${nodeId}`));
      }
    });

    // Print summary
    this.printSummary();
  }

  printSummary() {
    console.log(chalk.bold.cyan('\n📊 Test Summary'));
    console.log(chalk.gray('═'.repeat(50)));

    const passed = this.testResults.filter(r => r.success).length;
    const failed = this.testResults.filter(r => !r.success).length;
    const totalDuration = this.testResults.reduce((sum, r) => sum + r.duration, 0);

    console.log(chalk.green(`✅ Passed: ${passed}`));
    console.log(chalk.red(`❌ Failed: ${failed}`));
    console.log(chalk.gray(`⏱️  Total time: ${totalDuration}ms`));

    if (failed > 0) {
      console.log(chalk.red('\n❌ Failed tests:'));
      this.testResults.filter(r => !r.success).forEach(result => {
        console.log(chalk.red(`   • ${result.name}: ${result.error}`));
      });
    }

    console.log(chalk.gray('═'.repeat(50)));

    // Check store snapshots
    console.log(chalk.cyan('\n📁 Store Snapshots'));
    console.log(chalk.gray('━'.repeat(50)));

    const snapshotDir = path.join(process.cwd(), 'store-snapshots');

    if (fs.existsSync(snapshotDir)) {
      const files = fs.readdirSync(snapshotDir).filter(f => f.endsWith('.json'));
      files.forEach(file => {
        const stats = fs.statSync(path.join(snapshotDir, file));
        const size = (stats.size / 1024).toFixed(2);
        console.log(chalk.gray(`   • ${file} (${size} KB)`));
      });
    } else {
      console.log(chalk.yellow('   No snapshots directory found'));
    }

    // Exit with appropriate code
    process.exit(failed > 0 ? 1 : 0);
  }
}

// Run tests
async function main() {
  const runner = new StoreTestRunner();

  try {
    await runner.runAllTests();
  } catch (error: any) {
    console.error(chalk.red('❌ Test runner failed:'), error);
    process.exit(1);
  }
}

// Handle errors
process.on('unhandledRejection', (error) => {
  console.error(chalk.red('❌ Unhandled rejection:'), error);
  process.exit(1);
});

process.on('uncaughtException', (error) => {
  console.error(chalk.red('❌ Uncaught exception:'), error);
  process.exit(1);
});

// Run
main();