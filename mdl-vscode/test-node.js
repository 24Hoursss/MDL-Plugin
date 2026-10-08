'use strict';

// Offline test harness: stubs the minimal vscode API surface used by
// extension.js and checks folding / circuitdeck detection against a real file.
const fs = require('fs');
const path = require('path');

class FakeRange {
    constructor(startLine, startChar, endLine, endChar) {
        this.start = { line: startLine, character: startChar };
        this.end = { line: endLine, character: endChar };
    }
}
class FakeCompletionItem {
    constructor(label, kind) {
        this.label = label;
        this.kind = kind;
    }
}
class FakeSnippetString {
    constructor(value) { this.value = value; }
}
class FakeFoldingRange {
    constructor(start, end) {
        this.start = start;
        this.end = end;
    }
}
const fakeVscode = {
    CompletionItem: FakeCompletionItem,
    CompletionItemKind: { Keyword: 1, Constant: 2, Variable: 3, Snippet: 4 },
    SnippetString: FakeSnippetString,
    FoldingRange: FakeFoldingRange,
    FoldingRangeKind: { Region: 'region' },
    Range: FakeRange,
    Diagnostic: function (range, message, severity) {
        this.range = range;
        this.message = message;
        this.severity = severity;
    },
    DiagnosticSeverity: { Error: 0, Warning: 1 },
    languages: {
        createDiagnosticCollection() {
            return { set() {}, dispose() {} };
        },
    },
};

const srcRaw = fs.readFileSync(path.join(__dirname, 'extension.js'), 'utf8');
const src = srcRaw.replace("const vscode = require('vscode');", "const vscode = global.__fakeVscode;");
global.__fakeVscode = fakeVscode;

function loadModule(code) {
    const mod = { exports: {} };
    new Function('module', 'exports', 'require', code)(mod, mod.exports, () => fakeVscode);
    return mod.exports;
}

const testExports = loadModule(
    src + '\nmodule.exports._test = { isInCircuitdeck, MdlFoldingRangeProvider, MdlCompletionProvider, findDataRanges, mdCheck };');
const { isInCircuitdeck: isInCirc, MdlFoldingRangeProvider, MdlCompletionProvider, findDataRanges, mdCheck } = testExports._test;

const file = process.argv[2];
const lines = fs.readFileSync(file, 'utf8').split('\n');
if (lines.length > 0 && lines[lines.length - 1] === '') {
    lines.pop(); // VSCode documents do not have the trailing empty element
}
const doc = {
    lineCount: lines.length,
    lineAt(i) { return { text: lines[i] }; },
};

// --- folding ---
const provider = new MdlFoldingRangeProvider();
const ranges = provider.provideFoldingRanges(doc);
let wholeFile = 0;
for (const r of ranges) {
    if (r.start === 0 && r.end >= doc.lineCount - 2) wholeFile++;
}
let sorted = true;
for (let i = 1; i < ranges.length; i++) {
    if (ranges[i].start < ranges[i - 1].start) sorted = false;
}
console.log('folding ranges:', ranges.length, 'wholeFile:', wholeFile, 'sorted:', sorted);

// --- circuitdeck detection: locate the first circuitdeck block by content ---
let circLine = -1;
for (let i = 0; i < doc.lineCount; i++) {
    if (/^\s*circuitdeck\s*$/.test(doc.lineAt(i).text)) {
        circLine = i;
        break;
    }
}
if (circLine < 0) {
    console.log('no circuitdeck found');
    process.exit(1);
}
// find the matching close line with the reference column-0 brace scan
let depth = 0, closeLine = -1;
for (let i = circLine + 1; i < doc.lineCount; i++) {
    const t = doc.lineAt(i).text.trim();
    if (t === '{' && doc.lineAt(i).text[0] === '{') depth++;
    else if (t === '}' && doc.lineAt(i).text[0] === '}') {
        if (depth === 1) { closeLine = i; break; }
        depth--;
    }
}
const checks = [
    [0, false, 'file header'],
    [circLine, false, 'circuitdeck header line'],
    [circLine + 1, true, 'its opening brace line'],
    [circLine + 2, true, 'first netlist line'],
    [closeLine - 1, true, 'last netlist line'],
    [closeLine, false, 'closing brace line of circuitdeck'],
    [closeLine + 1, false, 'line right after the block'],
];
let pass = 0;
for (const [ln, want, name] of checks) {
    const got = isInCirc(doc, { line: ln, character: 0 });
    if (got === want) pass++;
    console.log(`${got === want ? 'PASS' : 'FAIL'} line ${ln + 1} (${name}: ${JSON.stringify(doc.lineAt(ln).text)}) inCirc=${got} want=${want}`);
}
console.log(`circuitdeck checks: ${pass}/${checks.length} (lines are 0-based, block lines ${circLine + 1}..${closeLine + 1})`);

// --- data-block ranges for the Fold Data Blocks command ---
const dataRanges = findDataRanges(doc);
let badHeader = 0;
for (const r of dataRanges) {
    if (doc.lineAt(r.start).text.trim() !== 'data') badHeader++;
    if (r.end <= r.start) badHeader++;
}
console.log('data folding ranges:', dataRanges.length, 'bad:', badHeader);

// --- deck checks ---
function makeDocFromFile(file) {
    const ls = fs.readFileSync(file, 'utf8').split('\n');
    if (ls.length > 0 && ls[ls.length - 1] === '') ls.pop();
    return {
        lineCount: ls.length,
        lineAt(i) { return { text: ls[i] }; },
    };
}
const broken = process.argv[3] && fs.existsSync(process.argv[3]) ? process.argv[3] : null;
if (broken) {
    const bissues = mdCheck(makeDocFromFile(broken));
    const bErrors = bissues.filter((i) => i.error).length;
    console.log(`mdCheck(${path.basename(broken)}): issues=${bissues.length} errors=${bErrors}`);
} else {
    const samples = [
        ['sample', 'C:/Users/PC/Desktop/mdl-ide-plugin/test-assets/sample.mdl', 0],
        ['broken', 'C:/Users/PC/Desktop/mdl-ide-plugin/test-assets/broken.mdl', 8],
        ['template', 'C:/Users/PC/Desktop/python/skills/iccap-asm-switch-modeling/scripts/ASM_Switch_Example.mdl', 58],
    ];
    for (const [name, file, want] of samples) {
        const issues = mdCheck(makeDocFromFile(file));
        const errors = issues.filter((i) => i.error).length;
        console.log(`mdCheck ${name}: issues=${issues.length} errors=${errors} want=${want}`);
    }
}
