package org.jenkinsci.plugins.infonovabuildstats;

import hudson.Extension;
import hudson.Plugin;
import hudson.model.ManagementLink;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
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

public class InfonovaBuildStatsPlugin extends Plugin {

    private static final Logger LOGGER = Logger.getLogger(InfonovaBuildStatsPlugin.class.getName());

    /**
     * Business layer for infonova build stats
     */
    transient private final InfonovaBuildStatsBusiness business = new InfonovaBuildStatsBusiness(this);

    /**
     * TODO Update Comment (due to changes)
     */
    private JobBuildResultSharder jobBuildResultsSharder = new JobBuildResultSharder();

    /**
     * Highered visibility of load method
     */
    public void load() throws IOException {
        super.load();
    }

    public File getConfigXmlFile() {
        return getConfigXml().getFile();
    }

    public static InfonovaBuildStatsPlugin getInstance() {
        return Jenkins.getInstance().getPlugin(InfonovaBuildStatsPlugin.class);
    }

    public static InfonovaBuildStatsBusiness getPluginBusiness() {
        // Retrieving global build stats plugin & adding build result to the registered build
        // result
        return getInstance().business;
    }

    /**
     * @return An unmodifiable list of job build results
     */
    public List<JobBuildResult> getJobBuildResults() {
        return this.jobBuildResultsSharder.getJobBuildResults();
    }

    @Extension
    public static class InfonovaBuildStatsItemListener extends ItemListener {

        /**
         * After all items are loaded, plugin is loaded
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

    @Extension
    public static class GlobalBuildStatsRunListener extends RunListener<AbstractBuild> {

        public GlobalBuildStatsRunListener() {
            super(AbstractBuild.class);
        }

        @Override
        public void onCompleted(AbstractBuild r, TaskListener listener) {

            LOGGER.info("A job completed, should be logged? ");

            InfonovaBuildStatsConfig config = GlobalConfiguration.all().get(InfonovaBuildStatsConfig.class);

            // Only collect job data if config is enabled
            if (config.isCollectBuildStats()) {

                LOGGER.info("Collect job data is enabled, start logging job.");

                super.onCompleted(r, listener);

                getPluginBusiness().onJobCompleted(r);

                LOGGER.info("Finished job logging.");
            } else {

                LOGGER.info("Collect job data is disabled, no further action.");
            }
        }
    }

    /**
     * Let's add a link in the administration panel linking to the infonova build stats page
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

    public JobBuildResultSharder getJobBuildResultsSharder() {
        return jobBuildResultsSharder;
    }

    @JavaScriptMethod
    public String generateBuildInfos() throws IOException {

        try {

            LOGGER.log(Level.FINER, "Logger test for record build infos (before check permissions).");

            Hudson.getInstance().checkPermission(getRequiredPermission());

            LOGGER.log(Level.FINER, "Logger test for record build infos (after check permissions).");

        } catch (AccessDeniedException2 e) {

            LOGGER.log(Level.SEVERE, "Permission denied!");

        } catch (Exception e) {

            LOGGER.log(Level.SEVERE, e.getMessage());

            return "NOT_OKAY";
        }

        LOGGER.log(Level.FINER, "Correct permissions");

        business.recordBuildInfos();

        return "OKAY";

    }

    public Permission getRequiredPermission() {
        return Jenkins.ADMINISTER;
    }

    public void reloadJobBuildResults(List<JobBuildResult> results) {
        this.jobBuildResultsSharder = new JobBuildResultSharder(this.jobBuildResultsSharder, results);
    }
}
