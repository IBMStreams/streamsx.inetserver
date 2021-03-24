# Changes

## v4.3.4
* Bump jettyVersion 9.4.38.v20210224
* Fix: HTTPTupleInject operator fails when attribute type is unsigned integer. (Issue #46)

## v4.3.3
* Bump jettyVersion from 9.4.18.v20190429 to 9.4.35.v20201120

## v4.3.2
* Globalization support

## v4.3.1
* Fix: Use relative path in all sample index.html files to be able to be used behind proxy
* Fix: Correct timing issue in junit tests

## v4.3.0
* Enhancement: New parameter 'sslAppConfigName' to get the SSL server key/certificate and the client trust material from Streams application configuration with this name

## v4.2.0
* Fixed: Operators can not load trustStore file if path is not an absolute one
* Enhancemement: Parameter contextResourceBase is now independent from parameter context
* Enhancemement in description for generation of client certitficate which can be used in a brower

## v4.1.0
* Enhancement in operator HTTPTupleView: Flexible partition query
* Enhancement in operator HTTPTupleView: new parameter 'partitionBy'
* Enhancement in Servlet Operator: new Parameter 'host'
* Enhancement in operator HTTPTupleInjection: insert the default values
* Correction in operator HTTPTupleView: selection of partition fails if attribute has type boolean

## v4.0.0
* Remove target 'setcommitversion' from build.xml - we have now a build.info
* Update jetty server libs to 9.4.18.v20190429
* Sample HTTPTupleInjectAndView is now enabled for secure connections
* Operator HTTPRequestProcess: Corrected and competed parameters and attribute
* Operator HTTPRequestProcess: code cleanup, introduce exception handling in process method and return error responses in such cases
* Operator HTTPRequestProcess: remove parameter statusMessage
* Operator HTTPRequestProcess: Use Async Context instead of deprecated jetty continuation
* Operator HTTPRequestProcess: renaming input attribute names to response...
* Operator HTTPRequestProcess: Ignore WindowMarker and set default for non supplied json response artifacts
* Operator HTTPRequestProcess: Add contextPath attribute to tuple output
* Websocket Operators: Switch implementation from Java-WebSocket to jetty websocket libs
* Fix metrics in HTTPRequestProcess
* Improved documentation
* Samples: Makefiles are now enabled for studio build
* Move junit tests to tests directory
* Integrate junit tests and framework tests into main build.xml test targets
* New Junit Testcases for websocket operators, XMLView, PostBLOB and PostXML
* New Framework Test Cases with HTTPRequestProcess operator attribute checks and runtime tests
* New sample: HTTPXMLView
* Simplified sample RequestProcessTuple
* Removed puzzeling RequestProcessTuple Json Sample

## v3.0.0:
* Initial version forked from [streams.inet v2.9.6](https://github.com/IBMStreams/streamsx.inet/tree/v2.9.6)

## v2.9.5:
* Enhancement #324: HTTPTupleView Windowing does not support multiple partition keys in a comma-delimited string

