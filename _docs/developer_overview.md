---
title: "Toolkit Development overview"
permalink: /docs/developer/overview/
excerpt: "Contributing to this toolkits development."
last_modified_at: 2017-08-04T12:37:48-04:00
redirect_from:
   - /theme-setup/
sidebar:
   nav: "developerdocs"
---
{% include toc %}
{% include editme %}

# Developing operators of this toolkit:

This toolkit uses Apache Ant 1.8 (or later) to build.

The top-level build.xml contains the main targets:

* all - Builds and creates SPLDOC for the toolkit and samples. Developers should ensure this target is successful when creating a pull request.
* toolkit - Build the complete toolkit code
* build-all-samples - Builds all samples. Developers should ensure this target is successful when creating a pull request.
* release - Builds release artifacts, which is a tar bundle containing the toolkits and samples. It includes stamping the SPLDOC and toolkit version numbers with the git commit number (thus requires git to be available).

The release should use Java 8 for the Java compile to allow the widest use of the toolkit (with Streams 4.0.1 or later). (Note Streams 4.0.1 ships Java 8).
The build script inserts the commit hash into the toolkit version if the version number has a form like X.Y.Z.__dev__ 
This change in the info.xml file can be removed with ant target revertversion.
