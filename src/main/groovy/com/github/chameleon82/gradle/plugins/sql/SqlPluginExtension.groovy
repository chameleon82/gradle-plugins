package com.github.chameleon82.gradle.plugins.sql

/**
 *
 *
 * @author nekrasov
 * @version $Id: $.
 * @created 08.02.2017 11:56
 */
// TODO: Плагин должер работать через extension, из SqlPlugin нужно все таски перенести как экшены в экстеншен
// Сейчас класс отключен
class SqlPluginExtension {

    def String[] userOrder = ['SYS', 'SYSTEM']

    def String baseName = "sql"

    def Boolean wrapped = false

    def String beforeEachSchema = ""

}
