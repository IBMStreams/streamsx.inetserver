---
title: "Toolkit Usage Overview"
permalink: /docs/user/overview/
excerpt: "How to use this toolkit."
last_modified_at: 2018-12-04T09:37:48-04:00
redirect_from:
   - /theme-setup/
sidebar:
   nav: "userdocs"
---
{% include toc %}
{%include editme %}

# Toolkit Usage Overview

## SPLDOC

* [SPLDoc of the streamsx.inetserver toolkit](https://ibmstreams.github.io/streamsx.inetserver/doc/spldoc/html/tk$com.ibm.streamsx.inetserver/tk$com.ibm.streamsx.inetserver.html)

Reference of former releases:
* [v3.0](https://ibmstreams.github.io/streamsx.inetserver/v3.0/doc/spldoc/html/index.html)
* [v4.0](https://ibmstreams.github.io/streamsx.inetserver/v4.0/doc/spldoc/html/index.html)
* [v4.1](https://ibmstreams.github.io/streamsx.inetserver/v4.1/doc/spldoc/html/index.html)
* [v4.2](https://ibmstreams.github.io/streamsx.inetserver/v4.2/doc/spldoc/html/index.html)

## Samples

* [SPLDoc for the samples of streamsx.inetserver toolkit](https://ibmstreams.github.io/streamsx.inetserver/samples/doc/spldoc/html/index.html)


## What is new

[CHANGELOG.md](https://github.com/IBMStreams/streamsx.inetserver/blob/master/com.ibm.streamsx.inetserver/CHANGELOG.md)


## Running applications that use the Internet Server Toolkit

To create applications that use the Internet Server Toolkit, you must configure either Streams Studio
or the SPL compiler to be aware of the location of the toolkit.

### Before you begin

Install IBM InfoSphere Streams. Configure the product environment variables by entering the following command: 
    source product-installation-root-directory/product-version/bin/streamsprofile.sh

### About this task

After the location of the toolkit is communicated to the compiler, the SPL artifacts that are specified
in the toolkit can be used by an application. The application can include a use directive to bring the necessary namespaces into scope.
Alternatively, you can fully qualify the operators that are provided by toolkit with their namespaces as prefixes.

### Procedure

1. Configure the SPL compiler to find the toolkit root directory. Use one of the following methods:
  * Set the **STREAMS_SPLPATH** environment variable to the root directory of a toolkit
    or multiple toolkits (with : as a separator).  For example:
      export STREAMS_SPLPATH=<your_toolkit_root_directory>/com.ibm.streamsx.inetserver
  * Specify the **-t** or **--spl-path** command parameter when you run the **sc** command. For example:
      sc -t <your_toolkit_root_directory>/com.ibm.streamsx.inetserver -M MyMain
    where MyMain is the name of the SPL main composite.
    **Note**: These command parameters override the **STREAMS_SPLPATH** environment variable.
  * Add the toolkit location in InfoSphere Streams Studio.
2. Develop your application. To avoid the need to fully qualify the operators, add a use directive in your application. 
  * For example, you can add the following clause in your SPL source file:
      use com.ibm.streamsx.inet.rest::*;
    You can also specify a use clause for individual operators by replacing the asterisk (\*) with the operator name. For example: 
      use com.ibm.streamsx.inet.rest::HTTPRequestProcess;
3. Build your application.  You can use the **sc** command or Streams Studio.  
4. Start the InfoSphere Streams instance. 
5. Run the application. You can submit the application as a job by using the **streamtool submitjob** command or by using Streams Studio. 

