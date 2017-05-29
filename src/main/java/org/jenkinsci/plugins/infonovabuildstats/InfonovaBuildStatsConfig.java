package org.jenkinsci.plugins.infonovabuildstats;

import hudson.Extension;
import hudson.model.Descriptor;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Holds the config for the infonova build stats plugin.
 * Configuration in "Configure system" to enable or disable build stats collection.
 * 
 * @author davcem
 * 
 */

@Extension
public class InfonovaBuildStatsConfig extends GlobalConfiguration {

    private boolean collectBuildStats;

    public InfonovaBuildStatsConfig() {
        load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }

    public boolean isCollectBuildStats() {
        return collectBuildStats;
    }

    public void setCollectBuildStats(boolean collectBuildStats) {
        this.collectBuildStats = collectBuildStats;
    }

}
