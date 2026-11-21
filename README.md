# File Searcher

Desktop file search and duplicate detection utility built with Java 21, Swing, and Maven.

## Features

- Recursive or single-directory file system scanning (`FileSystemScannerService`).
- Configurable search criteria, match modes, and search targets (`SearchCriteria`, `MatchMode`, `SearchTarget`).
- Duplicate file detection using cryptographic hashing (`DuplicateDetectorService`, `HashAlgorithm`, `DuplicateGroup`).
- File operations and report exporting (`FileActionService`, `ReportExportService`).
- Centralized configuration and logging (`AppConfig`, `LogManager`).
- Standalone portable runtime package (`portable-runtime`) with launcher scripts (`launch.bat` and `launch.sh`).

## Requirements

### Running the Portable Release (Recommended)
- **Operating System**: Windows (x64) for the bundled Windows runtime, or Linux/macOS with a compatible Java 21 runtime when using `launch.sh`.
- **Java**: Not required on Windows if using the bundled `portable-runtime` directory; otherwise **Java 21** is required.

### Building from Source
- **Java**: 21
- **Maven**: 3.9+ (or Eclipse's embedded Maven runtime)

## Usage

### 1. Download and Extract the Release Archive
1. Download the release archive (for example, `file-searcher-1.0.0-windows-x64.zip`) from the **Releases** section.
2. Extract its contents into a destination folder of your choice (for example, `C:\Tools\FileSearcher\` or a folder on a USB drive).

### 2. Launch the Application

#### On Windows
Run `launch.bat` from the extracted folder (either by double-clicking it or from Command Prompt / a portable launcher):

```cmd
launch.bat
```

#### On Linux / macOS

Make `launch.sh` executable with `chmod` and run it from the extracted folder:

```bash
chmod +x launch.sh
./launch.sh
```

*(If `launch.sh` is executed from another directory, `cd` into the extracted folder first so relative paths to `file-searcher-1.0.0.jar` and the runtime resolve properly).*

### 3. Building and Running with Maven (Optional)

From the project root directory:

```bash
mvn clean package
```

## Help

### Searching for Files

1. Select the target folder or drive to scan and choose whether scanning should be recursive or limited to a single directory (`FileSystemScannerService`).
2. Configure your search filters (`SearchCriteria`), such as file name, pattern, size, or other supported attributes.
3. Choose how criteria are evaluated (`MatchMode`) and what items are targeted (`SearchTarget`, such as files or directories).
4. Start the scan and review the matching entries (`SearchResultItem`).

### Finding Duplicate Files

1. Run a scan or point the duplicate detector (`DuplicateDetectorService`) at the target folder(s).
2. Select the hashing algorithm (`HashAlgorithm`) to group files with identical content (`DuplicateGroup`).
3. Inspect each duplicate group carefully before taking action so you keep the copy you want.

### File Actions, Reports, Configuration, and Logs

* **File Actions (`FileActionService`)**: Perform supported actions on selected search results or duplicate items.
* **Exporting Reports (`ReportExportService`)**: Export search or duplicate findings into a report file for later review or auditing.
* **Configuration & Logs (`AppConfig`, `LogManager`)**: Application settings and runtime/error logs are managed centrally—check the application logs if a scan encounters permission errors or inaccessible system folders.

### Troubleshooting

* **Permission / Access Denied Errors**: Some system or protected directories require elevated permissions or may fail due to OS locks. Check the logs via `LogManager` for details on skipped or unreadable paths.
* **`launch.sh: Permission denied`**: Run `chmod +x launch.sh` before executing `./launch.sh`.
* **JAR or Runtime Not Found**: Make sure `launch.bat` / `launch.sh`, `file-searcher-1.0.0.jar`, and the runtime directories (`bin`, `conf`, `lib`, `legal`, `release`) remain together in the same extracted folder.

## Project Structure

```text
src/main/java/com/josegonzalezal/filesearcher/
├── MainApp.java
├── config/
│   ├── AppConfig.java
│   └── LogManager.java
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
    └── MainFrame.java
```
