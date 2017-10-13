# Nuxeo HA Tests

This repository holds several tools to test that a running Nuxeo cluster can survive various malfunctions.

All the test infracstructure has to be run on Kubernetes.


## Test parts
### Nuxeo test project

That project allows to setup various 


### Activity injector

This parts generates some test activity on the cluster using Gatling. 


### Chaos monkey setup

This part setup some monkeys that will kill pods in a randomly manner

## Expected results

To validate Nuxeo resilience, the Gatling tests should not experience any failure. 


    


# Licensing

Most of the source code in the Nuxeo Platform is copyright Nuxeo and
contributors, and licensed under the Apache License, Version 2.0.

See [/licenses](/licenses) and the documentation page [Licenses](http://doc.nuxeo.com/x/gIK7) for details.

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at [www.nuxeo.com](http://www.nuxeo.com).



