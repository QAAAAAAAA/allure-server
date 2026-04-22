# Changelog

All notable changes to this project are documented in this file.

## [0.1] - 2026-04-22

### Added

- Added UI-specific report filtering in `Reports`:
  - Always show `active=true` reports.
  - Show only the latest `N` inactive reports per logical `path`.
- Added configurable property `allure.reports.ui-inactive-per-path` with default value `2`.
- Added full-height adaptive table layout and built-in pagination support in shared `FilteredGrid`.
- Added `SYSTEM_PARAMETERS.md` with Docker Compose-ready environment variable names.
- Added a minimal production preset block in `SYSTEM_PARAMETERS.md`.

### Changed

- Removed the `Size KB` column from the Reports UI table.
- Moved the `Created` column to the last position in the Reports UI table.
- Updated `ReportsView` data source to use UI-filtered report list.
- Updated `ReportsView` and `ResultsView` layout to use full-size pages for better table rendering.
