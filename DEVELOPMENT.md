# Running applications that use the Internet Server Toolkit

To create applications that use the Internet Server Toolkit, you must configure either Streams Studio
or the SPL compiler to be aware of the location of the toolkit.

## Before you begin

Install IBM InfoSphere Streams. Configure the product environment variables by entering the following command: 
    source product-installation-root-directory/product-version/bin/streamsprofile.sh

## About this task

After the location of the toolkit is communicated to the compiler, the SPL artifacts that are specified
in the toolkit can be used by an application. The application can include a use directive to bring the necessary namespaces into scope.
Alternatively, you can fully qualify the operators that are provided by toolkit with their namespaces as prefixes.

## Procedure

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


# Developing operators of this toolkit:

## Command Line Build

This toolkit uses Apache Ant 1.8 (or later) to build.

The top-level build.xml contains the main targets:

* **all** - Builds and creates SPLDOC for the toolkit and samples. Developers should ensure this target is successful when creating a pull request.
* **toolkit** - Build the complete toolkit code
* **samples-build** - Builds all samples. Developers should ensure this target is successful when creating a pull request.
* **release** - Builds release artifacts, which is a tar bundle containing the toolkits and samples. It includes stamping the SPLDOC and toolkit version numbers with the git commit number (thus requires git to be available).
* **test** - Start the test
* **spldoc** - Generate the toolkit documentation

Execute the comman `ant -p` to display the target information.

The release should use Java 8 for the Java compile to allow the widest use of the toolkit (with Streams 4.0.1 or later). (Note Streams 4.0.1 ships Java 8).

## Studio Java Build

To work with Streams Studio import the Eclipse project in directory `com.ibm.streamsx.inetserver` and execute the command 
`ant maven-deps` to import the required libraries.
