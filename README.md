# File Searcher

Desktop file search and duplicate detection utility built with Java 21, Swing, and Maven.

## Features

- Recursive or single-directory file system scanning (`FileSystemScannerService`).
- Configurable search criteria, match modes, and search targets (`SearchCriteria`, `MatchMode`, `SearchTarget`).
- Duplicate file detection using cryptographic hashing (`DuplicateDetectorService`, `HashAlgorithm`, `DuplicateGroup`).
- File operations and report exporting (`FileActionService`, `ReportExportService`).
- Centralized configuration and logging (`AppConfig`, `LogManager`).

## Requirements

- **Java**: 21
- **Maven**: 3.9+ (or Eclipse's embedded Maven runtime)

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