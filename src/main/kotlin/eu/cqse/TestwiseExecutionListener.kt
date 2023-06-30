package eu.cqse

import com.teamscale.report.testwise.model.ETestExecutionResult
import com.teamscale.tia.client.ITestwiseCoverageAgentApi
import com.teamscale.tia.client.RunningTest
import com.teamscale.tia.client.TestRun
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure
import java.io.IOException
import org.junit.runner.notification.RunListener;
import java.util.*

private const val TEAMSCALE_JACOCO_AGENT_URL_PROPERTY: String = "JACOCO_AGENT_URL"

/**
 * A TestExecutionListener for JUnit5 that sends messages of test starts and ends to the Teamscale JaCoCo Agent.
 */
class TestwiseExecutionListener() : TestExecutionListener, RunListener() {
    private val testApi: ITestwiseCoverageAgentApi?
    private var currentTestResult: TestExecutionResult = TestExecutionResult.successful()

    init {
        testApi = getTeamscaleAgentService()
    }

    override fun executionStarted(testIdentifier: TestIdentifier?) {
        if (testIdentifier == null) {
            return
        }
        if (testIdentifier.isTest) {
            val testSource: Optional<TestSource> = testIdentifier.source ?: return
            if (testSource.isPresent) {
                return
            }
            val testMethodSource: MethodSource = testSource.get() as MethodSource
            val testPath = testMethodSource.className + "." + testIdentifier.legacyReportingName
            startTest(testPath)
        }
    }

    override fun testStarted(description: Description?) {
        currentTestResult = TestExecutionResult.successful()
        super.testStarted(description)
        if (description != null) {
            startTest(description.displayName)
        }
    }

    override fun executionFinished(testIdentifier: TestIdentifier?, testExecutionResult: TestExecutionResult?) {
        if (testIdentifier == null || !testIdentifier.parentId.isPresent || !testIdentifier.source.isPresent) {
            return
        }
        if (testIdentifier.isTest && testExecutionResult != null) {
            val testSource: Optional<TestSource> = testIdentifier.source
            if (testSource.isPresent) {
                return
            }
            val testMethodSource: MethodSource = testSource.get() as MethodSource
            val testPath = testMethodSource.className + "." + testIdentifier.legacyReportingName
            endTest(testPath, testExecutionResult)
        } else {
            endTestRun()
        }
    }

    override fun testFinished(description: Description?) {
        super.testFinished(description)
        if (description != null) {
            endTest(description.displayName, currentTestResult)
        }
    }

    override fun testFailure(failure: Failure?) {
        super.testFailure(failure)
        currentTestResult = TestExecutionResult.failed(failure?.exception)
    }

    override fun testRunFinished(result: Result?) {
        super.testRunFinished(result)
        endTestRun()
    }

    private fun startTest(testUniformPath: String) {
        if (testApi == null) {
            return
        }
        try {
            testApi.testStarted(testUniformPath).execute()
        } catch (e: IOException) {
            println("Error while calling service api.")
        }
    }

    private fun endTest(testUniformPath: String,
        testExecutionResult: TestExecutionResult
    ) {
        RunningTest(testUniformPath, testApi).endTest(
            TestRun.TestResultWithMessage(
                toTestExecutionResult(testExecutionResult.status),
                null
            )
        )
    }

    private fun endTestRun() {
        if (testApi == null) {
            return
        }
        try {
            testApi.testRunFinished().execute()
        } catch (e: IOException) {
            println("Error contacting test wise coverage agent.")
        }
    }

    private fun getTeamscaleAgentService(): ITestwiseCoverageAgentApi? {
        val agentUrl: String = System.getProperty(TEAMSCALE_JACOCO_AGENT_URL_PROPERTY)
        if (agentUrl.isBlank()) {
            return null
        }
        val url: HttpUrl = agentUrl.toHttpUrl()
        return ITestwiseCoverageAgentApi.createService(url)
    }

    private fun toTestExecutionResult(status: TestExecutionResult.Status): ETestExecutionResult {
        return when (status) {
            TestExecutionResult.Status.SUCCESSFUL -> ETestExecutionResult.PASSED
            TestExecutionResult.Status.ABORTED -> ETestExecutionResult.SKIPPED
            TestExecutionResult.Status.FAILED -> ETestExecutionResult.FAILURE
        }
    }
}