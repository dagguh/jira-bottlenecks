package com.atlassian.performance.tools.hardware.provisioning

import com.amazonaws.services.ec2.model.Subnet
import com.amazonaws.services.ec2.model.Vpc

internal class CopiedNetwork(
    val vpc: Vpc,
    val subnet: Subnet
)
