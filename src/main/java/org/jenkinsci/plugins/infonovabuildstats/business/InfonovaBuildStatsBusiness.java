package org.jenkinsci.plugins.infonovabuildstats.business;

import hudson.model.TopLevelItem;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.infonovabuildstats.JobBuildResultFactory;
import org.jenkinsci.plugins.infonovabuildstats.InfonovaBuildStatsPlugin;
import org.jenkinsci.plugins.infonovabuildstats.model.JobBuildResult;
import org.jenkinsci.plugins.infonovabuildstats.utils.CollectionsUtil;


public class InfonovaBuildStatsBusiness {

    private static final Logger LOGGER = Logger.getLogger(InfonovaBuildStatsBusiness.class.getName());

    private final InfonovaBuildStatsPlugin plugin;

    final InfonovaBuildStatsPluginSaver pluginSaver;


    public InfonovaBuildStatsBusiness(InfonovaBuildStatsPlugin infonovaBuildStatsPlugin) {
        this.plugin = infonovaBuildStatsPlugin;

        this.pluginSaver = new InfonovaBuildStatsPluginSaver(this.plugin);
    }

    public void reloadPlugin() {

        LOGGER.log(Level.INFO, "Call Pluginsaver reload Plugin");

        this.pluginSaver.reloadPlugin();

        LOGGER.log(Level.INFO, "Finished call Pluginsaver reload Plugin");


        // If job results are empty, let's perform an initialization !
        if (this.plugin.getJobBuildResults() == null || this.plugin.getJobBuildResults().size() == 0) {
            try {

                LOGGER.log(Level.INFO, "Job results empty or 0, so record build infos");

                this.recordBuildInfos();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    /**
     * Records the result of a build.
     */
    public void onJobCompleted(final AbstractBuild build) {

        this.pluginSaver.updatePlugin(new InfonovaBuildStatsPluginSaver.BeforeSavePluginCallback() {

            public void changePluginStateBeforeSavingIt(InfonovaBuildStatsPlugin plugin) {

                plugin.getJobBuildResultsSharder().queueResultToAdd(
                    JobBuildResultFactory.INSTANCE.createJobBuildResult(build));
            }
        });
    }

    public void recordBuildInfos() throws IOException {

        this.pluginSaver.updatePlugin(new InfonovaBuildStatsPluginSaver.BeforeSavePluginCallback() {

            public void changePluginStateBeforeSavingIt(InfonovaBuildStatsPlugin plugin) {

                List<JobBuildResult> jobBuildResultsRead = new ArrayList<JobBuildResult>();

                // TODO fix MatrixProject and use getAllJobs()
                for (TopLevelItem item : Hudson.getInstance().getItems()) {
                    LOGGER.log(Level.INFO, "Item " + item.getName() + " is of type: " + item.getClass());
                    if (item instanceof AbstractProject) {
                        addBuildsFrom(jobBuildResultsRead, (AbstractProject)item);
                    }
                }

                LOGGER.log(Level.INFO, "Total read items: " + jobBuildResultsRead.size());

                plugin.getJobBuildResultsSharder().queueResultsToAdd(
                    CollectionsUtil.<JobBuildResult> minus(jobBuildResultsRead, plugin.getJobBuildResults()));
            }
        });
    }

    private static void addBuildsFrom(List<JobBuildResult> jobBuildResultsRead, AbstractProject project) {
        List<AbstractBuild> builds = project.getBuilds();
        Iterator<AbstractBuild> buildIterator = builds.iterator();

        LOGGER.log(Level.INFO, "Project: " + project.getName() + ", number builds: " + builds.size());

        while (buildIterator.hasNext()) {
            addBuild(jobBuildResultsRead, buildIterator.next());
        }
    }

    private static void addBuild(List<JobBuildResult> jobBuildResultsRead, AbstractBuild build) {
        jobBuildResultsRead.add(JobBuildResultFactory.INSTANCE.createJobBuildResult(build));
    }
}
