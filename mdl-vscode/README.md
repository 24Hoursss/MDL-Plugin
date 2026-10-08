# IC-CAP MDL (VSCode)

Syntax highlighting, code folding and typing completion for Keysight IC-CAP
`.mdl` (MDIF model definition) files. Mirrors the structural rules used by the
repo's `iccap-mdl-edit` skill/agent (`agents/iccap-mdl-edit/mdl_edit.py`,
`skills/iccap-mdl-edit/reference/grammar.md`).

## Features

- **Highlighting** (TextMate grammar):
  - `LINK` + block kinds (`MODEL` `DUT` `DAT` `SWEEP` `OUT` `XFORM` `PLOT`
    `MACRO` `CIRC` `TCIRC` `PS` `DPS`)
  - line keywords: `list`, `element`, `param`, `fullprecision`, `View`,
    `dataset`, `datasize`, `type`, `member`, `BLKEDIT`, `TABLE`, `HYPTABLE`,
    `OPTIMEDIT`, `data`, `circuitdeck`, `__iccapDataBlockEnd`
  - strings, engineering numbers (`170m`, `9f`, `2.5e-7`)
  - `#load` / `#echo` / `#include` directives, `$dpar()` / `$mpar()` /
    `$xpar()` / `$var()` references
  - inside `circuitdeck` bodies (per `iccap-hpeesofsim`): ADS keywords
    (`define` `model` `end` `parameters` `global` `Options`), VarEqn
    (`if`/`then`/`else`/`endif`/`equals`), functions (`file{}` `list()`
    `dep_data()` `fg()`), instance prefixes (`C:Cpg` `R:Rg` `M:X1`), `;`
    comments, `#uselib`/`#if`-style C preprocessor directives
  - **BLKEDIT "Program Body"**: PEL keywords (`FOR`/`STEP`/`NEXT`,
    `IF`/`ELSE`/`ENDIF`, `WHILE`, `GLOBAL_VAR`, `ICCAP_FUNC`, ...,
    case-insensitive), constants `PI`/`2PI`/`J`, `!` comments
  - **BLKEDIT "Macro Editor"**: embeds the regular Python grammar
    (`source.python`) for full Python highlighting
- **Folding**: column-0 brace blocks fold to their header line (same rule as
  the PyCharm plugin: only lines that are exactly `{`/`}` at column 0 open or
  close a block; indented braces in Program bodies are body text). Implemented
  as a FoldingRangeProvider. A freshly opened .mdl is folded recursively once
  (outline view), the closest VSCode equivalent to the IDE's
  collapsed-by-default blocks; reuse the fold state or run Fold All again as
  needed.
- **Fold measured data quickly**: the command **MDL: Fold Data Blocks**
  (`Ctrl+Alt+Shift+F`) folds every `data` block (the raw point tables) in one
  go. VSCode has no API for "collapsed by default", so this is the equivalent
  of the PyCharm plugin's default-collapsed data blocks.
- **Completion**: at line start (keywords / netlist keywords inside
  `circuitdeck`), after `LINK` (block kinds), after `#` (directives), after
  `$` (parameter references), plus snippets (`link block`, `link transform`,
  `param + fullprecision`).
- **Typing helpers**: auto-closing `{ }` `( )` `" "`, bracket matching,
  indent increase on block headers (`LINK`/`TABLE`/...), decrease on `}`.

## Publish / distribute

`publish.cmd` is the one-command publish check:

1. packages `iccap-mdl-1.0.0.vsix` with `npx @vscode/vsce package`
2. validates the .vsix structure (`validate_vsix.py`)
3. installs it locally via `code --install-extension` (skip with `SKIP_INSTALL=1`)
4. uploads to the VS Code Marketplace if `VSCE_TOKEN` is set, else prints the
   exact steps needed

Marketplace publishing requires a registered publisher (the `publisher` field
in `package.json` must match it) and a PAT:

```bat
set VSCE_TOKEN=<token>
publish.cmd
```

Tokens are issued under https://marketplace.visualstudio.com/manage/publishers.
`set SKIP_PUBLISH=1` runs steps 1-3 only.

## Deck checks (diagnostics)

The extension runs the same checks as the PyCharm plugin and marks problems
in the Problems panel / squiggles as you type (debounced):

- column-0 brace integrity (`{`/`}` at column 0) and unpaired `( )` `[ ]`
  inline braces/strings (excluded)
- unterminated strings (Python bodies excluded)
- `param` / `fullprecision` pairing
- Optimize `Use=Yes` rows: numeric Min and Max required, no `!`-prefixed
  unresolved names (Stored is intentionally not checked)

Command **MDL: Run Deck Checks** (or `set SKIP_PUBLISH=1` + the checks pass
message) summarizes errors/warnings for the active file.

## Install

```bat
install.cmd
```

copies the extension into `%USERPROFILE%\.vscode\extensions\iccap-mdl-1.0.0`
(same effect as "Install from VSIX"; restart VSCode to load it).

The extension registers `*.mdl` itself; additionally a user-level
`files.associations` entry (`"*.mdl": "mdl"`) was added to
`%APPDATA%\Code\User\settings.json` so VSCode always maps the extension to
this plugin (user settings beat extension fallbacks). Backup:
`settings.json.bak-*`; remove that key to revert.

For a shareable vsix:

```bash
npx @vscode/vsce package
code --install-extension iccap-mdl-1.0.0.vsix
```

## Layout

```
package.json                 manifest (language, grammar, activation)
language-configuration.json  brackets/auto-close/indentation
syntaxes/mdl.tmLanguage.json TextMate grammar
extension.js                 completion + folding providers
install.cmd                  copy into extensions dir
```

No runtime dependencies — plain Node with the `vscode` API only.
