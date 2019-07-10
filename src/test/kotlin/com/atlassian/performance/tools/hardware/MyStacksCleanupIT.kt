package com.atlassian.performance.tools.hardware

import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
import com.amazonaws.regions.Regions.EU_WEST_1
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder
import com.amazonaws.services.support.AWSSupportClientBuilder
import com.atlassian.performance.tools.aws.api.Aws
import com.atlassian.performance.tools.aws.api.ProvisionedStack
import com.atlassian.performance.tools.aws.api.SupportCapacityMediator
import com.atlassian.performance.tools.aws.api.currentUser
import com.atlassian.performance.tools.lib.LogConfigurationFactory
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.core.config.ConfigurationFactory
import org.junit.Test
import java.time.Duration
import java.time.Instant.now
import java.util.*


class MyStacksCleanupIT {
    @Test
    fun cleanUp(){
        ConfigurationFactory.setConfigurationFactory(LogConfigurationFactory(IntegrationTestRuntime.rootWorkspace.currentTask))
        val logger: Logger = LogManager.getLogger(this::class.java)

        val credentialsProvider =
            STSAssumeRoleSessionCredentialsProvider.Builder(
                "arn:aws:iam::342470128466:role/dcng-spike-jpt",
                UUID.randomUUID().toString()
            ).withStsClient(AWSSecurityTokenServiceClientBuilder
                .standard()
                .withCredentials(EC2ContainerCredentialsProviderWrapper())
                .build()
            ).build()


        val aws = Aws(
            credentialsProvider = credentialsProvider,
            region = EU_WEST_1,
            regionsWithHousekeeping = listOf(EU_WEST_1),
            capacity = SupportCapacityMediator(
                AWSSupportClientBuilder
                    .standard()
                    .withCredentials(credentialsProvider)
                    .withRegion(EU_WEST_1)
                    .build(),
                EU_WEST_1
            ),
            batchingCloudformationRefreshPeriod = Duration.ofMinutes(1)
        )

        DeleteMyStacks(aws, logger).perform()
    }
}



private class DeleteMyStacks(
    private val aws: Aws,
    private val logger: Logger
) {
    fun perform() {
        logger.info("Pulling stacks data...")
        val stacks = aws.listDisposableStacks()
            .map { ProvisionedStack(it, aws) }
            .filter { it.bambooBuild.isNullOrEmpty() }
            .filter { it.user == currentUser() }
        if (stacks.isNotEmpty()) {
            warn(stacks.size)
            logger.info("Deleting stacks...")
            stacks
                .map { stack ->
                    stack
                        .release()
                        .thenAccept { logger.info("Deleted ${stack.stackName} ") }
                }
                .forEach { it.get() }
            logger.info("All of your stacks have been deleted")
        } else {
            logger.info("No stacks to delete")
        }
    }

    private fun warn(stackCount: Int) {
        val wait = Duration.ofSeconds(10)
        logger.info("Will start deleting $stackCount stacks in ${wait.seconds}s. Press CTRL+C to interrupt.")
        countdown(
            duration = wait,
            action = { logger.info("$it") }
        )
    }

    private fun countdown(
        duration: Duration,
        action: (secondsRemaining: Long) -> Unit
    ) {
        val end = now() + duration
        (duration.seconds downTo 0).forEach { remaining ->
            val waitedFor = end - Duration.ofSeconds(remaining)
            while (now().isBefore(waitedFor)) {
            }
            action(remaining)
        }
    }
}
