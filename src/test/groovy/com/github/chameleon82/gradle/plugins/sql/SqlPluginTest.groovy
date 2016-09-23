package com.github.chameleon82.gradle.plugins.sql

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import static org.junit.Assert.*

class SqlPluginTest {
    @Test
    public void sqlPluginAddsSqlTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'sql'

      //  assertTrue(project.tasks.sql instanceof Sql)
    }
}
