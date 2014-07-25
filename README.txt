Infonova build stats
====================
General
-------
The Infonova build stats plugin is based on the [global build stats plugin](https://wiki.jenkins-ci.org/display/JENKINS/Global+Build+Stats+Plugin).
The whole structure was adopted from this plugin: only a few performance improvements were added, and some blocks for the realization of graphs and some config were deleted.

[Infonova build stats plugin documentation](https://product.infonova.at/confluence/display/OPSS/Infonova+build+stats+plugin)

Function
--------
The Infonova build stats plugin collects data in two ways:
* Continuous collection of build stats, if the feature is enable via jenkins global config (Collect job data = true)
* Immediate collection of build stats by using the "Initialize stats" option on the plugin page

The build stats data are saved within the folder "infonova-build-stats" in the JENKINS_HOME folder.