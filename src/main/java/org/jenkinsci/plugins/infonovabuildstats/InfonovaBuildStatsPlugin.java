package org.jenkinsci.plugins.infonovabuildstats;

import hudson.Extension;
import hudson.Plugin;
import hudson.model.ManagementLink;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.listeners.ItemListener;
import hudson.model.listeners.RunListener;
import hudson.security.AccessDeniedException2;
import hudson.security.Permission;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.infonovabuildstats.business.InfonovaBuildStatsBusiness;
import org.jenkinsci.plugins.infonovabuildstats.model.JobBuildResult;
import org.jenkinsci.plugins.infonovabuildstats.model.JobBuildResultSharder;
import org.kohsuke.stapler.bind.JavaScriptMethod;

/**
 * Plugin collects builds stats in two ways:
 * - ongoing (see in method {@link GlobalBuildStatsRunListener#onCompleted(AbstractBuild, TaskListener) onCompleted} if
 * config is enabled (see in class {@link org.jenkinsci.plugins.infonovabuildstats.InfonovaBuildStatsConfig} <br>
 * - or through explicit data initialization via GUI {@link #generateBuildInfos()}
 * 
 * */

public class InfonovaBuildStatsPlugin extends Plugin {

    private static final Logger LOGGER = Logger.getLogger(InfonovaBuildStatsPlugin.class.getName());

    transient private final InfonovaBuildStatsBusiness business = new InfonovaBuildStatsBusiness(this);

    private JobBuildResultSharder jobBuildResultsSharder = new JobBuildResultSharder();

    /**
     * Highered visibility of load method
     */
    public void load() throws IOException {
        super.load();
    }

    /**
     * @return File the config file of plugin
     * */

    public File getConfigXmlFile() {
        return getConfigXml().getFile();
    }

    /**
     * Used to work with the plugin singleton
     * 
     * @return InfonovaBuildStatsPlugin - the actual loaded plugin from jenkins instance
     */

    public static InfonovaBuildStatsPlugin getInstance() {
        return Jenkins.getInstance().getPlugin(InfonovaBuildStatsPlugin.class);
    }

    /**
     * 
     * @return InfonovaBuildStatsBusiness - the business layer of plugin
     */

    public static InfonovaBuildStatsBusiness getPluginBusiness() {
        return getInstance().business;
    }

    /**
     * @return An unmodifiable list of job build results
     */
    public List<JobBuildResult> getJobBuildResults() {
        return this.jobBuildResultsSharder.getJobBuildResults();
    }

    /**
     * 
     * @return JobBuildResultSharder
     */
    public JobBuildResultSharder getJobBuildResultsSharder() {
        return jobBuildResultsSharder;
    }

    /**
     * Inner class for loading the plugin, when jenkins instance is loaded
     * 
     * 
     */
    @Extension
    public static class InfonovaBuildStatsItemListener extends ItemListener {

        /**
         * Called from jenkins for (re-) loading plugin
         */
        @Override
        public void onLoaded() {
            super.onLoaded();

            LOGGER.log(Level.INFO, "InfonovaBuildStatsItemListener on Loaded - start");

            getPluginBusiness().reloadPlugin();

            LOGGER.log(Level.INFO, "InfonovaBuildStatsItemListener on Loaded - end");
        }

        // TODO: check if a node has been renamed and, if so, replace old name by new name in
        // every job results
    }


    /**
     * Used to collect build stats data if job is completed.
     * 
     */
    @Extension
    public static class InfonovaBuildStatsRunListener extends RunListener<AbstractBuild> {

        public InfonovaBuildStatsRunListener() {
            super(AbstractBuild.class);
        }


        /**
         * 
         * @param r - The completed build
         * @param listener - Could be used to write log-messages into job
         */
        @Override
        public void onCompleted(AbstractBuild r, TaskListener listener) {

            LOGGER.log(Level.FINER, "A job completed, should be logged? ");

            InfonovaBuildStatsConfig config = GlobalConfiguration.all().get(InfonovaBuildStatsConfig.class);

            // Only collect job data if config is enabled
            if (config.isCollectBuildStats()) {

                LOGGER.log(Level.FINER, "Collect job data is enabled, start logging job.");

                super.onCompleted(r, listener);

                getPluginBusiness().onJobCompleted(r);

                LOGGER.log(Level.FINER, "Finished job logging.");
            } else {

                LOGGER.log(Level.FINER, "Collect job data is disabled, no further action.");
            }
        }
    }

    /**
     * Responsible for the global configuration link of plugin in jenkins
     */
    @Extension
    public static class InfonovaBuildStatsManagementLink extends ManagementLink {

        public String getIconFileName() {
            return "/plugin/infonova-build-stats/icons/infonova-build-stats.png";
        }

        public String getDisplayName() {

            // TODO set real Description
            return Messages.Infonova_Build_Stats_Titel();
        }

        public String getUrlName() {
            return "plugin/infonova-build-stats/";
        }

        @Override
        public String getDescription() {
            return "Collects job build results.";
        }
    }

    /**
     * Method is called from Jelly file
     * "src/main/resources/org/jenkinsci/plugins/infonovabuildstats/InfonovaBuildStatsPlugin/index.jelly"
     * 
     * @return String - The state of the execution of recordBuildInfos
     * 
     * @throws IOException
     */
    @JavaScriptMethod
    public String generateBuildInfos() throws IOException {

        try {

            LOGGER.log(Level.FINER, "Check permissions before generating build infos.");

            Jenkins.getInstance().checkPermission(getRequiredPermission());

        } catch (AccessDeniedException2 e) {

            LOGGER.log(Level.WARNING, "Permission denied!");

            return "PERMISSION_DENIED";

        } catch (Exception e) {

            LOGGER.log(Level.SEVERE, e.getMessage());

            return "NOT_OKAY";
        }

        LOGGER.log(Level.FINER, "Correct permissions!");

        business.recordBuildInfos();

        return "OKAY";

    }

    /**
     * 
     * @return Permission - the permission needed for generateBuildInfos
     */
    public Permission getRequiredPermission() {
        return Jenkins.ADMINISTER;
    }

    public void reloadJobBuildResults(List<JobBuildResult> results) {
        this.jobBuildResultsSharder = new JobBuildResultSharder(this.jobBuildResultsSharder, results);
    }
}
