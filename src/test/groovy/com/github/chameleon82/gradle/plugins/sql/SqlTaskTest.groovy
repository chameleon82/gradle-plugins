package com.github.chameleon82.gradle.plugins.sql

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import com.github.chameleon82.gradle.plugins.sql.tasks.Sql

import static org.junit.Assert.*

class SqlTaskTest {
    @Test
    public void canAddTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        def task = project.task('greeting', type: Sql)
        assertTrue(task instanceof Sql)
    }
}
