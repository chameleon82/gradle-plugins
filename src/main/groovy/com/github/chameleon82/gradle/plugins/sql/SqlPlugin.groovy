package com.github.chameleon82.gradle.plugins.sql

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import com.github.chameleon82.gradle.plugins.sql.tasks.Init
import com.github.chameleon82.gradle.plugins.sql.tasks.Sql

public class SqlPlugin extends GroovyObjectSupport implements Plugin<Project> {

    public static final String SQL_TASK_NAME = "sql";

    private File buildDir;
    private String tmpDir;

    private Sql sqlTask;

    public SqlPlugin() {
    }

    void apply(Project project) {
        project.getPluginManager().apply(BasePlugin.class);
        //   project.extensions.create(SQL_TASK_NAME, SqlPluginExtension)    // Добавить плагин в список расширений пакета

        // Соглашения в проекте
        final SqlPluginConvention pluginConvention = new SqlPluginConvention(project);
        project.getConvention().getPlugins().put(SQL_TASK_NAME, pluginConvention);

        // Пример переопределения для задач
        //     project.tasks.withType(Sql) { task ->
        //           task.dirOrder = { pluginConvention.dirOrder } // Если порядок директорий переопределен - переопределяем и для задачи
        //      }
//
        buildDir = project.getBuildDir();
        tmpDir = "${buildDir}/tmp/sql"
        project.ext.archivesBaseName = "sql"

        /*
         * Находит список измененных/добавленных файлов в директории sqlAppDir (DB) относительно ветки MASTER
         */
        Exec diff = project.task('diff', type: Exec) {
            workingDir '.'
            executable = 'git'
            args = ['diff', '--diff-filter=AM', '--name-only', "${pluginConvention.sinceTag}...${pluginConvention.releaseTag}", "${pluginConvention.sqlAppDir}/"]
            standardOutput = new ByteArrayOutputStream()
            ext.output = { return standardOutput.toString().readLines() }

            outputs.upToDateWhen { false }
        }

        /*
         * Копирует список измененных в гите файлов в сборочную директорию
         */
        Copy collect = project.task('collect', type: Copy, dependsOn: diff) {

            from(diff.output) {
                eachFile { details ->
                    details.path = details.getFile().absolutePath.replace(project.getProjectDir().absolutePath, '')
                }
            }
            into tmpDir
            includeEmptyDirs = false

            outputs.upToDateWhen { false }

        }

        /*
         * Формируем скрипты запуска установки
         */
        DefaultTask doSql = project.task('dosql') {
            outputs.upToDateWhen { false }
            outputs.dir tmpDir

            doLast {

                def dimention = { filePath, group ->
                    def matcher = (filePath - pluginConvention.sqlAppDir =~ /[^\/]+/)
                    matcher[group]
                }

                pluginConvention.versions.each {
                    def exclude = it.value;

                    def allFilePaths = diff.output().findAll { !it.contains(exclude) }.groupBy(
                            { dimention(it, 0) }, { dimention(it, 1) }, { dimention(it, 2) }
                    )

                    File doSql = new File("${tmpDir}/" + it.key)
                    doSql.delete()

                    //project.getTasksByName('sql', true).find() { taskSql ->

                    //def users = []
                    //new File(outputDir + "/" + dbDir ).eachDir() { dir -> users += dir.getName() }
                    //users.sort() { a,b -> taskSql.userOrder.reverse().indexOf(b) <=> taskSql.userOrder.reverse().indexOf(a) }

                    /*     from(diff.output) {
                             eachFile { details ->
                                 details.path = details.getFile().absolutePath.replace(project.getProjectDir().absolutePath, '')
                             }
                         }
 */
                    project.getTasks().withType(Sql).find() { taskSql ->
                        taskSql.userOrder.each { user ->

                            def userFilePaths = allFilePaths[user]
                            if (userFilePaths != null) {

                                // Add text before each schema
                                doSql << taskSql.beforeEachSchema.replace(':user', "${user}")

                                pluginConvention.dirOrder.each { phase, order ->

                                    def phaseFilePaths = userFilePaths[phase]
                                    if (phaseFilePaths != null) {

                                        order.each { dbObj ->
                                            def dbObjFilePaths = phaseFilePaths[dbObj]
                                            if (dbObjFilePaths != null) {

                                                // execute each
                                                dbObjFilePaths.each {
                                                    if (!pluginConvention.singleFile) {
                                                        doSql << "PROMPT ${it}\n@@${it}\n\n"
                                                    } else {
                                                        doSql << "PROMPT ${it}\n\n"
                                                        doSql.append(project.file(it).readBytes())
                                                        doSql << "\n\n"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        doSql.dependsOn diff

        /*
         * Собирает changelog файл
         */
        Exec changeLog = project.task('changelog', type: Exec, dependsOn: [collect]) {
            executable = 'git'
            args = ['log', '--oneline', '--decorate', '--no-merges', "${pluginConvention.sinceTag}..${pluginConvention.releaseTag}", '--reverse', "--pretty=format:%h %s", "${pluginConvention.sqlAppDir}/"]
            standardOutput = new ByteArrayOutputStream()
            ext.log = { standardOutput.toString().readLines() }
            doLast {
                def cleanLog = ext.log().unique().join("\n")
                if (cleanLog) {
                    project.file("${buildDir}/tmp/sql/changelog.txt").setText(cleanLog)
                }
            }
        }

        /*
         * Выполняет всю работу
         * Собирает архив с набором инсталяционных файлов
         */

//        Sql sqlTask = (Sql)project.getTasks().create(SQL_TASK_NAME, Sql.class);

//        sqlTask.dependsOn([collect,doSql,changeLog])

        sqlTask = project.task('sql', type: Sql, dependsOn: [collect, doSql, changeLog]) {

            outputs.dir buildDir
            destinationDir = project.file(buildDir)
            //     baseName = 'sql'
            extension = 'tar'
            from(tmpDir)

            doFirst {

                if (wrapped != false) {
                    def wrapInOs, wrapInDocker = false
                    try {
                        project.exec {
                            executable 'wrap'
                            args "iname=build.gradle", "oname=build/build.wrapped"
                            standardOutput = new ByteArrayOutputStream()
                            errorOutput = new ByteArrayOutputStream()
                        }
                        wrapInOs = true
                        project.file("build/build.wrapped").delete()
                    } catch (Exception e) {
                    }

                    try {
                        project.exec {
                            executable 'docker'
                            args "run", "--rm", "chameleon82/oracle-xe-10g", "sh"
                            standardOutput = new ByteArrayOutputStream()
                            errorOutput = new ByteArrayOutputStream()
                        }
                        wrapInDocker = true
                    } catch (Exception e) {
                    }

                    if (!wrapInDocker && !wrapInOs) {
                        throw new GradleException('Cant wrap files. You should install Oracle client/server on build machine or have installed docker')
                    }
                    println(":wrap")
                    FileTree ioTree = project.fileTree(dir: tmpDir, includes: wrapped)

                    ioTree.each { f ->

                        def p = project.relativePath(f).replace("\\", "/")

                        if (wrapInOs) {
                            def res = project.exec {
                                workingDir project.file('.')
                                executable 'wrap'
                                args "iname=${p}", "oname=${p}.wrapped", "keep_comments=no"
                                standardOutput = new ByteArrayOutputStream()
                            }
                        } else if (wrapInDocker) {
                            project.exec {
                                executable 'docker'
                                args "run", "--rm", "-v", "${buildDir}:/tmp/build", "-w", "/tmp", "chameleon82/oracle-xe-10g", "wrap", "iname=${p}", "oname=${p}.wrapped", "keep_comments=no"
                                standardOutput = new ByteArrayOutputStream()
                            }
                        }

                        project.file("${f}").delete()
                        project.file("${f}.wrapped").renameTo("${f}");
                    }
                }

            }
        }

        sqlTask.setDescription("Generates a sql archive with patch increment.");
        sqlTask.setGroup("build");
        ArchivePublishArtifact warArtifact = new ArchivePublishArtifact(sqlTask);

        project.getTasksByName('build', true).each {
            it.dependsOn sqlTask
        }

        Init init = project.task('init', type: Init, dependsOn: sqlTask) {
            users = sqlTask.userOrder
            order = pluginConvention.dirOrder
        }
        init.setDescription("Creates structure for sql project");
        init.setGroup("build setup");


    }


}
