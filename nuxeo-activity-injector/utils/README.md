# Utility Scripts

These node.js scripts are a companion to the gatling scripts found in the parent directory.  They provide the necessary functionality to test cold replication as well as verify Gatling test results.

## Installation

Some additional node packages are required.  Use `npm install` to set up your local directory.

## Configuration

See `config.js` for all possible configuration options.  Defaults should be configured, per environment, in the `config/` subdirectory.  At a minimum, the primary URL should be defined.

## Scripts

* `export_uid.js`: Use in conjunction with the `ace` and `cold` Gatling scenarios to capture the current state of the system.  Copy the generated `uid_export.csv` to the `bin` directory of your Gatling installation.

### Diagnosis / Utility

* `check_updated.js`: Use to verify that the `dc:description` field has been updated as part of the `ha` scenario.  The `ha` report may produce misleading errors if the update period is longer than the scenario timeout.  This script is used post-execution to verify that all description fields are updated.
* `check_permissions.js`: Use to verify permissions on the workspace folder. Used to help diagnose any unexpected permissions failures.
* `show_ace.js`: Produce a report of permissions for documents updated by `ace` scenario.  Used to diagnose any problems.
