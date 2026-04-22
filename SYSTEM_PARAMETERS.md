# System Parameters (Docker Compose Ready)

This document lists runtime parameters using Docker Compose environment variable names.
All keys below can be used directly under `services.<name>.environment`.

## Quick Example

```yaml
environment:
  ALLURE_REPORTS_UI_INACTIVE_PER_PATH: 2
  ALLURE_REPORTS_HISTORY_LEVEL: 20
  SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE: 1000MB
```

## Minimal Production Preset

Copy and paste this block into `docker-compose.yaml` under `services.allure-server.environment`.

```yaml
environment:
  # Network
  SERVER_PORT: 8080

  # Database (PostgreSQL example)
  SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/allure
  SPRING_DATASOURCE_USERNAME: postgres
  SPRING_DATASOURCE_PASSWORD: postgres
  SPRING_JPA_DATABASE: postgresql
  SPRING_JPA_HIBERNATE_DDL_AUTO: update

  # Upload limits
  SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE: 1000MB
  SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE: 1000MB

  # Report storage and UI behavior
  ALLURE_RESULTS_DIR: /allure/results/
  ALLURE_REPORTS_DIR: /allure/reports/
  ALLURE_REPORTS_PATH: reports/
  ALLURE_REPORTS_HISTORY_LEVEL: 20
  ALLURE_REPORTS_UI_INACTIVE_PER_PATH: 2

  # Cleanup
  ALLURE_CLEAN_DRY_RUN: "false"
  ALLURE_CLEAN_AGE_DAYS: 180
  ALLURE_CLEAN_TIME: "00:00"

  # Security (enable only if needed)
  BASIC_AUTH_ENABLE: "false"
  BASIC_AUTH_USERNAME: admin
  BASIC_AUTH_PASSWORD: admin
```

## Core Server Parameters

| Environment Variable | Default Value | Description |
|---|---|---|
| `PORT` | `8080` | Optional shortcut variable for the HTTP port. |
| `SERVER_PORT` | `8080` | HTTP port used by the server. |
| `VAADIN_URL_MAPPING` | `/ui/*` | URL mapping for the Vaadin UI. |

## Spring Runtime Parameters

| Environment Variable | Default Value | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:h2:file:./allure/db` | JDBC URL for the application database. |
| `SPRING_DATASOURCE_USERNAME` | `sa` | Database username. |
| `SPRING_DATASOURCE_PASSWORD` | _(empty)_ | Database password. |
| `SPRING_JPA_DATABASE` | `H2` | Target database platform for JPA. |
| `SPRING_JPA_SHOW_SQL` | `false` | Enables SQL logging when set to `true`. |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Hibernate schema strategy (`create`, `update`, etc.). |
| `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE` | `100MB` | Max uploaded file size for a single file. |
| `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE` | `100MB` | Max total multipart request size. |

## Allure Parameters

| Environment Variable | Default Value | Description |
|---|---|---|
| `ALLURE_TITLE` | `BrewCode \| Allure Report` | UI title shown in the application. |
| `ALLURE_LOGO` | `""` | Logo resource path or URL. |
| `ALLURE_RESULTS_DIR` | `allure/results/` | Storage directory for uploaded/unzipped results. |
| `ALLURE_REPORTS_DIR` | `allure/reports/` | Storage directory for generated reports. |
| `ALLURE_REPORTS_PATH` | `reports/` | Public URL path prefix for active reports. |
| `ALLURE_REPORTS_HISTORY_LEVEL` | `20` | Max number of reports preserved per logical `path`. |
| `ALLURE_REPORTS_UI_INACTIVE_PER_PATH` | `2` | Number of latest inactive reports displayed per logical `path` in UI tables. |
| `ALLURE_SUPPORT_OLD_FORMAT` | `false` | Enables migration support for old report format. |
| `ALLURE_DATE_FORMAT` | `yy/MM/dd HH:mm:ss` | Date-time format used in UI tables. |
| `ALLURE_SERVER_BASE_URL` | _(empty)_ | Explicit external base URL for proxy/ingress setups. |

## Cleanup Parameters

| Environment Variable | Default Value | Description |
|---|---|---|
| `ALLURE_CLEAN_DRY_RUN` | `false` | If `true`, cleanup only logs candidates without deleting. |
| `ALLURE_CLEAN_TIME` | `00:00` | Daily cleanup scheduler trigger time. |
| `ALLURE_CLEAN_AGE_DAYS` | `90` | Global report retention in days. |
| `ALLURE_CLEAN_PATHS_0_PATH` | `manual_uploaded` | Logical report path for the first path-specific retention rule. |
| `ALLURE_CLEAN_PATHS_0_AGE_DAYS` | `30` | Retention (days) for `ALLURE_CLEAN_PATHS_0_PATH`. |

## Basic Auth Parameters

| Environment Variable | Default Value | Description |
|---|---|---|
| `BASIC_AUTH_ENABLE` | `false` | Enables HTTP Basic Authentication for UI and API. |
| `BASIC_AUTH_USERNAME` | `admin` | Basic auth username. |
| `BASIC_AUTH_PASSWORD` | `admin` | Basic auth password. |

## TMS Integration Parameters

| Environment Variable | Default Value | Description |
|---|---|---|
| `TMS_ENABLED` | `false` | Enables Test Management System integration. |
| `TMS_HOST` | `tms.localhost` | TMS host name used by integration clients. |
| `TMS_API_BASE_URL` | `https://${tms.host}/api` | Base API URL for TMS calls. |
| `TMS_TOKEN` | `my-token` | API token for TMS authentication. |
| `TMS_ISSUE_KEY_PATTERN` | `[A-Za-z]+-\d+` | Regex used to detect issue keys in metadata. |
| `TMS_DRY_RUN` | `false` | If `true`, logs TMS actions without sending them. |

## Logging Parameters

| Environment Variable | Default Value | Description |
|---|---|---|
| `LOGGING_LEVEL_ROOT` | `INFO` | Global log level fallback. |
| `LOGGING_LEVEL_ORG_ATMOSPHERE` | `WARN` | Vaadin transport layer logging level. |
| `LOGGING_LEVEL_ORG_SPRINGFRAMEWORK` | `INFO` | Spring framework log level. |
| `LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CORE` | `WARN` | Spring core log level. |
| `LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_BEANS_FACTORY_SUPPORT` | `WARN` | Spring bean factory log level. |
| `LOGGING_LEVEL_RU_IOPUMP_QA_ALLURE` | `INFO` | Application package log level. |
| `LOGGING_LEVEL_RU_IOPUMP_QA_ALLURE_API` | `DEBUG` | API-specific log level for this project. |
