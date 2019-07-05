package com.atlassian.performance.tools.hardware.instenv

import com.atlassian.performance.tools.hardware.HardwareRecommendationIT

class InstenvEc2 {
    var description: String? = null
    var instances: List<InstenvInstance> = ArrayList()

    fun homeNode(): InstenvInstance {
        return instances.filter { e -> "nfs".equals(e.type) }.first()
    }

    fun jiraNodes(): List<InstenvInstance> {
        return instances.filter { e -> "application".equals(e.type) }
    }
}
