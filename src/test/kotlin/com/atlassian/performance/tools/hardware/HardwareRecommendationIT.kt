package com.atlassian.performance.tools.hardware

import com.amazonaws.services.ec2.model.InstanceType.C59xlarge
import com.amazonaws.services.ec2.model.InstanceType.M44xlarge
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.atlassian.performance.tools.aws.api.Aws
import com.atlassian.performance.tools.aws.api.Storage
import com.atlassian.performance.tools.aws.api.UnallocatedResource
import com.atlassian.performance.tools.awsinfrastructure.api.RemoteLocation
import com.atlassian.performance.tools.awsinfrastructure.api.jira.Jira
import com.atlassian.performance.tools.awsinfrastructure.api.jira.ProvisionedJira
import com.atlassian.performance.tools.awsinfrastructure.api.jira.StartedNode
import com.atlassian.performance.tools.hardware.guidance.SingleHardwareGuidance
import com.atlassian.performance.tools.hardware.instenv.Instenv
import com.atlassian.performance.tools.hardware.instenv.InstenvInstance
import com.atlassian.performance.tools.infrastructure.api.distribution.PublicJiraSoftwareDistribution
import com.atlassian.performance.tools.lib.LogConfigurationFactory
import com.atlassian.performance.tools.lib.s3cache.S3Cache
import com.atlassian.performance.tools.lib.workspace.GitRepo2
import com.atlassian.performance.tools.ssh.api.Ssh
import com.atlassian.performance.tools.ssh.api.SshHost
import com.atlassian.performance.tools.ssh.api.auth.PublicKeyAuthentication
import org.apache.logging.log4j.core.config.ConfigurationFactory
import org.eclipse.jgit.api.Git
import org.junit.Before
import org.junit.Test
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.net.URI
import java.nio.file.Paths

class HardwareRecommendationIT {

    private val taskName = "DCNG-195-7nodes-lb9"
    private val workspace = IntegrationTestRuntime.rootWorkspace.isolateTask(taskName)
    private val jswVersion = System.getProperty("hwr.jsw.version") ?: "8.1.0"

    @Before
    fun setUp() {
        ConfigurationFactory.setConfigurationFactory(LogConfigurationFactory(workspace))
    }

    @Test
    fun shouldRecommendHardware() {

//        requireCleanRepo()
        val aws = IntegrationTestRuntime.prepareAws()

        val engine = HardwareRecommendationEngine(
            product = PublicJiraSoftwareDistribution(jswVersion),
            scale = ApplicationScales().extraLarge(jiraVersion = jswVersion),
            jiraExploration = SingleHardwareGuidance(
                Hardware(
                    jira = C59xlarge,
                    nodeCount = 7,
                    db = M44xlarge)),
            dbInstanceTypes = listOf(
                M44xlarge
            ),
            maxErrorRate = 0.01,
            minApdex = 0.70,
            repeats = 1,
            aws = aws,
            workspace = workspace,
            s3Cache = S3Cache(
                transfer = TransferManagerBuilder.standard()
                    .withS3Client(aws.s3)
                    .build(),
                bucketName = "dkedzierski-bottlenecks",
                cacheKey = taskName,
                localPath = workspace.directory
            ),
            explorationCache = HardwareExplorationResultCache(workspace.directory.resolve("processed-cache.json")),
            instenvFileName = "/Users/damiankedzierski/projects/tools/jira-hardware-exploration/src/test/resources/jira-bottlenecks.yml"
        )
        engine.recommend()
    }

    private fun requireCleanRepo() {
        val status = Git(GitRepo2.findInAncestors(File(".").absoluteFile)).status().call()
        if (status.isClean.not()) {
            throw Exception("Your Git repo is not clean. Please commit the changes and consider pushing them.")
        }
    }
}
