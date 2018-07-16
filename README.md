# Nuxeo High Availability Tests

This repository has several tools to test that a running Nuxeo cluster can survive various malfunctions.

All the test infrastructure is expected to run on Kubernetes or OpenShift.

## Test Modules

### Nuxeo HA Test Artifact

The `nuxeo-ha-testartifact` project produces a marketplace module to be used with the corresponding Activity Injector tests.  It should be installed as part of the Nuxeo image deployed on the infrastructure.

### Activity Injector

The `nuxeo-activity-injector` is a collection of [Gatling](https://gatling.io/docs/2.3/general/scenario/) scripts that generates load on the cluster.  There are a collection of scenarios that provide user and database activity.

### [Chaos Monkey](https://en.wikipedia.org/wiki/Chaos_Monkey)

The go script will use the infrastructure APIs to cause failures in the underlying services.

## Execution Plan

Deploy the Nuxeo platform and associated backing services on a Kubernetes or OpenShift cluster.  Include nuxeo-ha-testartifact marketplace package within Nuxeo platform.  Execute Gatling scenarios to develop baseline.  Introduce chaos monkey to cause failures with various components in your deployment.

## Expected results

Validate Nuxeo resilience.  The Gatling tests should not experience any fatal failures.

## Support

**These features are not part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.

## Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

## About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.

More information is available at [www.nuxeo.com](http://www.nuxeo.com).