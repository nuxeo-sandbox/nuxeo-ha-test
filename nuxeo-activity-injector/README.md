# Nuxeo Activity Injector

This project is a collection of Gatling scripts that produce load on the target Nuxeo cluster.  See the `utils` sub-directory for a set of companion scripts.

## Configuration

When using `gatling.sh`, set the `JAVA_OPTS` environment variable with the configuration you'd like to run:

`export JAVA_OPTS="-Dsimulation=cold -Dcsv=${PATH_TO}/uid_export.csv"`

System Properites for HA/DR Test:
* `simulation`: Target simulation to run (default: `ha`)
  * Options: ace, ha, cold, vreset, vcheck
  * `ace`: Access Control Entry test update and remove
  * `ha`: HA/DR test when both primary and secondary are set
  * `cold`: Uses 'csvInput' to verify document and binary entries
  * `vreset`: Reset vocabulary orderings for 'replication' vocabulary to 1
  * `vcheck`: Modifies ordering of 'replication' vocabulary and tracks the replication from primary to secondary
* `primary`: Primary URL (default: http://nuxeo.apps.io.nuxeo.com)
* `secondary`: Secondary URL * set to test DR capabilities, assumes hot-standby
* `users`: Number of users to simulate (default: 20)
* `ramp`: Second to ramp up users (default: 30)
* `duration`: Number of seconds to run the simulation (defualt: 300)
* `entries`: Number of Vocabulary entries to exercise (default: 3000)
* `csv`: Full path to CSV Input file (default: uid_export.csv)
* `checkUpdate`: (true [default] or false) enable or disable `dc:description` update check for the `ha` scenario.  May be disabled to prevent artificial failures during other test scenarios.
* `index`: (true [default] or false) enable or disable ElasticSearch index examination during tests
* `delete`: (true [default] or false) enable or disable document delete as part of the `ha` scenario.  May be used to populate a repository for the `cold` replication scenario check.
* `vocabulary`: Specify the vocabulary to use for the `vcheck` / `vreset` tests.

## Execution

### With Gatling (recommended)

Download Gatling 2.3.0 bundle from https://gatling.io/ and extract.  Copy the `org` package from `src/main/scala` into the target Gatling `user-files/simulations` sub-directory.

Run the simulation from the Gatling `bin` directory:

`./gatling.sh -s org.nuxeo.ecm.ha.injector.HaInjector -rd "Description Here" -on "results-dir-name-is-optional"`

Results will be collected in the Gatling installation's `results` sub-directory.

### With Maven

_Maven execution might cause some errors due to scala classpath incompatibilities.  If you encounter this error, follow the `With Gatling` instructions._

```bash
$ mvn clean install
$ java [-Dsysprop=...]... -jar target/nuxeo-activity-injector-1.0-SNAPSHOT.jar
```

### Chaos Monkey

Specify the label selector for the target service to kill during the load test.  Spin up the Chaos Monkey pod within the infrastructure.