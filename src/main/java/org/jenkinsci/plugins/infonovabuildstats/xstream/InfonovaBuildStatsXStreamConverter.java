package org.jenkinsci.plugins.infonovabuildstats.xstream;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.infonovabuildstats.InfonovaBuildStatsPlugin;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * XStream Converter for the InfonovaBuildStatsPlugin.
 * This converter is registered within
 * {@link org.jenkinsci.plugins.infonovabuildstats.business.InfonovaBuildStatsPluginSaver#InfonovaBuildStatsPluginSaver(InfonovaBuildStatsPlugin)}
 *
 */
public class InfonovaBuildStatsXStreamConverter implements Converter {

    private static final Logger LOGGER = Logger.getLogger(InfonovaBuildStatsXStreamConverter.class.getName());

    public static final String JOB_BUILD_RESULT_CLASS_ALIAS = "jbr";

    @Override
    public boolean canConvert(Class type) {
        return InfonovaBuildStatsPlugin.class.isAssignableFrom(type);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {

        LOGGER.log(Level.FINE, "Start marshalling plugin.");

        InfonovaBuildStatsPlugin plugin = (InfonovaBuildStatsPlugin)source;

        //only persist job build results to files, if we have pending changes
        if(plugin.getJobBuildResultsSharder().pendingChanges()){

        	plugin.getJobBuildResultsSharder().applyQueuedResultsInFiles();

        }

        LOGGER.log(Level.FINE, "Finished marshalling plugin.");

    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

        LOGGER.log(Level.FINER, "Start unmarshalling plugin.");

        InfonovaBuildStatsPlugin plugin;

        if (context.currentObject() == null || !(context.currentObject() instanceof InfonovaBuildStatsPlugin)) {
            // This should never happen to get here
            LOGGER.log(Level.WARNING, "Plugin is created with NEW!!!");

            plugin = new InfonovaBuildStatsPlugin();
        } else {
            // Retrieving already instantiated GlobalBuildStats plugin into current context ..
            plugin = (InfonovaBuildStatsPlugin)context.currentObject();
        }

        return plugin;
    }
}
