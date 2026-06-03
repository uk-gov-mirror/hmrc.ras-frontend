# ras-frontend

This service is designed for Pension Scheme Administrators and Pension Scheme Practitioners to get residency status
of a pension scheme member.


Requirements
------------

This service is written in [Scala] and [Play], requires Java 11 to run.


Authentication
------------

This user logs into this service using the [Government Gateway]

Testing
------------

#### Unit Tests
To run the unit tests for the application run the following:

1. `cd $workspace`
2. `sbt test`

To run a single unit test/spec

1. `cd $workspace`
2. `sbt`
3. `test-only *SpecToUse*` - Example being the class name of your WordSpecLike

#### Test Coverage
To run the test coverage suite `scoverage`

1. `sbt clean scoverage:test`

#### All tests

Prior to raising a PR use the following script to run Unit tests with coverage and generate a report:

    ./run_all_tests.sh

#### Acceptance Tests

**NOTE:** Acceptance tests are available in a separate project at:
[https://github.com/hmrc/ras-acceptance-tests](https://github.com/hmrc/ras-acceptance-tests)

#### Performance Tests

**NOTE:** Performance tests are available in a separate project at:
[https://github.com/hmrc/ras-performance-tests](https://github.com/hmrc/ras-performance-tests)

Running Locally
------------

Install [Service Manager](https://github.com/hmrc/sm2), then start dependencies:

    sm2 --start RAS_ALL

Start the app:

    sbt "run 9673"
    
Endpoint URL:

    /relief-at-source

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")

[Scala]: httpS://www.scala-lang.org/
[Play]: httpS://playframework.com/
