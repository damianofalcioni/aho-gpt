#!/usr/bin/env node

import { readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { generate, parse } from 'css-tree';

const toolsDir = path.dirname(fileURLToPath(import.meta.url));
const docsDir = path.resolve(toolsDir, '../docs');
const sourcePath = path.join(docsDir, 'styles.css');
const targetPath = path.join(docsDir, 'styles.min.css');

// Serialize parsed CSS without restructuring rules or optimizing their values.
// The grammar preserves descendant combinators, strings and calc() spacing.
export function minifyCss(source) {
  const ast = parse(source, {
    parseCustomProperty: true,
    onParseError(error) { throw error; }
  });
  return `${generate(ast)}\n`;
}

async function main() {
  const generated = minifyCss(await readFile(sourcePath, 'utf8'));

  if (process.argv.includes('--check')) {
    let current = '';
    try {
      current = await readFile(targetPath, 'utf8');
    } catch (error) {
      if (error.code !== 'ENOENT') throw error;
    }

    if (current !== generated) {
      console.error('styles.min.css is out of date; run: npm run build:css');
      process.exitCode = 1;
    } else {
      console.log('styles.min.css is in sync with styles.css');
    }
  } else {
    await writeFile(targetPath, generated, 'utf8');
    console.log(`wrote ${path.relative(docsDir, targetPath)} (${Buffer.byteLength(generated)} bytes)`);
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  await main();
}
