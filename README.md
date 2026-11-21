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
