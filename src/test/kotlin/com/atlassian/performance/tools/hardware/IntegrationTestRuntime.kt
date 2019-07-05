package com.atlassian.performance.tools.hardware

import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder
import com.atlassian.performance.tools.aws.api.Aws
import com.atlassian.performance.tools.aws.api.TextCapacityMediator
import com.atlassian.performance.tools.workspace.api.RootWorkspace
import java.nio.file.Paths
import java.time.Duration
import java.util.*

object IntegrationTestRuntime {

    val rootWorkspace = RootWorkspace(Paths.get("build"))

    fun prepareAws() = Aws(
        credentialsProvider = AWSCredentialsProviderChain(
            STSAssumeRoleSessionCredentialsProvider.Builder(
                "arn:aws:iam::695067801333:role/server-gdn-bamboo",
                UUID.randomUUID().toString()
            ).build(),

            STSAssumeRoleSessionCredentialsProvider.Builder(
                "arn:aws:iam::342470128466:role/dcng-spike-jpt",
                UUID.randomUUID().toString()
            ).withStsClient(AWSSecurityTokenServiceClientBuilder
                .standard()
                .withCredentials(EC2ContainerCredentialsProviderWrapper())
                .build()
            ).build()
        ),
        region = Regions.EU_WEST_1,
        regionsWithHousekeeping = listOf(Regions.EU_WEST_1),
        capacity = TextCapacityMediator(Regions.EU_WEST_1),
        batchingCloudformationRefreshPeriod = Duration.ofSeconds(20)
    )
}
