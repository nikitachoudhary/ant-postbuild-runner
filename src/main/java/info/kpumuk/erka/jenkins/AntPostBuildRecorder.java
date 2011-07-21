package info.kpumuk.erka.jenkins;

import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.Ant;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.PrintStream;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/** 
 * @author Roman Dmytrenko
 */
public class AntPostBuildRecorder extends Recorder {

	private final String name;
	private String targets;
	protected Builder testRunner;

	@DataBoundConstructor
	public AntPostBuildRecorder(String name, String targets) {
		this.name = name;
		this.targets = targets;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	public String getName() {
		return name;
	}

	public String getAntTargets() {
		return targets;
	}

	@Override
	public boolean needsToRunAfterFinalized() {
		return true;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		boolean result = false;
		Result buildResult = build.getResult();

		if (buildResult.equals(SUCCESS) || buildResult.equals(UNSTABLE)) {
			Builder runner = runner();
			PrintStream logger = listener.getLogger();
			logger.append("[ant postbuild runner] running.....\r\n");
			result = runner.perform(build, launcher, listener);
			if (!result) {
				build.setResult(Result.FAILURE);
			}
		}
		return result;
	}

	protected Builder runner() {
		if (testRunner == null) {
			return new Ant(this.targets, "ant-default", "", "", "");
		}
		return testRunner;
	}

	@SuppressWarnings("unchecked")
	@Override
	public BuildStepDescriptor getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public DescriptorImpl() {
			super(AntPostBuildRecorder.class);
		}

		/**
		 * Performs on-the-fly validation of the form field 'name'.
		 * 
		 * @param value
		 *            This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the
		 *         browser.
		 */
		public FormValidation doCheckAntTargets(@QueryParameter String antTargets) throws IOException, ServletException {
			if (antTargets == null || antTargets.length() == 0) {
				return FormValidation.error("Please set ant targets");
			}
			return FormValidation.ok();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		public String getDisplayName() {
			return "ant postbuild runner";
		}

		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			String antTargets = formData.getString("antTargets");
			return new AntPostBuildRecorder(this.getDisplayName(), antTargets);
		}
	}

}
