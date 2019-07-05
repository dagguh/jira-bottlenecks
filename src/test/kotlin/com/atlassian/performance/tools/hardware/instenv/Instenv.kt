package com.atlassian.performance.tools.hardware.instenv

import com.atlassian.performance.tools.hardware.HardwareRecommendationIT

class Instenv {
    var app: InstenvApp? = null
    var elb: InstenvLoadBalancer? = null
    var ec2: InstenvEc2? = null
    var rds: InstenvRds? = null
    var nfs: InstenvNfs? = null
    var end: String? = null
}
