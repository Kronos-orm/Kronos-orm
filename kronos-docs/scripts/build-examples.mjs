// Runs as an ES2022 module on Node 22.12+; Aura and Lodash are not used here.
/* eslint-env node, es2022 */
/* eslint
    "es-x/no-arrow-functions": "off",
    "es-x/no-for-of-loops": "off",
    "es-x/no-number-parseint": "off",
    "@salesforce/aura/ecma-intrinsics": "off",
    "lodash/prefer-lodash-method": "off"
*/
import {execFileSync} from 'node:child_process';
import {existsSync, mkdirSync, readFileSync, writeFileSync} from 'node:fs';
import path from 'node:path';

const root = path.resolve(path.dirname(process.argv[1]), '..');
const source = path.join(root, 'src/assets/examples/kotlin-toolchain');
function sourcePath(file) {
    const target = path.resolve(source, file), relative = path.relative(source, target);
    if (path.isAbsolute(relative) || /^\.\.(?:[\\/]|$)/.test(relative)) throw new Error(`Invalid example path: ${file}`);
    return target;
}
const tracked = execFileSync('git', ['ls-files', '--cached', '--others', '--exclude-standard', '-z', '--', source],
    {cwd: source, encoding: 'utf8'});
// Git still lists unstaged deletions; archive the source files present in this worktree.
// eslint-disable-next-line security/detect-non-literal-fs-filename -- sourcePath confines Git-listed names to the maintained example root.
const files = (tracked.match(/[^\0]+/g) || []).filter(file => existsSync(sourcePath(file))).sort();
if (files.length === 0) throw new Error('Kotlin Toolchain example sources are missing');

function crc32(bytes) {
    let crc = 0xffffffff;
    for (const byte of bytes) {
        crc ^= byte;
        for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0);
    }
    return (crc ^ 0xffffffff) >>> 0;
}

// A deterministic ZIP with UTF-8 names, executable wrapper permissions and no build outputs.
const entries = [], directory = [];
let offset = 0;
for (const file of files) {
    // eslint-disable-next-line security/detect-non-literal-fs-filename -- The Git-listed filename is checked against the example root by sourcePath.
    const bytes = readFileSync(sourcePath(file));
    const name = Buffer.from(`kotlin-toolchain/${file}`), checksum = crc32(bytes);
    const local = Buffer.alloc(30);
    local.writeUInt32LE(0x04034b50, 0); local.writeUInt16LE(20, 4);
    local.writeUInt16LE(0x800, 6); local.writeUInt16LE(0x21, 12);
    local.writeUInt32LE(checksum, 14); local.writeUInt32LE(bytes.length, 18);
    local.writeUInt32LE(bytes.length, 22); local.writeUInt16LE(name.length, 26);
    const central = Buffer.alloc(46);
    central.writeUInt32LE(0x02014b50, 0); central.writeUInt16LE(0x0314, 4);
    central.writeUInt16LE(20, 6); central.writeUInt16LE(0x800, 8); central.writeUInt16LE(0x21, 14);
    central.writeUInt32LE(checksum, 16); central.writeUInt32LE(bytes.length, 20);
    central.writeUInt32LE(bytes.length, 24); central.writeUInt16LE(name.length, 28);
    central.writeUInt32LE((Number.parseInt(file === 'kotlin' ? '100755' : '100644', 8) << 16) >>> 0, 38);
    central.writeUInt32LE(offset, 42);
    entries.push(local, name, bytes); directory.push(central, name);
    offset += local.length + name.length + bytes.length;
}
const central = Buffer.concat(directory), end = Buffer.alloc(22);
end.writeUInt32LE(0x06054b50, 0); end.writeUInt16LE(files.length, 8); end.writeUInt16LE(files.length, 10);
end.writeUInt32LE(central.length, 12); end.writeUInt32LE(offset, 16);
const destination = path.join(root, 'src/assets/downloads/kotlin-toolchain.zip');
// eslint-disable-next-line security/detect-non-literal-fs-filename -- The output directory is fixed under this documentation project.
mkdirSync(path.dirname(destination), {recursive: true});
entries.push(central, end);
// eslint-disable-next-line security/detect-non-literal-fs-filename -- The archive path is fixed by this build script, not supplied by its input files.
writeFileSync(destination, Buffer.concat(entries));
process.stdout.write(`Packaged ${files.length} Kotlin Toolchain example sources.\n`);
