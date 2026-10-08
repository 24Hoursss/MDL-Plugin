'use strict';

const vscode = require('vscode');

const KEYWORDS = [
    'LINK', 'list', 'element', 'param', 'fullprecision', 'View',
    'dataset', 'datasize', 'type', 'member',
    'BLKEDIT', 'TABLE', 'HYPTABLE', 'data', 'circuitdeck', 'OPTIMEDIT',
    '__iccapDataBlockEnd'
];
const LINK_KINDS = ['MODEL', 'DUT', 'DAT', 'SWEEP', 'OUT', 'XFORM', 'PLOT', 'MACRO', 'CIRC', 'TCIRC', 'PS', 'DPS'];
const DIRECTIVES = ['#load', '#echo', '#include', '#define'];
const VARREFS = ['$dpar', '$mpar', '$xpar', '$var'];
const NETLIST = ['define', 'model', 'end', 'subckt', 'ends', 'parameters', 'include'];

/** Compile a list of simple keyword items. */
function keywordItems(word, list, kind) {
    return list.map((label) => {
        const item = new vscode.CompletionItem(label, kind);
        item.range = word;
        return item;
    });
}

/**
 * Finds the open-ahead scanning state (same column-0 brace rule as the
 * iccap-mdl-edit agent): returns true when the given position is inside a
 * circuitdeck body.
 */
function isInCircuitdeck(document, position) {
    let depth = 0;
    let circDepth = -1;
    let inCirc = false;
    let pendingCirc = false;
    for (let line = 0; line <= position.line; line++) {
        const text = document.lineAt(line).text;
        const trimmed = text.trimEnd();
        const isLast = line === position.line;
        if (trimmed.length > 0 && trimmed[0] === '{' && trimmed === '{') {
            depth++;
            if (pendingCirc) {
                circDepth = depth;
                inCirc = true;
                pendingCirc = false;
            }
        } else if (trimmed.length > 0 && trimmed[0] === '}' && trimmed === '}') {
            if (inCirc && depth === circDepth) {
                inCirc = false;
            }
            if (depth > 0) {
                depth--;
            }
        } else {
            pendingCirc = /^\s*circuitdeck\s*$/.test(trimmed);
        }
        if (isLast) {
            break;
        }
    }
    return inCirc;
}

