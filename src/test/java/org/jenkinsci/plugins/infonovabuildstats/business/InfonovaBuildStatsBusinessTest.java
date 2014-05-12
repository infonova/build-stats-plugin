package org.jenkinsci.plugins.infonovabuildstats.business;

import org.jenkinsci.plugins.infonovabuildstats.InfonovaBuildStatsPlugin;
import org.jvnet.hudson.test.HudsonTestCase;


public class InfonovaBuildStatsBusinessTest extends HudsonTestCase {

    private InfonovaBuildStatsPlugin plugin;

    private InfonovaBuildStatsBusiness business;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        plugin = InfonovaBuildStatsPlugin.getInstance();
        business = InfonovaBuildStatsPlugin.getPluginBusiness();
    }

    /**
     * Make sure builds are recorded and written out correctly.
     */
    public void testCallback() throws Exception {

        /*
         * List<FreeStyleProject> projects = new ArrayList<FreeStyleProject>();
         * for (int i = 0; i < 5; i++) {
         * projects.add(createFreeStyleProject());
         * }
         * 
         * hudson.setNumExecutors(5);
         * 
         * for (int i = 0; i < 5; i++) {
         * List<Future<FreeStyleBuild>> builds = new ArrayList<Future<FreeStyleBuild>>();
         * for (FreeStyleProject p : projects) {
         * builds.add(p.scheduleBuild2(0));
         * }
         * // this simulates a lengthy plugin.save() and cause the grouping writes.
         * business.pluginSaver.writer.submit(new Runnable() {
         * 
         * public void run() {
         * try {
         * Thread.sleep(3000);
         * } catch (InterruptedException e) {
         * e.printStackTrace();
         * }
         * }
         * });
         * 
         * for (Future<FreeStyleBuild> f : builds) {
         * FreeStyleBuild b = assertBuildStatusSuccess(f);
         * }
         * }
         * 
         * // make sure we flush all the pending writes
         * business.pluginSaver.writer.submit(new Runnable() {
         * 
         * public void run() {
         * }
         * }).get();
         * 
         * assertEquals(25, plugin.getJobBuildResults().size());
         */
    }
}
