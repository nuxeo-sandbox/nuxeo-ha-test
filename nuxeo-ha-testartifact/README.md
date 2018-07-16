# High Availability Test Artifact Package

This project generates a deployable marketplace package.  It is used in conjunction with the `nuxeo-activity-injector` package to verify asynchronous work.  By default, it will asynchronously update the `dc:description` field of all `Note` documents to `updated` after a specified period of time.

## Installation

Build with `mvn package`.  Deploy the generated as marketplace package (`nuxeo-ha-testartifact-package/target/nuxeo-ha-testartifact-package-*.zip`)in target Nuxeo Platform instance.

## Configuration

The following behavior is configurable for the package:

* `nuxeo.ha.listener.updateType`: The type of document to update.  By default, this is a `Note` but may be `File` or whatever type your test requires.
* `nuxeo.ha.listener.duration`: Number of millisecond to sleep to simulate work.  Default is 1500 milliseconds.

Sleep time (listener duration) may be set on each individual document by specifying a millisecond value in the document's `dc:source` property.