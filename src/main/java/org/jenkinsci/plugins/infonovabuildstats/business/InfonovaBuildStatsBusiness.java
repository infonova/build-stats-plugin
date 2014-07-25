package org.jenkinsci.plugins.infonovabuildstats.business;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.infonovabuildstats.JobBuildResultFactory;
import org.jenkinsci.plugins.infonovabuildstats.InfonovaBuildStatsPlugin;
import org.jenkinsci.plugins.infonovabuildstats.model.JobBuildResult;
import org.jenkinsci.plugins.infonovabuildstats.utils.CollectionsUtil;


/**
 * Business layer for our plugin (reload plugin, collect build stats)
 *
 *
 */
public class InfonovaBuildStatsBusiness {

    private static final Logger LOGGER = Logger.getLogger(InfonovaBuildStatsBusiness.class.getName());

    private final InfonovaBuildStatsPlugin plugin;

    final InfonovaBuildStatsPluginSaver pluginSaver;


    public InfonovaBuildStatsBusiness(InfonovaBuildStatsPlugin infonovaBuildStatsPlugin) {
        this.plugin = infonovaBuildStatsPlugin;

        this.pluginSaver = new InfonovaBuildStatsPluginSaver(this.plugin);
    }

    public void reloadPlugin() {

        LOGGER.log(Level.FINER, "Call Pluginsaver reload Plugin");

        this.pluginSaver.reloadPlugin();

        LOGGER.log(Level.FINER, "Finished call Pluginsaver reload Plugin");
    }

    /**
     * Records the result of actual completed build.
     */
    public void onJobCompleted(final AbstractBuild build) {

        this.pluginSaver.updatePlugin(new InfonovaBuildStatsPluginSaver.BeforeSavePluginCallback() {

            public void changePluginStateBeforeSavingIt(InfonovaBuildStatsPlugin plugin) {

                plugin.getJobBuildResultsSharder().queueResultToAdd(
                    JobBuildResultFactory.INSTANCE.createJobBuildResult(build));
            }
        });
    }

    /**
     * Records results of past builds (all builds available in jenkins)
     *
     * @throws IOException
     */
    public void recordBuildInfos() throws IOException {

        this.pluginSaver.updatePlugin(new InfonovaBuildStatsPluginSaver.BeforeSavePluginCallback() {

            public void changePluginStateBeforeSavingIt(InfonovaBuildStatsPlugin plugin) {

                List<JobBuildResult> jobBuildResultsRead = new ArrayList<JobBuildResult>();

                List<AbstractProject> projects = new ArrayList<AbstractProject>();

                projects = Jenkins.getInstance().getAllItems(AbstractProject.class);

                LOGGER.log(Level.INFO, "Found projects: " + projects.size());

                for (AbstractProject project : projects) {

                    addBuildsFrom(jobBuildResultsRead, project);
                }

                LOGGER.log(Level.INFO, "Read builds: " + jobBuildResultsRead.size());

                plugin.getJobBuildResultsSharder().queueResultsToAdd(
                    CollectionsUtil.<JobBuildResult> minus(jobBuildResultsRead, plugin.getJobBuildResults()));
            }
        });
    }

    /**
     *
     * @param jobBuildResultsRead - List in which builds should be added
     * @param project - From which builds should be added
     */
    private static void addBuildsFrom(List<JobBuildResult> jobBuildResultsRead, AbstractProject project) {
        List<AbstractBuild> builds = project.getBuilds();

        Iterator<AbstractBuild> buildIterator = builds.iterator();

        while (buildIterator.hasNext()) {
            addBuild(jobBuildResultsRead, buildIterator.next());
        }
    }

    private static void addBuild(List<JobBuildResult> jobBuildResultsRead, AbstractBuild build) {
        jobBuildResultsRead.add(JobBuildResultFactory.INSTANCE.createJobBuildResult(build));
    }
}
