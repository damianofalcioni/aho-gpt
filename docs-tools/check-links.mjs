import assert from 'node:assert/strict';
import { readFile, readdir, stat } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const docs = fileURLToPath(new URL('../docs/', import.meta.url));
let checked = 0;

async function checkPage(file) {
  const html = await readFile(file, 'utf8');
  assert.match(html, /<html lang="en">/);
  assert.match(html, /<meta name="viewport"/);
  assert.equal((html.match(/<h1[\s>]/g) || []).length, 1, `${file}: one page heading`);
  for (const match of html.matchAll(/(?:href|src)="([^"]+)"/g)) {
    const url = match[1];
    if (/^https:\/\//.test(url)) continue;
    assert.ok(!url.startsWith('/'), `${file}: links must work below the Pages project path`);
    const [relative, fragment] = url.split('#');
    let target = path.resolve(path.dirname(file), relative || path.basename(file));
    assert.ok(path.relative(docs, target).split(path.sep)[0] !== '..', `${file}: link leaves docs`);
    if ((await stat(target)).isDirectory()) target = path.join(target, 'index.html');
    await stat(target);
    if (fragment) assert.ok((await readFile(target, 'utf8')).includes(`id="${fragment}"`), `${file}: missing ${url}`);
    checked++;
  }
}

async function visit(directory) {
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const file = path.join(directory, entry.name);
    if (entry.isDirectory()) await visit(file);
    else if (entry.name.endsWith('.html')) await checkPage(file);
  }
}

await visit(docs);
console.log(`Checked ${checked} local links and assets.`);
