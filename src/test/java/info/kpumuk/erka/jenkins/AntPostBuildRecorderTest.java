package info.kpumuk.erka.jenkins;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Ant;
import hudson.util.FormValidation;

import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;

/** 
 * @author Roman Dmytrenko
 */
public class AntPostBuildRecorderTest {
	private AntPostBuildRecorder recorder;

	@Before
	public void setUp() {
		recorder = new AntPostBuildRecorder("Test name", "default");
	}

	@Test
	public void testGetName() throws Exception {
		assertThat(recorder.getName(), equalTo("Test name"));
	}

	@Test
	public void testGetAntTargets() throws Exception {
		assertThat(recorder.getAntTargets(), equalTo("default"));
	}

	@Test
	public void testGetRequiredMonitorService() throws Exception {
		assertThat(recorder.getRequiredMonitorService(), equalTo(BuildStepMonitor.BUILD));
	}

	@Test
	public void testNeedsToRunAfterFinalized() throws Exception {
		assertThat(recorder.needsToRunAfterFinalized(), equalTo(true));
	}

	@Test
	public void testPerformWhenBuildFailed() throws Exception {
		AbstractBuild build = mock(AbstractBuild.class);
		when(build.getResult()).thenReturn(Result.FAILURE);
		Launcher launcher = null;
		BuildListener listener = null;
		boolean result = recorder.perform(build, launcher, listener);
		assertThat(result, equalTo(false));
	}

	@Test
	public void testPerformWhenBuildSuccessful() throws Exception {
		AbstractBuild build = mock(AbstractBuild.class);
		when(build.getResult()).thenReturn(Result.SUCCESS);
		Launcher launcher = mock(Launcher.class);
		PrintStream logger = mock(PrintStream.class);
		BuildListener listener = mock(BuildListener.class);
		when(listener.getLogger()).thenReturn(logger);
		Builder runner = mock(Builder.class);
		recorder.testRunner = runner;
		boolean result = recorder.perform(build, launcher, listener);
		assertThat(result, equalTo(false));
		verify(build).getResult();
		verify(listener).getLogger();
		verify(logger).append("[ant postbuild runner] running.....\r\n");
		verify(runner).perform(build, launcher, listener);
	}

	@Test
	public void testDoCheckAntTargetsWhenUserPutsNothing() throws Exception {
		AntPostBuildRecorder.DescriptorImpl descriptor = new AntPostBuildRecorder.DescriptorImpl();
		FormValidation validation = descriptor.doCheckAntTargets("");
		assertThat(validation.kind, equalTo(FormValidation.Kind.ERROR));
		validation = descriptor.doCheckAntTargets(null);
		assertThat(validation.kind, equalTo(FormValidation.Kind.ERROR));
		assertThat(validation.getMessage(), equalTo("Please set ant targets"));
	}

	@Test
	public void testDoCheckAntTargetsWhenUserPutsSomeTask() throws Exception {
		AntPostBuildRecorder.DescriptorImpl descriptor = new AntPostBuildRecorder.DescriptorImpl();
		FormValidation validation = descriptor.doCheckAntTargets("echo");
		assertThat(validation.kind, equalTo(FormValidation.Kind.OK));
	}
	
	@Test
	public void testRunner() {
		Builder runner = recorder.runner();
		assertTrue(runner instanceof Ant);
	}
}
