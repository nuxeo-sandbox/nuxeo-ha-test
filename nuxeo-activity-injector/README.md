# Nuxeo activity injector

That project is basically a gatling test. To execute :


	# mvn clean install
	# java [-Dsysprop=...]... -jar target/nuxeo-activity-injector-1.0-SNAPSHOT.jar

System Properites for HA/DR Test:
* simulation: Target simulation to run (default: ha)
  * Options: ha, cold, vreset, vcheck
  * ha: HA/DR test when both primary and secondary are set
  * cold: Uses 'csvInput' to verify document and binary entries
  * vreset: Reset vocabulary orderings for 'replication' vocabulary to 1
  * vcheck: Modifies ordering of 'replication' vocabulary and tracks the replication from primary to secondary
* primary: Primary URL (default: http://nuxeo.apps.io.nuxeo.com)
* secondary: Secondary URL * set to test DR capabilities, assumes hot-standby
* users: Number of users to simulate (default: 20)
* ramp: Second to ramp up users (default: 30)
* duration: Number of seconds to run the simulation (defualt: 300)
* entries: Number of Vocabulary entries to exercise (default: 3000)
* csvInput: Full path to CSV Input file (default: uid_export.csv)
