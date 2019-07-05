package com.atlassian.performance.tools.hardware.instenv

class InstenvInstance {
    var type: String? = null
    var size: String? = null
    var dns_name: String? = null
    var ec2_id: String? = null
    var ssh: String? = null
    var app_url: String? = null

    fun ipAddress(): String {
        return ssh!!.split("@")[1]
    }

    fun sshKey(): String {
        return ssh!!.removePrefix("ssh -i").trim().split(" ")[0]
    }
}
