package org.jenkinsci.plugins.infonovabuildstats;

import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Node;

import java.util.Calendar;

import org.jenkinsci.plugins.infonovabuildstats.model.BuildResult;
import org.jenkinsci.plugins.infonovabuildstats.model.JobBuildResult;


/**
 * 
 * Class produces {@link org.jenkinsci.plugins.infonovabuildstats.business.JobBuildResult} from given
 * AbstractBuild.
 * 
 */
public class JobBuildResultFactory {

    public static final JobBuildResultFactory INSTANCE = new JobBuildResultFactory();

    /** @see hudson.security.ACL#SYSTEM */
    private static final String SYSTEM_USERNAME = "SYSTEM";

    /**
     * 
     * @param build - The build from which JobBuildResult is produced.
     * @return JobBuildResult - Produced JobBuildResult
     */
    public JobBuildResult createJobBuildResult(AbstractBuild build) {

        String buildName = build.getProject().getFullName();

        String nodeName = build.getBuiltOnStr();

        String buildClass = build.getClass().getSimpleName();

        long duration = build.getDuration();

        /* build start date */
        Calendar startDate = build.getTimestamp();

        /* build start date on executor */
        Calendar executionStartDate = Calendar.getInstance();
        executionStartDate.setTimeInMillis(build.getStartTimeInMillis());

        long queueDuration = executionStartDate.getTimeInMillis() - startDate.getTimeInMillis();

        /* build complete date */
        Calendar completeDate = Calendar.getInstance();
        completeDate.setTimeInMillis(build.getStartTimeInMillis() + duration);

        String nodeLabel = extractNodeLabels(build);

        /*
         * Can't do that since MavenModuleSet is in maven-plugin artefact which is in test scope
         * if(build.getProject() instanceof MavenModuleSet){
         * buildName = ((MavenModuleSet)build.getProject()).getRootModule().toString();
         * }
         */

        return new JobBuildResult(build.getId(), createBuildResult(build.getResult()), buildName, buildClass, build
            .getNumber(), startDate.getTimeInMillis(), completeDate.getTimeInMillis(), executionStartDate
            .getTimeInMillis(), duration, queueDuration, nodeLabel, nodeName, extractUserNameIn(build));
    }

    /**
     * 
     * @param result
     * @return BuildResult
     */
    public BuildResult createBuildResult(Result result) {
        if (Result.ABORTED.equals(result)) {
            return BuildResult.ABORTED;
        } else if (Result.FAILURE.equals(result)) {
            return BuildResult.FAILURE;
        } else if (Result.NOT_BUILT.equals(result)) {
            return BuildResult.NOT_BUILD;
        } else if (Result.SUCCESS.equals(result)) {
            return BuildResult.SUCCESS;
        } else /* if(Result.UNSTABLE.equals(result)) */{
            return BuildResult.UNSTABLE;
        }
    }

    public static String extractUserNameIn(AbstractBuild<?, ?> build) {
        String userName;
        @SuppressWarnings("deprecation")
        Cause.UserCause uc = build.getCause(Cause.UserCause.class);
        Cause.UserIdCause uic = build.getCause(Cause.UserIdCause.class);
        if (uc != null) {
            userName = uc.getUserName();
        } else if (uic != null) {
            userName = uic.getUserId();
        }
        // If no UserCause has been found, SYSTEM user should have launched the build
        else {
            userName = SYSTEM_USERNAME;
        }
        return userName;
    }

    /**
     * 
     * @param build
     * @return String - label of node without label "jobEnvProperties")
     */
    public static String extractNodeLabels(AbstractBuild build) {

        Node node = build.getBuiltOn();

        if (node == null) {
            return "";
        } else {

            // first delete jobEnvProperties Label
            String label = node.getLabelString().replaceAll("jobEnvProperties", "");

            // second remove all spaces
            return label.replaceAll("\\s", "");
        }
    }
}
