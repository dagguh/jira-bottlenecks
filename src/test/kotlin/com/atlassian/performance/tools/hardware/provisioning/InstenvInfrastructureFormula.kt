package com.atlassian.performance.tools.hardware.provisioning

import com.atlassian.performance.tools.aws.api.*
import com.atlassian.performance.tools.awsinfrastructure.api.Infrastructure
import com.atlassian.performance.tools.awsinfrastructure.api.jira.ProvisionedJira
import com.atlassian.performance.tools.awsinfrastructure.api.virtualusers.VirtualUsersFormula
import com.atlassian.performance.tools.concurrency.api.submitWithLogContext
import com.atlassian.performance.tools.infrastructure.api.virtualusers.ResultsTransport
import com.atlassian.performance.tools.infrastructure.api.virtualusers.VirtualUsers
import com.atlassian.performance.tools.ssh.api.SshConnection
import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.Executors

class InstenvInfrastructureFormula<out T : VirtualUsers>(
    private val investment: Investment,
    private val provisionedJira: ProvisionedJira,
    private val virtualUsersFormula: VirtualUsersFormula<T>,
    private val aws: Aws
) {

    fun provision(
        workingDirectory: Path
    ): ProvisionedInstenvInfrastructure<T> {
        val nonce = investment.reuseKey()

        val resultsStorage = aws.resultsStorage(nonce)
        val roleProfile = aws.shortTermStorageAccess()

        val executor = Executors.newCachedThreadPool(
            ThreadFactoryBuilder()
                .setNameFormat("provisioning-thread-%d")
                .build()
        )
        val keyProvisioning = executor.submitWithLogContext("provision key") {
            SshKeyFormula(
                ec2 = aws.ec2,
                workingDirectory = workingDirectory,
                prefix = nonce,
                lifespan = investment.lifespan
            ).provision()
        }

        val provisionVirtualUsers = executor.submitWithLogContext("virtual users") {
            virtualUsersFormula.provision(
                investment = investment,
                shadowJarTransport = aws.virtualUsersStorage(nonce),
                resultsTransport = S3ResultsTransport(
                    results = resultsStorage
                ),
                key = keyProvisioning,
                roleProfile = roleProfile,
                aws = aws
            )
        }

        val provisionedVirtualUsers = provisionVirtualUsers.get()
        val sshKey = keyProvisioning.get()

        executor.shutdownNow()

        return ProvisionedInstenvInfrastructure(
            infrastructure = Infrastructure(
                virtualUsers = provisionedVirtualUsers.virtualUsers,
                jira = provisionedJira.jira,
                resultsTransport = resultsStorage,
                sshKey = sshKey
            ),
            resource = CompositeResource(
                listOf(
                    provisionedJira.resource,
                    provisionedVirtualUsers.resource,
                    sshKey.remote
                )
            )
        )
    }

    class ProvisionedInstenvInfrastructure<out T : VirtualUsers>(
        val infrastructure: Infrastructure<T>,
        val resource: Resource
    ) {
        override fun toString(): String {
            return "ProvisionedInfrastructure(infrastructure=$infrastructure, resource=$resource)"
        }
    }

    class S3ResultsTransport(
        private val results: Storage
    ) : ResultsTransport {

        override fun transportResults(
            targetDirectory: String,
            sshConnection: SshConnection
        ) {

            CopiedAwsCli().upload(results.location, sshConnection, targetDirectory, Duration.ofMinutes(10))
        }

        override fun toString(): String {
            return "S3ResultsTransport(results=$results)"
        }
    }
}
