# IC-CAP MDL Plugin (PyCharm)

Syntax highlighting, code folding and brace matching for Keysight IC-CAP `.mdl`
(MDIF model definition) files in PyCharm 2026.1 - 2026.2
(`since-build=261 until-build=262.*`).

## Features

- **File type**: `*.mdl` files open as "IC-CAP MDL". Registered by the
  `<fileType>` extension point in plugin.xml (`fieldName="INSTANCE"`); the
  deprecated `FileTypeFactory` was dropped in 1.0.4.
- **Highlighting**, based on the MDL grammar used by the repo's
  `iccap-mdl-edit` skill/agent (`agents/iccap-mdl-edit/mdl_edit.py`,
  `skills/iccap-mdl-edit/reference/grammar.md`):
  - block headers: `LINK`, `list`, `TABLE`, `HYPTABLE`, `BLKEDIT`,
    `element`, `param`, `fullprecision`, `View`, `data`, `circuitdeck`,
    `OPTIMEDIT`, `member`, `dataset`/`datasize`/`type`, `__iccapDataBlockEnd`
  - second word of a `LINK` header (`MODEL`, `DUT`, `DAT`, `SWEEP`, `OUT`,
    `XFORM`, `PLOT`, `MACRO`, `CIRC`, `TCIRC`, `PS`, `DPS`)
  - strings, engineering numbers with suffix (`170m`, `9f`, `75.00u`,
    `2.5e-7`), simulator directives `#echo` / `#load` / `#include`,
    parameter references `$dpar(...)` / `$mpar(...)` / `$xpar(...)` / `$var(...)`
  - **circuitdeck bodies** (per `iccap-hpeesofsim`): ADS keywords (`define`,
    `model`, `end`, `parameters`, `global`, `globalnode`, `ground`, `Options`),
    VarEqn keywords (`if`/`then`/`else`/`endif`/`equals`), DAC/expression
    functions (`file{}` `list()` `dep_data()` `fg()` `abs` ...), instance
    prefixes (`C:Cpg`, `R:Rg`, `M:X1`, `S_Port:S2P1`) and `;` comments.
    The lexer tracks the column-0 brace depth, so Program/Python bodies
    never leak into the netlist classification.
  - **BLKEDIT "Program Body"** (per `iccap-pel`): PEL statements
    (`FOR/STEP/NEXT`, `IF/ELSE/ENDIF`, `WHILE/ENDWHILE`, `GLOBAL_VAR`,
    `GET_REAL`, `ICCAP_FUNC`, `RETURN_VALUE`, ... case-insensitive), constants
    `PI`/`2PI`/`J` and `!` comments.
  - **BLKEDIT "Macro Editor"** (per `iccap-python`): body text is Python, so
    `#` starts a line comment (including `# depend` / `# update` directives);
    Python keyword coloring is provided by VSCode's embedded `source.python`
    grammar.
- **Folding**: column-0 brace blocks fold to their header line
  (`LINK XFORM "opt" "790.004"` -> `LINK PS "Parameter Set" ...` etc.).
  The structural-brace rule is the same one `mdl_edit.py` uses: only lines
  that are exactly `{` or `}` at column 0 open/close a block; indented braces
  inside Program bodies are body text and are never folded.
  The pair that spans the whole file (outermost `LINK MODEL`) is not folded.
- **Brace matching**: `{ }` and `( )` pairs (highlight matching brace,
  pair deletion, "move to matching brace"). Matching ignores quotes.
- **Typing completion** (Ctrl+Space / autopopup): line-start keywords, LINK
  kinds after `LINK `, `#load`/`#echo`/`#include`/`#define` after `#`,
  `$dpar`/`$mpar`/`$xpar`/`$var` after `$`, netlist keywords
  (`define`/`model`/`end`/...) when the caret is inside a `circuitdeck` body
  (same context rules as the VSCode extension).
- **Every block opens collapsed (recursive)**: a deck opens as an outline of
  headers (`LINK MODEL` / `TABLE` / `data` / `LINK XFORM` ...) and expands on
  demand; expand with `Ctrl+=` / `Ctrl+Shift+=` or the gutter icon. The
  outermost whole-file pair stays expanded so the model header stays visible.

## Large files

PyCharm disables code insight for files over 2.56 MB by default
(`idea.max.intellisense.filesize`), which shows the yellow banner
"File size (...) exceeds configured limit". Since .mdl decks embed measured
data, a custom properties file was written to
`%APPDATA%\JetBrains\PyCharm2026.1\idea.properties`:

