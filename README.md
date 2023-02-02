## How to run

From command line run

- `gradlew build` to compile and test
- `gradlew runServer -q --console=plain` to run server in console
- from another terminal `gradlew runClient -q --console=plain`
    - this will open a socket connection to the server
    - type anything and hit enter. Each line will be sent to the server
- to test throughput, run `gradlew test --tests org.newrelic.MainTest`
    - this test generates large number of data (2 million of records) and sends it to the server through 5 sockets
    - the test has timeout configured timeout, to show execution in time

**Note**, checkout [tests](src/test/java/org/newrelic) folder for covered use cases