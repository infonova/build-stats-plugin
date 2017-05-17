package org.jenkinsci.plugins.infonovabuildstats;

import hudson.model.*;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.infonovabuildstats.model.BuildResult;
import org.jenkinsci.plugins.infonovabuildstats.model.JobBuildResult;
import org.jenkinsci.plugins.mesos.MesosSlave;
import org.jenkinsci.plugins.mesos.config.slavedefinitions.MesosSlaveInfo;

import java.util.Calendar;


/**
 *
 * Class produces {@link org.jenkinsci.plugins.infonovabuildstats.model.JobBuildResult} from given
 * AbstractBuild.
 *
 */
public class JobBuildResultFactory {

    public static final JobBuildResultFactory INSTANCE = new JobBuildResultFactory();
    public static final Jenkins JENKINS = Jenkins.getInstance();

    /** @see hudson.security.ACL#SYSTEM */
    private static final String SYSTEM_USERNAME = "SYSTEM";

    /**
     *
     * @param run - The build from which JobBuildResult is produced.
     * @return JobBuildResult - Produced JobBuildResult
     */
    public JobBuildResult createJobBuildResult(Run run) {

        String buildName = "";
        String nodeName = "";

        if(run instanceof AbstractBuild) {
            AbstractBuild abstractBuild = (AbstractBuild) run;
            buildName = abstractBuild.getProject().getFullName();
            nodeName = abstractBuild.getBuiltOnStr();
        }

        String buildClass = run.getClass().getSimpleName();

        long duration = run.getDuration();

        /* build start date */
        Calendar startDate = run.getTimestamp();

        /* build start date on executor */
        Calendar executionStartDate = Calendar.getInstance();
        executionStartDate.setTimeInMillis(run.getStartTimeInMillis());

        /*
         * queue duration of the job (difference of date when job was started and execution on
         * executor was started
         * Note: sadly, the value calculated here, does not provide the real queue time
         */
        // long queueDurationOld = executionStartDate.getTimeInMillis() - startDate.getTimeInMillis();
        long queueDuration = -1;
        int memory = 0;
        float cpus = 0.0f;
        String principal = "";

        Executor executor = run.getExecutor();

        if(executor != null) {
            /*
             * this returns the real queue time, provided the executor is still alive.
             * since the method is called upon "onCompleted" this is the case. however, when the method
             * would be called at "onFinalized" the executor does not exist anymore.
             */
            queueDuration = executor.getTimeSpentInQueue();

            Computer computer = executor.getOwner();
            if(computer != null) {
                Node node = computer.getNode();
                if (node instanceof MesosSlave) {
                    MesosSlave mesosSlave = (MesosSlave) node;
                    MesosSlaveInfo slaveInfo = mesosSlave.getSlaveInfo();

                    if(slaveInfo != null) {
                        int executorCount = mesosSlave.getNumExecutors();

                        cpus = (float) ((slaveInfo.getExecutorCpus() * executorCount) + slaveInfo.getSlaveCpus());
                        memory = (int) (((slaveInfo.getExecutorMem() * executorCount) + slaveInfo.getSlaveMem()) * 1.1); // 10% Overhead see mesos plugin
                        principal = mesosSlave.getCloud().getPrincipal();
                    }

                }
            }
        }
        /* build complete date */
        Calendar completedDate = Calendar.getInstance();
        completedDate.setTimeInMillis(run.getStartTimeInMillis() + duration);

        String nodeLabel = extractNodeLabels(run);

        return new JobBuildResult(createBuildResult(run.getResult()), buildName, buildClass,
                run.getNumber(), startDate.getTime(), completedDate.getTime(), executionStartDate.getTime(),
        		duration, queueDuration, nodeLabel, nodeName, extractUserNameIn(run), memory, cpus, principal, JENKINS.getRootUrl()  );
    }

    /**
     *
     * @param result - A Jenkins builds result
     * @return BuildResult - The matching enum to the @param result
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

    public static String extractUserNameIn(Run run) {
        String userName;

        Cause.UserCause uc = null;
        Cause.UserIdCause uic = null;

        uc = (Cause.UserCause) run.getCause(Cause.UserCause.class);
        uic = (Cause.UserIdCause) run.getCause(Cause.UserIdCause.class);

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
     * @param build - The build from which nodeLabel is extracted
     *
     * @return String - label of node without label "jobEnvProperties")
     */
    public static String extractNodeLabels(Run build) {

        Node node = null;

        if(build.getExecutor() != null && build.getExecutor().getOwner()!= null && build.getExecutor().getOwner().getNode() != null) {
            node = build.getExecutor().getOwner().getNode();
        }

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