```
idea.max.intellisense.filesize=-1
idea.max.content.load.filesize=-1
```

`-1` means unlimited (also raise the 20 MB content-load limit so big decks
open fully). Restart PyCharm after changing it. Re-set a number (in KB) if
you ever want to cap it again.

The file is per IDE version: upgrading to PyCharm 2026.2 installs a fresh
config dir, and this tweak has to be re-created there
(`%APPDATA%\JetBrains\PyCharm2026.2\idea.properties`).
- Colors follow the editor scheme (light and dark themes both work) via
  `DefaultLanguageHighlighterColors` keys; custom keys can be tweaked later
  under Settings -> Editor -> Color Scheme -> Language Defaults if desired.

## Build

Prerequisites: an installed PyCharm whose platform the plugin is built
against (currently 2026.2.3 at
`C:\Program Files\JetBrains\PyCharm 2026.2.3`; its bundled JBR supplies
javac) and Python 3 (`py -3`) for `pack.py` / `validate_plugin.py`. Override
the IDE with `set MDL_IDE=<path>` if it lives elsewhere - the jar embeds no
IDE path, but compiling against an older IDE keeps the bytecode within that
release's API.

```bat
build.bat          :: compiles, packages, copies into the user plugins dir
```

or from Git Bash:

```bash
./build.sh
```

The build compiles `src/mdlide/*.java` against
`<PyCharm>/lib/*` (the platform jars) with `--release 21`, packs
`build/mdl-ide.jar` (classes + `META-INF/plugin.xml`) with `pack.py` (the JBR
ships no `jar` tool) and copies it to:

```
%APPDATA%\JetBrains\PyCharm2026.2\plugins\mdl-ide\lib\mdl-ide.jar
```

## Install / update

Either run `build.bat`, or copy the jar manually to the path above. Restart
PyCharm. The plugin appears in Settings -> Plugins as **IC-CAP MDL**.

`"Text"`, then reopen the file. The plugin registers the `mdl` extension
itself; a verified check found no disabled-plugin entry and no competing
file-type pattern, so after a full PyCharm restart `.mdl` opens as the MDL
file type by default (check the status bar: it shows **MDL**).

## Publish / distribute

`verify.cmd` (or `./verify.sh`) is the pre-flight: it builds + packs the
plugin and runs the **JetBrains Plugin Verifier** - the tool behind the
Marketplace review - against the IDE builds in `IDE_DIRS`. Needs the verifier
CLI jar and an unpacked IDE (defaults:
`%USERPROFILE%\.jetbrains-verifier\verifier-cli-1.410-all.jar` and
`...\ide-261` = build 261.27258.48, the one the Marketplace checks against):

```bat
verify.cmd
set IDE_DIRS=%IDE_DIRS%;C:\Program Files\JetBrains\PyCharm 2026.2.3  &  verify.cmd
```

A run must end in `Compatible` / `OK: verifier found no problems` - the
verifier is what flagged the deprecated `FileTypeFactory` in 1.0.3. The
verifier CLI needs `github.com` and `plugins.jetbrains.com` reachable (it
fetches the documented-problems list and plugin dependencies).

`publish.cmd` is the one-command publish check:

1. compiles the plugin (no IDE plugins-dir write, safe while PyCharm runs)
2. packs the Marketplace distribution zip `dist\mdl-ide-1.0.5.zip`
   (root holds only `lib/`, plugin.xml inside the jar; `pack.py --zip` also
   writes `dist\verify\...-verify.zip` in the wrapper layout the verifier CLI
   wants)
3. validates the zip (`validate_plugin.py`): structure, plugin.xml,
   idea-version, `<fileType>` registration (no `fileTypeFactory`)
4. uploads to plugins.jetbrains.com if `JB_MARKETPLACE_TOKEN` and
   `JB_PLUGIN_ID` are set, else prints the exact steps needed

For Marketplace publishing create a personal access token on
plugins.jetbrains.com (account page) and use the numeric plugin id shown on
the plugin page:

```bat
set JB_PLUGIN_ID=<id>
set JB_MARKETPLACE_TOKEN=<token>
publish.cmd
```

Or just upload `dist\mdl-ide-1.0.5.zip` manually on the plugin page's Update
tab. `set SKIP_PUBLISH=1` runs steps 1-3 only. The zip can also be installed
with Settings -> Plugins -> Gear -> Install Plugin from Disk.

### Marketplace compliance (1.0.4)

The 1.0.3 review flagged the plugin for the deprecated `FileTypeFactory` and
noted that it could not be enabled/disabled without an IDE restart. Both came
from the same extension point:

