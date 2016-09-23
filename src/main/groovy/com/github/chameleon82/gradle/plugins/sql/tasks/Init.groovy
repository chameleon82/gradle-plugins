package com.github.chameleon82.gradle.plugins.sql.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Задача для создания структуры каталогов
 * TODO: неправильно подтягивает переменные users, order ( не инициализируются из основной таски )
 * @author nekrasov
 * @version $Id: $.
 * @created 07.02.2017 15:15
 */
class Init extends DefaultTask {

    def users, order
    @TaskAction
    def makeUserStructure() {

        def tree = new FileTreeBuilder()

        users.each { name ->
            println "$name"
            order.each { root, value ->
                value.each { sub ->
                    tree.dir("DB/$name/$root/$sub")
                }
            }
        }
    }
}