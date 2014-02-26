package org.jenkinsci.plugins.cppcheck;


import com.thalesgroup.hudson.plugins.cppcheck.graph.CppcheckGraph;
import com.thalesgroup.hudson.plugins.cppcheck.util.AbstractCppcheckBuildAction;
import hudson.model.AbstractBuild;
import hudson.model.HealthReport;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import hudson.util.Graph;
import org.jenkinsci.plugins.cppcheck.config.CppcheckConfig;
import org.jenkinsci.plugins.cppcheck.config.CppcheckConfigGraph;
import org.jenkinsci.plugins.cppcheck.util.CppcheckBuildHealthEvaluator;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.Calendar;

/**
 * @author Gregory Boissinot
 */
public class CppcheckBuildAction extends AbstractCppcheckBuildAction {

    public static final String URL_NAME = "cppcheckResult";

    private CppcheckResult result;
    private CppcheckConfig cppcheckConfig;

    public CppcheckBuildAction(AbstractBuild<?, ?> owner, CppcheckResult result, CppcheckConfig cppcheckConfig) {
        super(owner);
        this.result = result;
        this.cppcheckConfig = cppcheckConfig;
    }

    public String getIconFileName() {
        return "/plugin/cppcheck/icons/cppcheck-24.png";
    }

    public String getDisplayName() {
        return Messages.cppcheck_CppcheckResults();
    }

    public String getUrlName() {
        return URL_NAME;
    }

    public String getSearchUrl() {
        return getUrlName();
    }

    public CppcheckResult getResult() {
        return this.result;
    }

    AbstractBuild<?, ?> getBuild() {
        return this.owner;
    }

    public Object getTarget() {
        return this.result;
    }

    public HealthReport getBuildHealth() {
        try {
            return new CppcheckBuildHealthEvaluator().evaluatBuildHealth(cppcheckConfig,
                    result.getNumberErrorsAccordingConfiguration(cppcheckConfig, false));
        } catch (IOException ioe) {
            return new HealthReport();
        }
    }

    private DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> getDataSetBuilder() {
        DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb
                = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();

        for (CppcheckBuildAction a = this; a != null; a = a.getPreviousResult()) {
            ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(a.owner);
            CppcheckStatistics statistics = a.getResult().getStatistics();
            CppcheckConfigGraph configGraph = cppcheckConfig.getConfigGraph();

            // error
            if (configGraph.isDisplayErrorSeverity())
                dsb.add(statistics.getNumberErrorSeverity(),
                        Messages.cppcheck_Error(), label);

            //warning
            if (configGraph.isDisplayWarningSeverity())
                dsb.add(statistics.getNumberWarningSeverity(),
                        Messages.cppcheck_Warning(), label);

            //style
            if (configGraph.isDisplayStyleSeverity())
                dsb.add(statistics.getNumberStyleSeverity(),
                        Messages.cppcheck_Style(), label);

            //performance
            if (configGraph.isDisplayPerformanceSeverity())
                dsb.add(statistics.getNumberPerformanceSeverity(),
                        Messages.cppcheck_Performance(), label);

            //information
            if (configGraph.isDisplayInformationSeverity())
                dsb.add(statistics.getNumberInformationSeverity(),
                        Messages.cppcheck_Information(), label);

            //no category
            if (configGraph.isDisplayNoCategorySeverity())
                dsb.add(statistics.getNumberNoCategorySeverity(),
                        Messages.cppcheck_NoCategory(), label);

            //portability
            if (configGraph.isDisplayPortabilitySeverity())
                dsb.add(statistics.getNumberPortabilitySeverity(),
                        Messages.cppcheck_Portability(), label);

            // all errors
            if (configGraph.isDisplayAllErrors())
                dsb.add(statistics.getNumberTotal(),
                        Messages.cppcheck_AllErrors(), label);
        }
        return dsb;
    }

    public void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
        if (ChartUtil.awtProblemCause != null) {
            rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
            return;
        }

        Calendar timestamp = getBuild().getTimestamp();
        if (req.checkIfModified(timestamp, rsp)) {
            return;
        }

        Graph g = new CppcheckGraph(getOwner(), getDataSetBuilder().build(),
                Messages.cppcheck_NumberOfErrors(),
                cppcheckConfig.getConfigGraph().getXSize(),
                cppcheckConfig.getConfigGraph().getYSize());
        g.doPng(req, rsp);
    }

    // Backward compatibility
    @Deprecated
    private transient AbstractBuild<?, ?> build;

    /**
     * Initializes members that were not present in previous versions of this plug-in.
     *
     * @return the created object
     */
    private Object readResolve() {
        if (build != null) {
            this.owner = build;
        }
        return this;
    }
}