- `<fileTypeFactory>` -> `<fileType name="MDL" language="MDL"
  implementationClass="mdlide.MdlFileType" fieldName="INSTANCE"
  extensions="mdl"/>`. The EP is declared `dynamic="true"`, so the restart
  warning is gone, and it is the documented replacement for the deprecated
  factory (`MdlFileType.INSTANCE` must stay named as in plugin.xml).
- Verified locally with verifier 1.410 against `IC-261.27258.48` and
  `PY-262.10968.92`: `Compatible`, no deprecated/internal API usages, no
  structural findings. Compatibility was widened to `until-build="262.*"` so
  PyCharm 2026.2 can install it.

## Deck checks (errors / warnings in the editor)

An annotator (`MdlAnnotator` / `MdlCheck`) flags problems with red squiggles
(errors) and yellow (warnings), never blocking the file from opening:

- **Brace integrity**: unmatched `}` / unclosed `{` at column 0 (same rule as
  `mdl_edit.py`), plus unpaired `( )` `[ ]` and inline `{ }` (strings excluded).
  Program Body (PEL) and Macro Editor (Python) bodies are skipped: their
  parentheses and brackets belong to the embedded language, and `#` starts a
  comment there, so prose like `# fraction (0,1]; larger = smoother` is not
  parsed as deck markup.
- **Strings**: a `"` without a closing quote on the same line (Python docstring
  bodies excluded).
- **param/fullprecision**: a `param` row without its `fullprecision` twin
  (agent rule: always update both) and vice versa.
- **Optimize Use=Yes rows**: numeric Min and Max required; a `!`-prefixed
  unresolved name cannot be used. Stored is intentionally not checked.
- **Legacy leaf optimizer tables**: an Optimize `TABLE "Param Table"` header
  followed directly by `element` rows is not canonical — IC-CAP 2025 saves
  open the table with `{` on the next line (`verify` in iccap-mdl-edit and
  `checkOptimize` diagnose this as well)
- **Empty sentinel rows**: the last row of the Variable Table and of an
  Optimize's Param / Input Table must be blank (empty `Name` / `Target`) —
  IC-CAP needs that trailing empty row to import the table.

Run from the CLI: `mdlide.test.CheckDump` (see tools-src) or
`MdlCheck.check(String)` — the same logic is exposed headlessly.

## Self-test (no IDE needed)

```bash
"C:/Program Files/JetBrains/PyCharm 2026.2.3/jbr/bin/javac.exe" \
  -cp "build/classes;C:/Program Files/JetBrains/PyCharm 2026.2.3/lib/*" \
  -d build/test tools-src/LexerDump.java
"C:/Program Files/JetBrains/PyCharm 2026.2.3/jbr/bin/java.exe" \
  -cp "build/test;build/classes;C:/Program Files/JetBrains/PyCharm 2026.2.3/lib/*" \
  mdlide.test.LexerDump <some.mdl> 0
```

Prints the token stream, token counts and brace balance; `braceBalance=0`
over the whole file is the pass condition.

## Source layout

```
src/mdlide/
  MdlLanguage.java            language definition (id MDL)
  MdlFileType.java            *.mdl file type (INSTANCE is named in plugin.xml)
  MdlTokenTypes.java          lexer token types
  MdlLexer.java               line-oriented lexer (column-0 brace depth state)
  MdlSyntaxHighlighter.java   token -> color mapping
  MdlHighlighterFactory.java  per-language highlighter
  MdlParserDefinition.java    flat syntax tree (all tokens as leaves)
  MdlPsiFile.java             file PSI
  MdlFoldingBuilder.java      folds column-0 brace blocks to their header line
  MdlBraceMatcher.java        { } / ( ) pairing
resources/META-INF/plugin.xml
tools-src/LexerDump.java      command-line lexer self-test
build.bat / build.sh         compile + package + install
publish.cmd                  compile + pack Marketplace zip (+ upload)
verify.cmd / verify.sh       JetBrains Plugin Verifier pre-flight
pack.py                      jar + zip packing (no JDK jar tool needed)
validate_plugin.py           structure/descriptor check for the dist zip
```

## Notes

- MDL has no comment syntax, so there is no commenter (Ctrl+/ is a no-op on
  `element`/`param` lines by design).
- Folding/brace rules deliberately match `agents/iccap-mdl-edit`'s parser:
  the file's structure is more like "lines + column-0 braces" than a
  bracket-grammar, so a fold region always starts at the header line.
