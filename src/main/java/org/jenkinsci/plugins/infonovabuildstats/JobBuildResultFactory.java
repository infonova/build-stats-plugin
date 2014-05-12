package org.jenkinsci.plugins.infonovabuildstats;

import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Node;

import java.util.Calendar;

import org.jenkinsci.plugins.infonovabuildstats.model.BuildResult;
import org.jenkinsci.plugins.infonovabuildstats.model.JobBuildResult;


public class JobBuildResultFactory {

    public static final JobBuildResultFactory INSTANCE = new JobBuildResultFactory();

    /** @see hudson.security.ACL#SYSTEM */
    private static final String SYSTEM_USERNAME = "SYSTEM";

    public JobBuildResult createJobBuildResult(AbstractBuild build) {

        String buildName = build.getProject().getFullName();

        String nodeName = build.getBuiltOnStr();

        long overallDuration = build.getDuration();

        /* build start date */
        Calendar startDate = build.getTimestamp();

        /* build complete date */
        Calendar completeDate = Calendar.getInstance();
        completeDate.setTimeInMillis(startDate.getTimeInMillis() + overallDuration);

        /* build start date on executor */
        Calendar executionStartDate = Calendar.getInstance();
        executionStartDate.setTimeInMillis(build.getStartTimeInMillis());

        /* duration of Job on the executor */
        long executionDuration = completeDate.getTimeInMillis() - executionStartDate.getTimeInMillis();

        String nodeLabel = extractNodeLabels(build);

        /*
         * Can't do that since MavenModuleSet is in maven-plugin artefact which is in test scope
         * if(build.getProject() instanceof MavenModuleSet){
         * buildName = ((MavenModuleSet)build.getProject()).getRootModule().toString();
         * }
         */

        return new JobBuildResult(build.getId(), createBuildResult(build.getResult()), buildName, build.getNumber(),
            startDate, completeDate, executionStartDate, overallDuration, executionDuration, nodeLabel, nodeName,
            extractUserNameIn(build));
    }

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

    public static String extractNodeLabels(AbstractBuild build) {

        Node node = build.getBuiltOn();

        if (node == null) {
            return "";
        } else {
            return node.getLabelString().replaceAll(" jobEnvProperties", "");
        }
    }
}
