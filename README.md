# IC-CAP MDL Editor Plugins

Syntax highlighting, code folding, typing completion and deck checks
(brace integrity, strings, param/fullprecision pairing, Optimize bounds)
for Keysight IC-CAP `.mdl` model files — for PyCharm and VS Code.

Structural rules follow the repo's `iccap-mdl-edit` skill/agent
(`agents/iccap-mdl-edit/mdl_edit.py`, `skills/iccap-mdl-edit/reference/grammar.md`):
only lines whose sole character is `{` or `}` at column 0 open or close a block;
indented braces inside Program/Python bodies are body text.

## Contents

| Folder | What |
|--------|------|
| `mdl-ide-plugin/` | PyCharm (IntelliJ platform, Java, javac-built, no Gradle) |
| `mdl-vscode/`     | VS Code extension (TextMate grammar + JS providers, no npm deps) |

Both implement the same feature set so the two editors agree on a deck:

- highlighting: LINK/TABLE/element/param blocks, `#echo`/`$dpar()`,
  ADS netlist (`;` comments, `define`/`model`, instances `C:Cpg`, VarEqn
  `if/then/else/endif`, DAC functions), PEL Program bodies (`!` comments,
  `FOR/STEP`, `PI`/`J`), Python Macro Editor bodies
- folding: column-0 brace blocks fold to their header line; measured `data`
  blocks open collapsed (PyCharm) / one-command fold (VS Code)
- completion: line-start keywords, LINK kinds, `#` directives, `$` refs,
  netlist keywords inside `circuitdeck`
- checks: brace/bracket/paren balance, unterminated strings,
  param/fullprecision pairing, Optimize `Use=Yes` bounds, empty sentinel rows
  on Variable Table / Optimize Param and Input tables

Build/install/publish per plugin: see `mdl-ide-plugin/README.md`
(`build.bat` / `publish.cmd`) and `mdl-vscode/README.md`
(`install.cmd` / `publish.cmd`).
