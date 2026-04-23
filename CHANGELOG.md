# Changelog

All notable changes to this project are documented in this file.

## [0.1] - 2026-04-22

### Added

- Added UI-specific report filtering in `Reports`:
  - Always show `active=true` reports.
  - Show only the latest `N` inactive reports per logical `path`.
- Added configurable property `allure.reports.ui-inactive-per-path` with default value `2`.
- Added configurable property `allure.clean.results-age-days` (default `90`) to clean old `results` directories.
- Added full-height adaptive table layout and built-in pagination support in shared `FilteredGrid`.
- Added enhanced pagination controls in `FilteredGrid` (`First`, `Prev`, `Next`, `Last`) with range summary text.
- Added `SYSTEM_PARAMETERS.md` with Docker Compose-ready environment variable names.
- Added a minimal production preset block in `SYSTEM_PARAMETERS.md`.

### Changed

- Removed the `Size KB` column from the Reports UI table.
- Removed the `Build` column from the Reports UI table.
- Removed the `Size KB` column from the Results UI table.
- Moved the `Created` column to the last position in the Reports UI table and set default sorting by `Created` in descending order.
- Set the left navigation drawer to be collapsed by default.
- Reduced the `Active` column width in the Reports table and tuned compact filter input sizing for short columns.
- Removed footer `Count` display from tables.
- Reduced horizontal overflow in table rendering by updating column sizing and filter layout to avoid page-level horizontal scrolling.
- Updated `ReportsView` data source to use UI-filtered report list.
- Updated `ReportsView` and `ResultsView` layout to use full-size pages for better table rendering.
- Modernized `FilteredGrid` presentation (header filters with search icon, cleaner typography, refined borders, and improved footer/pagination layout).
- Updated Docker Compose examples to use normalized Spring environment variable names.
- Fixed `FilteredGrid` filter row alignment so filters bind to the correct columns when selection column is enabled.
