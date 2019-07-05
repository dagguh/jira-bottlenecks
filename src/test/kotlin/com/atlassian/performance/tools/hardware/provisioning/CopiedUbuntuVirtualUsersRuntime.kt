package com.atlassian.performance.tools.hardware.provisioning

import com.atlassian.performance.tools.aws.api.Storage
import com.atlassian.performance.tools.infrastructure.api.browser.Browser
import com.atlassian.performance.tools.infrastructure.api.jvm.OpenJDK
import com.atlassian.performance.tools.ssh.api.Ssh
import java.io.File

internal class CopiedUbuntuVirtualUsersRuntime {

    /**
     * @return remote JAR path
     */
    fun prepareForExecution(
        sshHost: Ssh,
        shadowJar: File,
        shadowJarTransport: Storage,
        browser: Browser
    ): String {
        shadowJarTransport.upload(shadowJar)
        sshHost.newConnection().use { ssh ->
            CopiedAwsCli().download(shadowJarTransport.location, ssh, target = ".")
            browser.install(ssh)
            OpenJDK().install(ssh)
        }
        return shadowJar.name
    }
}
