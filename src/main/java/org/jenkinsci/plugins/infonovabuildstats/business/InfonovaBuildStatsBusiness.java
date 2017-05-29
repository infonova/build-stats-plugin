package org.jenkinsci.plugins.infonovabuildstats.business;

import org.jenkinsci.plugins.infonovabuildstats.InfonovaBuildStatsPlugin;
import org.jenkinsci.plugins.infonovabuildstats.model.AgentStatistic;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Business layer for our plugin (reload plugin, collect build stats)
 *
 *
 */
public class InfonovaBuildStatsBusiness {

    private static final Logger LOGGER = Logger.getLogger(InfonovaBuildStatsBusiness.class.getName());

    private final InfonovaBuildStatsPlugin plugin;

    private final InfonovaBuildStatsPluginSaver pluginSaver;


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
     * @param agentStatistic The statistic of the offline computer
     */
    public void onComputerOffline(final AgentStatistic agentStatistic) {

        this.pluginSaver.updatePlugin(new InfonovaBuildStatsPluginSaver.BeforeSavePluginCallback() {

            public void changePluginStateBeforeSavingIt(InfonovaBuildStatsPlugin plugin) {
                plugin.getJobBuildResultsSharder().queueResultToAdd(agentStatistic);
            }
        });
    }
}