class MdlCompletionProvider {
    provideCompletionItems(document, position) {
        const lineText = document.lineAt(position.line).text;
        const before = lineText.slice(0, position.character);
        const items = [];

        // Replace only the word being typed.
        let wordStart = position.character;
        while (wordStart > 0 && /[A-Za-z0-9_$#]/.test(lineText[wordStart - 1])) {
            wordStart--;
        }
        const wordRange = new vscode.Range(position.line, wordStart, position.line, position.character);

        const inCirc = isInCircuitdeck(document, position);
        const atLineStart = /^\s*[A-Za-z0-9_]*$/.test(before);

        if (/^\s*LINK\s+[A-Za-z0-9_]*$/.test(before)) {
            // second word of a LINK header
            items.push(...keywordItems(wordRange, LINK_KINDS, vscode.CompletionItemKind.Keyword));
        } else if (/#[A-Za-z]*$/.test(before)) {
            items.push(...keywordItems(wordRange, DIRECTIVES, vscode.CompletionItemKind.Constant));
        } else if (/\$[A-Za-z]*$/.test(before)) {
            items.push(...keywordItems(wordRange, VARREFS, vscode.CompletionItemKind.Variable));
        } else if (atLineStart) {
            items.push(...keywordItems(wordRange, KEYWORDS, vscode.CompletionItemKind.Keyword));
            if (inCirc) {
                items.push(...keywordItems(wordRange, NETLIST, vscode.CompletionItemKind.Keyword));
            }
        }

        // Snippets
        const snippet = (label, detail, insert) => {
            const item = new vscode.CompletionItem(label, vscode.CompletionItemKind.Snippet);
            item.insertText = new vscode.SnippetString(insert);
            item.detail = detail;
            item.range = wordRange;
            return item;
        };
        items.push(
            snippet('link block', 'LINK block with braces',
                'LINK ${1|MODEL,DUT,DAT,SWEEP,OUT,XFORM,PLOT,MACRO,CIRC,TCIRC,PS,DPS|} "${2:name}" "790.004"\n{\n\t$0\n}'),
            snippet('param + fullprecision', 'parameter pair',
                'param ${1:NAME} "${2:value}"\nfullprecision ${1:NAME} "${3:value}"'),
            snippet('link transform', 'XFORM block',
                'LINK XFORM "${1:name}" "790.004"\n{\n\t$0\n}')
        );

        return items;
    }
}

/**
 * Folding ranges for the measured-data blocks only (header line is exactly
 * "data"). Used by the "Fold Data Blocks" command; VSCode cannot collapse
 * ranges by default, so this is the closest equivalent to the IDE plugin's
 * collapsed-by-default data blocks.
 */
function findDataRanges(document) {
    const ranges = [];
    const stack = [];
    for (let i = 0; i < document.lineCount; i++) {
        const text = document.lineAt(i).text;
        const trimmed = text.trimEnd();
        if (trimmed === '{' && text[0] === '{') {
            stack.push({ openLine: i });
        } else if (trimmed === '}' && text[0] === '}') {
            const open = stack.pop();
            if (!open || i - open.openLine < 2) {
                continue;
            }
            const header = findHeaderLine(document, open.openLine);
            if (document.lineAt(header).text.trim() === 'data') {
                ranges.push(new vscode.FoldingRange(header, i));
            }
        }
    }
    return ranges;
}

function findHeaderLine(document, braceLine) {
    let header = braceLine - 1;
    while (header >= 0) {
        const h = document.lineAt(header).text.trim();
        if (h.length > 0 && h !== '{' && h !== '}') {
            break;
        }
        header--;
    }
    return header < 0 ? braceLine : header;
}

/**
 * Deck-level sanity checks (same rules as the PyCharm plugin's MdlCheck):
 * column-0 brace balance, paren/bracket/inline-brace pairing, string closure,
 * param/fullprecision pairing, Optimize Use=Yes bounds.
 * Returns [{line, c0, c1, error, msg}].
 */
function mdCheck(document) {
    const MAX_ISSUES = 200;
    const issues = [];
    const add = (line, c0, c1, error, msg) => {
        if (issues.length < MAX_ISSUES) {
            issues.push({ line, c0, c1, error, msg });
        }
    };

    const structStack = [];   // { line, c }
    const pairStack = [];     // { line, c, close }
    let ctx = 0;              // 0 none, 1 circ, 2 pel, 3 py
    let ctxDepth = -1;
    let pendingCirc = false;
    let pendingBlk = false;
    let pendingPel = false;
    let pendingPy = false;
    let depth = 0;

    for (let line = 0; line < document.lineCount; line++) {
        const text = document.lineAt(line).text;
        const trimmed = text.trimEnd();
        const structuralBrace = text[0] === '{' || text[0] === '}';
        const isOpen = structuralBrace && trimmed === '{';
        const isClose = structuralBrace && trimmed === '}';

        if (isClose) {
            if (ctx !== 0 && depth === ctxDepth) ctx = 0;
            if (depth > 0) depth--;
            if (structStack.length === 0) {
                add(line, 0, 1, true, "Unmatched '}' (no column-0 '{' to close)");
            } else {
                structStack.pop();
            }
            continue;
        }
        if (isOpen) {
            depth++;
            if (pendingCirc) { ctx = 1; ctxDepth = depth; pendingCirc = false; }
            else if (pendingPel) { ctx = 2; ctxDepth = depth; pendingPel = false; }
            else if (pendingPy) { ctx = 3; ctxDepth = depth; pendingPy = false; }
            structStack.push({ line, c: 0 });
            continue;
        }

        // tables + block headers
        if (/^\s*circuitdeck\s*$/.test(trimmed)) pendingCirc = true;
        else if (/^\s*BLKEDIT\s+"Program Body"\s*$/.test(trimmed)) { pendingBlk = true; pendingPel = true; }
        else if (/^\s*BLKEDIT\s+"Macro Editor"\s*$/.test(trimmed)) { pendingBlk = true; pendingPy = true; }

        // char-level scan for strings and inline pairs
        const commentAt = ctx === 1 ? text.indexOf(';') : (ctx === 2 ? text.indexOf('!') : -1);
        const effEnd = commentAt >= 0 ? commentAt : text.length;
        let inStr = false;
        let strOpen = -1;
        for (let i = 0; i < effEnd; i++) {
            const c = text[i];
            if (c === '"') {
                if (!inStr) {
                    inStr = true;
                    strOpen = i;
                } else {
                    inStr = false;
                }
                continue;
            }
            if (inStr) continue;
            if (c === '(') pairStack.push({ line, c: i, close: ')' });
            else if (c === ')') matchPair(pairStack, ')', line, i, add);
            else if (c === '[') pairStack.push({ line, c: i, close: ']' });
            else if (c === ']') matchPair(pairStack, ']', line, i, add);
            else if (c === '{') pairStack.push({ line, c: i, close: '}' });
            else if (c === '}') matchPair(pairStack, '}', line, i, add);
        }
        if (inStr && ctx !== 3) {
            add(line, strOpen, strOpen + 1, true, 'Unterminated string (missing closing ")');
        }
    }
    for (const open of structStack) {
        add(open.line, open.c, open.c + 1, true, "Unclosed '{' (block never closed)");
    }
    for (const open of pairStack) {
        add(open.line, open.c, open.c + 1, true,
            "Unclosed '" + textChar(document, open) + "' (missing '" + open.close + "')");
    }

    checkTables(document, add);
    return issues;
}

function textChar(document, pos) {
    return document.lineAt(pos.line).text[pos.c] || '{';
}

function matchPair(stack, closeChar, line, col, add) {
    if (stack.length === 0) {
        add(line, col, col + 1, true, "Unmatched '" + closeChar + "'");
        return;
    }
    const open = stack[stack.length - 1];
    if (open.close !== closeChar) {
        add(line, col, col + 1, true, "Unmatched '" + closeChar + "' (expected '" + open.close + "')");
    } else {
        stack.pop();
    }
}

/** param/fullprecision pairing + Optimize Use=Yes bounds (same rules as the IDE plugin). */
function checkTables(document, add) {
    const params = new Map();
    const fullprec = new Map();
    const stack = [];
    let lastHeader = -1;
    for (let line = 0; line < document.lineCount; line++) {
        const text = document.lineAt(line).text;
        const trimmed = text.trimEnd();
        if (trimmed === '{' && text[0] === '{') {
            stack.push({ open: line, header: lastHeader });
            continue;
        }
        if (trimmed === '}' && text[0] === '}') {
            const open = stack.pop();
            if (open && open.header >= 0
                    && document.lineAt(open.header).text.trim() === 'OPTIMEDIT') {
                checkOptimize(document, open.open + 1, line, add);
            }
            continue;
        }
        lastHeader = line;
        const name = quotedName(text, 'param');
        if (name !== null) {
            params.set(name, line);
            continue;
        }
        const fp = quotedName(text, 'fullprecision');
        if (fp !== null) {
            fullprec.set(fp, line);
        }
    }
    for (const [name, line] of params) {
        if (!fullprec.has(name)) {
            add(line, 0, 1, false, 'param ' + name + ' has no matching fullprecision row');
        }
    }
    for (const [name, line] of fullprec) {
        if (!params.has(name)) {
            add(line, 0, 1, false, 'fullprecision ' + name + ' has no matching param row');
        }
    }
}

function quotedName(line, kind) {
    if (!line.startsWith(kind + ' ')) return null;
    const rest = line.substring(kind.length + 1).trim();
    const q = rest.indexOf('"');
    if (q <= 0) return null;
    return rest.substring(0, q).trim();
}

const ELEMENT_RE = /^\s*element\s+(\d+)\s+"([^"]+)"\s+"([^"]*)"/;

function checkOptimize(document, from, to, add) {
    const rows = new Map();
    const first = new Map();
    for (let line = from; line < to; line++) {
        const m = ELEMENT_RE.exec(document.lineAt(line).text);
        if (!m) continue;
        const idx = parseInt(m[1], 10);
        if (!rows.has(idx)) rows.set(idx, new Map());
        rows.get(idx).set(m[2], m[3]);
        if (!first.has(idx)) first.set(idx, line);
    }
    for (const [idx, row] of rows) {
        const use = (row.get('Use') || 'No').trim().toLowerCase();
        if (use !== 'yes' && use !== 'true') continue;
        const line = first.get(idx);
        const name = (row.get('Name') || '').trim();
        if (!name) continue;
        if (name.startsWith('!')) {
            add(line, 0, 1, true, "Use=Yes: parameter name '" + name + "' is unresolved (! prefix)");
            continue;
        }
        const min = parseValue(row.get('Min') || '');
        const max = parseValue(row.get('Max') || '');
        const stored = parseValue(row.get('Stored') || '');
        if (min === null || max === null || stored === null) {
            add(line, 0, 1, true, "Use=Yes: row '" + name + "' needs numeric Min, Max and Stored");
        } else if (stored < min || stored > max) {
            add(line, 0, 1, true,
                "Use=Yes: Stored " + (row.get('Stored') || '').trim()
                + ' outside Min ' + (row.get('Min') || '').trim()
                + ' .. Max ' + (row.get('Max') || '').trim());
        }
    }
}

function parseValue(raw) {
    const s = raw.trim();
    if (!s) return null;
    let i = s.length - 1;
    while (i >= 0 && /[A-Za-z]/.test(s[i])) i--;
    const num = s.substring(0, i + 1).trim();
    const unit = s.substring(i + 1);
    if (!num) return null;
    const base = Number(num);
    if (!isFinite(base)) return null;
    const mults = { '': 1, a: 1e-18, f: 1e-15, p: 1e-12, n: 1e-9, u: 1e-6,
        m: 1e-3, k: 1e3, K: 1e3, M: 1e6, MEG: 1e6, g: 1e9, G: 1e9, t: 1e12, T: 1e12 };
    return mults[unit] === undefined ? null : base * mults[unit];
}

class MdlFoldingRangeProvider {
    provideFoldingRanges(document) {
        const ranges = [];
        const stack = [];
        const lines = document.lineCount;
        for (let i = 0; i < lines; i++) {
            const text = document.lineAt(i).text;
            const trimmed = text.trimEnd();
            if (trimmed === '{' && text[0] === '{') {
                stack.push({ openLine: i });
            } else if (trimmed === '}' && text[0] === '}') {
                const open = stack.pop();
                if (!open || i - open.openLine < 2) {
                    continue;
                }
                // fold range starts at the header line (nearest non-blank line above
                // the opening brace), and the outermost whole-file block is skipped.
                let header = open.openLine - 1;
                while (header >= 0) {
                    const h = document.lineAt(header).text.trim();
                    if (h.length > 0 && h !== '{' && h !== '}') {
                        break;
                    }
                    header--;
                }
                if (header < 0) {
                    header = open.openLine;
                }
                if (header === 0 && i === lines - 1) {
                    continue;
                }
                ranges.push(new vscode.FoldingRange(header, i, vscode.FoldingRangeKind.Region));
            }
        }
        return ranges;
    }
}

const diagnostics = vscode.languages.createDiagnosticCollection('mdl');
let diagTimer = null;

function refreshDiagnostics(document) {
    if (!document || document.languageId !== 'mdl') {
        return;
    }
    const issues = mdCheck(document);
    const mapped = issues.map((i) => new vscode.Diagnostic(
        new vscode.Range(i.line, i.c0, i.line, Math.max(i.c1, i.c0 + 1)),
        i.msg, i.error ? vscode.DiagnosticSeverity.Error : vscode.DiagnosticSeverity.Warning));
    diagnostics.set(document.uri, mapped);
}

async function runDeckChecks() {
    const editor = vscode.window.activeTextEditor;
    if (!editor || editor.document.languageId !== 'mdl') {
        return;
    }
    const issues = mdCheck(editor.document);
    refreshDiagnostics(editor.document);
    const errors = issues.filter((i) => i.error).length;
    const warnings = issues.length - errors;
    if (issues.length === 0) {
        void vscode.window.showInformationMessage('MDL checks passed: no issues found.');
    } else {
        void vscode.window.showWarningMessage(
            `MDL checks: ${errors} error(s), ${warnings} warning(s).`);
    }
}

async function foldDataBlocks() {
    const editor = vscode.window.activeTextEditor;
    if (!editor || editor.document.languageId !== 'mdl') {
        return;
    }
    const ranges = findDataRanges(editor.document);
    const original = editor.selections;
    for (const r of ranges) {
        editor.selections = [new vscode.Selection(r.start, r.end)];
        await vscode.commands.executeCommand('editor.fold');
    }
    editor.selections = original;
}

function activate(context) {
    context.subscriptions.push(
        vscode.languages.registerCompletionItemProvider('mdl', new MdlCompletionProvider(), '#', '$'),
        vscode.languages.registerFoldingRangeProvider('mdl', new MdlFoldingRangeProvider()),
        vscode.commands.registerCommand('mdl.foldDataBlocks', foldDataBlocks),
        vscode.commands.registerCommand('mdl.checkDeck', runDeckChecks),
        vscode.workspace.onDidChangeTextDocument((e) => {
            if (e.document.languageId !== 'mdl') {
                return;
            }
            clearTimeout(diagTimer);
            diagTimer = setTimeout(() => refreshDiagnostics(e.document), 400);
        }),
        vscode.workspace.onDidOpenTextDocument((d) => refreshDiagnostics(d)),
        diagnostics
    );
    for (const doc of vscode.workspace.textDocuments) {
        refreshDiagnostics(doc);
    }
}

function deactivate() {}

module.exports = { activate, deactivate };
