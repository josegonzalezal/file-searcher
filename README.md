# File Searcher

Desktop file search and duplicate detection utility built with Java 21+, Swing, and Maven.

---

## ⚠️ Important Warnings & Limitations

Please read these warnings before running scans or deleting files:

- **Large Directories & RAM Exhaustion (OutOfMemoryError / System Slowdown):**
  - Scanning entire drives (e.g., `C:\` or `/`) or folders containing hundreds of thousands/millions of files can consume significant RAM and CPU—especially in **Find Duplicates** mode when hashing many large files or holding large candidate maps in memory.
  - **Recommendation:** Target specific subdirectories rather than root drives whenever possible, use **Min Size / Max Size** or **Extensions** to narrow the scope, and keep **Cap results** enabled.
- **Display & Directory Traversal Limits:**
  - **Result Display Cap:** By default, the UI caps displayed rows (e.g., `10,000` results via **Cap results**) to prevent the Swing table from freezing. Even if more matches exist on disk, only rows up to the cap are rendered in the table.
  - **Deep / Protected System Directories:** Scanning OS-level system directories (such as `C:\Windows`, `System Volume Information`, `/proc`, `/sys`, or `/dev`) may trigger frequent `AccessDeniedException` entries, slow down traversal, or hit OS path-length limits.
- **Permanent File Deletion (No Recycle Bin / Trash):**
  - Clicking **Delete Selected** in **Find Duplicates** mode **permanently deletes** files from disk (`Files.deleteIfExists`)—it does **not** move them to the Windows Recycle Bin or macOS/Linux Trash, and the action cannot be undone.
  - Although File Searcher enforces a safety check requiring at least one copy per duplicate group to remain unselected, always verify the selected paths before confirming deletion.
- **"File size only (fastest)" Duplicate Mode Caution:**
  - Selecting **File size only (fastest)** groups files solely by identical byte count without comparing file contents. Two completely different files that happen to have the exact same byte size will be grouped together—never delete files based on size-only matching without verifying their contents or running a cryptographic hash (`SHA-256`, `SHA-1`, or `MD5`).
- **Symbolic Links & Junctions (`Skip symlinks`):**
  - Disabling **Skip symlinks** allows traversal into symbolic links or directory junctions, which can cause duplicate scans of the same directory tree or infinite directory loops on certain file systems. Keep **Skip symlinks** checked unless you specifically need to follow links.
- **Locked or In-Use Files:**
  - Files actively locked by the operating system or other running programs cannot be read for hashing or deleted and will be logged as skipped/failed.

---

## Local-Only & Safety Assurance

- **100% Offline / Local Execution:** File Searcher runs locally on your machine to scan directories, compute file hashes, open or delete files you choose, and export local reports/logs. There is no account system, telemetry, or cloud sync built into the application.
- **Not Spyware or a Virus:** The source code is open in this repository (`src/main/java/...`), written in standard Java and Swing without hidden background daemons or network-exfiltration modules.
- **How to Verify Safely Without Modifying or Breaking the Program:**
  1. **Inspect the source code directly:** Read the classes under `src/main/java/com/josegonzalezal/filesearcher/` (such as `FileSystemScannerService.java`, `DuplicateDetectorService.java`, `FileActionService.java`, `ReportExportService.java`, and `MainFrame.java`) to see every operation performed on disk.
  2. **Scan the archive or portable folder with your antivirus / Defender:** Right-click the `.zip` or extracted folder and run a standard scan—this is read-only and leaves your files intact.
  3. **Verify the JAR / release checksums or build from source:** Run `mvn test` and `mvn clean package` in a clean folder to compile from source yourself, or compare SHA-256 hashes of your downloaded release asset.
  4. **Test on a disposable folder first:** Point **Search Roots** at a small temporary test folder before running scans or duplicate deletions on important data.

---

## Features

- Multi-root file and folder scanning (`FileSystemScannerService`).
- Configurable search criteria, match modes, literal name toggle, and search targets (`SearchCriteria`, `MatchMode`, `SearchTarget`).
- Duplicate file detection with selectable comparison/hashing methods (`DuplicateDetectorService`, `HashAlgorithm`, `DuplicateGroup`).
- File actions, CSV/TXT/LOG exporting, and light/dark Swing theming (`FileActionService`, `ReportExportService`, `MainFrame`).
- Centralized configuration and logging (`AppConfig`, `LogManager`).
- Standalone portable runtime package (`portable-runtime`) with launcher scripts (`launch.bat` and `launch.sh`).

## Requirements

### Running the Portable Release (Recommended)

- **Operating System:** Windows (x64) for the bundled Windows runtime, or Linux/macOS with a compatible Java runtime when using `launch.sh`.
- **Java:** Bundled in the portable runtime package (no separate Java installation required when using the included runtime), or a compatible Java runtime if running the JAR directly.

### Building from Source

- **Java:** 21 (`maven.compiler.release` is configured for Java 21)
- **Maven:** 3.9+ (or an IDE with Maven support)

## Usage

### 1. Download and Extract the Release Archive

1. Download the release archive (for example, `file-searcher-1.0.6.zip` or your platform release zip) from the **Releases** section.
2. Extract it **once** into a clean destination folder (for example, `C:\Tools\FileSearcher\`). Avoid extracting into a duplicate nested folder path.

### 2. Launch the Application

#### On Windows

Run `launch.bat` from the portable folder:

```cmd
launch.bat
```

#### On Linux / macOS

Make `launch.sh` executable and run it from the script's directory:

```bash
chmod +x launch.sh
./launch.sh
```

### 3. Building and Running with Maven (Optional)

From the project root directory:

```bash
mvn clean package
```

## Guide: How to Use File Searcher

### Root Source Directories (**Search Roots**)

- The **Search Roots** panel lists folders and drives to scan (such as your user folder or drives like `C:\`, `D:\`, `E:\`).
- Use **Add Folder...** to add a directory or **Remove** to remove a listed entry.
- **Which roots get scanned:** If you highlight/select one or more roots in the list, only those selected roots are scanned; if no root is selected, all listed roots are scanned.

### Folder & File Search (**Find Files / Folders**)

Set **Mode** to **Find Files / Folders** and configure your filters under **Search & Filter Options**:

- **Name / Pattern + Match Mode:** Evaluates a single name or pattern using the selected mode: **Name contains** (`CONTAINS`), **Exact name** (`EXACT`), **Starts with** (`STARTS_WITH`), **Ends with** (`ENDS_WITH`), **Wildcard (`*`, `?`)** (`WILDCARD`), or **Regular expression** (`REGEX`). Surrounding spaces are trimmed when literal mode is off.
  - *Single-term example:* Type `report` with **Name contains** to match `annual_report_2026.pdf`.
  - *Multi-term example:* Switch **Match** to **Regular expression** and separate terms with `|` (for example: `invoice|receipt|contract`).
- **Literal Name Toggle (`Literal: OFF` / `Literal: ON`):** Toggle **Literal: ON** to treat the text in **Name / Pattern** strictly as plain literal text rather than a wildcard or regex pattern—preserving leading and trailing spaces.
- **Target (File or Folder):** Use **Target** to restrict results to files, folders, or both (`FILES`, `FOLDERS`, `BOTH`).
- **Extensions:** Enter one or more extensions separated by **semicolons (`;`)**, **commas (`,`)**, or **spaces**. Leading dots (`.jpg`) are stripped automatically.
  - *Valid examples:*
    - `jpg; png; docx`
    - `jpg, png, docx`
    - `jpg png docx`
- **Min Size & Max Size:** Enter **only one size value per box** using units `B`, `KB`, `MB`, `GB`, or `TB` (do **not** separate multiple sizes with `;` inside a single box). Leave either box empty if you don't want a lower or upper limit.
  - *Example (files between 64 bytes and 2 KB):*
    - **Min Size:** `64B`
    - **Max Size:** `2KB`
  - *Example (files at least 50 MB with no upper limit):*
    - **Min Size:** `50MB`
    - **Max Size:** *(leave blank)*
- **Flags:**
  - **Case sensitive:** Makes name/pattern matching case-sensitive.
  - **Include hidden:** Includes hidden files and directories during traversal.
  - **Skip symlinks:** Skips symbolic links to avoid loops or unintended targets.
  - **Cap results:** Caps displayed rows (for example, at `10000`) to keep the UI responsive on large scans.

Click **Search** to start or **Cancel** to stop an active scan.

### Duplicate File Search (**Find Duplicates**)

Set **Mode** to **Find Duplicates** to locate duplicate files across your target roots:

1. Select your **Search Roots** and any optional filters (**Name / Pattern**, **Literal**, **Min Size / Max Size**, **Extensions**, **Case sensitive**, **Include hidden**, **Skip symlinks**, **Cap results**).
2. Choose an **Algorithm** from the dropdown (for example, **File size only (fastest)** or cryptographic hash algorithms such as `SHA-256`, `SHA-1`, or `MD5` in `HashAlgorithm`).
3. Click **Find Duplicates** to scan and group matching files (or **Cancel** to abort).

#### How Each Duplicate Search Method Works

- **File size only (fastest):** Groups candidate files that share the exact same byte length without reading full file contents—fastest for quick passes, though files with the same size aren't guaranteed to be byte-for-byte identical.
- **Hash-based algorithms (`SHA-256`, `SHA-1`, `MD5`):** Narrows candidates by file size and computes content digests so files with matching hashes have identical byte contents (with `SHA-256` offering the strongest collision resistance and `MD5`/`SHA-1` being legacy hash options).

### What to Do With Search or Duplicate Results

- **Inspect & Open:** Double-click a row—or select a row and click **Open File** / **Open Folder** (or right-click for the context menu)—to open the file or its containing folder.
- **Copy Paths:** Select one or more rows and click **Copy Path** (or use the context menu) to copy full paths to the clipboard.
- **Export or Clear:** Click **Export CSV** or **Export TXT** to save the current table results, or **Clear Results** to empty the table.
- **Clean Up Duplicates Safely (in Find Duplicates mode):**
  1. Review the groups shown in the table (**Group / Hash**, **File Name**, **Size**, **Reclaimable**, **Last Modified**, **Full Path**).
  2. Click **Select Duplicates** to automatically select duplicate copies while leaving the first copy in each group unselected—or pick specific rows manually.
  3. Click **Delete Selected** to permanently delete selected files. A safety check blocks deletion if all copies of any group are selected so at least one copy per group is preserved.

## Logs & Troubleshooting

- **Logs Panel:** Status updates and scan/error messages appear in the **Logs** pane at the bottom. Use **Export Log** to save logs to a `.log`/`.txt` file or **Clear Log** to clear the log display.
- **Permission / Access Denied Errors:** Protected system folders or locked files may be skipped; check the logs for details.
- **`launch.sh: Permission denied`:** Run `chmod +x launch.sh` before running `./launch.sh`.
- **JAR or Runtime Not Found:** Ensure `launch.bat` / `launch.sh`, your `file-searcher-*.jar`, and the runtime files remain together in the extracted folder and match the configured path.

## Project Structure

```text
src/main/java/com/josegonzalezal/filesearcher/
├── MainApp.java
├── config/
│   ├── AppConfig.java
│   ├── LogManager.java
│   └── ThemePreferences.java
├── model/
│   ├── DuplicateGroup.java
│   ├── HashAlgorithm.java
│   ├── MatchMode.java
│   ├── SearchCriteria.java
│   ├── SearchResultItem.java
│   └── SearchTarget.java
├── service/
│   ├── DuplicateDetectorService.java
│   ├── FileActionService.java
│   ├── FileSystemScannerService.java
│   └── ReportExportService.java
└── ui/
    ├── MainFrame.java
    └── ThemePalette.java
```