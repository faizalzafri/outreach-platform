/**
 * Bundle Size Check Script
 *
 * Verifies that the total gzipped JS bundle size does not exceed 1MB.
 * Intended to run after `vite build` completes.
 * Exits with code 1 if the threshold is breached.
 */

import { readdir, readFile } from 'node:fs/promises';
import { join } from 'node:path';
import { gzipSync } from 'node:zlib';

const DIST_DIR = join(import.meta.dirname, '..', 'dist', 'assets');
const MAX_TOTAL_GZIPPED_BYTES = 1024 * 1024; // 1MB
const MAX_INITIAL_GZIPPED_BYTES = 200 * 1024; // 200KB for initial bundle

async function getJsFiles(dir) {
  try {
    const entries = await readdir(dir);
    return entries.filter((f) => f.endsWith('.js'));
  } catch {
    console.error(`❌ Could not read directory: ${dir}`);
    console.error('   Make sure to run "npm run build" before this script.');
    process.exit(1);
  }
}

async function main() {
  const jsFiles = await getJsFiles(DIST_DIR);

  if (jsFiles.length === 0) {
    console.error('❌ No JS files found in dist/assets/');
    process.exit(1);
  }

  let totalGzipped = 0;
  let initialGzipped = 0;
  const results = [];

  for (const file of jsFiles) {
    const content = await readFile(join(DIST_DIR, file));
    const gzipped = gzipSync(content);
    const gzippedSize = gzipped.length;
    totalGzipped += gzippedSize;

    // Entry chunks (index-*) contribute to initial bundle
    if (file.startsWith('index-') || file.startsWith('vendor-react-') || file.startsWith('vendor-tanstack-')) {
      initialGzipped += gzippedSize;
    }

    results.push({ file, raw: content.length, gzipped: gzippedSize });
  }

  // Sort by gzipped size descending
  results.sort((a, b) => b.gzipped - a.gzipped);

  console.log('\n📦 Bundle Size Report');
  console.log('─'.repeat(70));
  console.log(`${'File'.padEnd(42)} ${'Raw'.padStart(10)} ${'Gzipped'.padStart(10)}`);
  console.log('─'.repeat(70));

  for (const { file, raw, gzipped } of results) {
    const name = file.length > 40 ? '…' + file.slice(-39) : file;
    console.log(
      `${name.padEnd(42)} ${formatBytes(raw).padStart(10)} ${formatBytes(gzipped).padStart(10)}`
    );
  }

  console.log('─'.repeat(70));
  console.log(
    `${'TOTAL'.padEnd(42)} ${formatBytes(results.reduce((s, r) => s + r.raw, 0)).padStart(10)} ${formatBytes(totalGzipped).padStart(10)}`
  );
  console.log(
    `${'Initial bundle (entry + vendor-react + vendor-tanstack)'.slice(0, 42).padEnd(42)} ${''.padStart(10)} ${formatBytes(initialGzipped).padStart(10)}`
  );
  console.log('─'.repeat(70));

  let failed = false;

  if (totalGzipped > MAX_TOTAL_GZIPPED_BYTES) {
    console.error(
      `\n❌ FAIL: Total JS gzipped size (${formatBytes(totalGzipped)}) exceeds limit (${formatBytes(MAX_TOTAL_GZIPPED_BYTES)})`
    );
    failed = true;
  } else {
    console.log(
      `\n✅ Total JS gzipped: ${formatBytes(totalGzipped)} / ${formatBytes(MAX_TOTAL_GZIPPED_BYTES)} limit`
    );
  }

  if (initialGzipped > MAX_INITIAL_GZIPPED_BYTES) {
    console.warn(
      `\n⚠️  WARNING: Initial bundle gzipped (${formatBytes(initialGzipped)}) exceeds target (${formatBytes(MAX_INITIAL_GZIPPED_BYTES)})`
    );
  } else {
    console.log(
      `✅ Initial bundle gzipped: ${formatBytes(initialGzipped)} / ${formatBytes(MAX_INITIAL_GZIPPED_BYTES)} target`
    );
  }

  if (failed) {
    process.exit(1);
  }
}

function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

main();
