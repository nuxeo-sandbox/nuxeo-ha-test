# Nuxeo activity injector

That project is basically a gatling test. To execute :


	# mvn clean install
	# java [-Dsysprop=...]... -jar target/nuxeo-activity-injector-1.0-SNAPSHOT.jar

System Properites for HA/DR Test:
- primary: Primary URL (default: http://nuxeo.apps.io.nuxeo.com)
- secondary: Secondary URL - set to test DR capabilities, assumes hot-standby
- users: Number of users to simulate (default: 20)
- ramp: Second to ramp up users (default: 30)
- duration: Number of seconds to run the simulation (defualt: 300)
