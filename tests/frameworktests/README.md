# README --  FrameworkTests

This directory provides an automatic test for a number of operators of the inet server toolkit.

## Test Execution

Change to `frameworktests` directory  

To start the full test execute:  
`make test-full`

To start a quick test, execute:  
`make test`

This script installs the test framework in directory `scripts` and starts the test execution. The script delivers the following result codes:  
0     : all tests Success  
20    : at least one test fails  
25    : at least one test error  
26    : Error during suite execution  
130   : SIGINT received  
other : another fatal error has occurred  

More options are available and explained with command:  
`make help`
and
`make scripthelp`


## Test Sequence

The test targets install the test framework into directory `scripts` and starts the test framework. The test framework 
checks if there is a running Streams instance.

If the Streams instance is not running, a domain and an instance is created from the scratch and started. You can force the 
creation of instance and domain with command line option `--clean`

The inet server toolkit is expected in directory `../../com.ibm.streamsx.inetserver/` and must be built with the current Streams version. 
The inet server toolkit samples are expected in `../../samples`. 

Use command line option `-D <name>=<value>` to set external variables or provide a new properties file with command line option 
`--properties <filename>`. The standard properties file is `tests/TestProperties.sh`.

## Requirements

The test framework requires an valid Streams installation and environment.
