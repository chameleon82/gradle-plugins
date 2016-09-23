package com.github.chameleon82.gradle.plugins.sql

import org.gradle.api.Project

import javax.inject.Inject

/**
 * Соглашение о переменныъ в плагине
 *
 * @author nekrasov
 * @version $Id: $.
 * @created 08.02.2017 12:46
 */


public class SqlPluginConvention extends GroovyObjectSupport {
    private final Project project;
    private dirOrder;
    private sqlAppDir;
    private releaseTag;
    private sinceTag;
    private versions;
    // TODO: for next releases
    //private dirOrderMode; // only | all
    private singleFile;

    @Inject
    public SqlPluginConvention(Project project) {
        this.project = project;
        this.dirOrder = [
                'BEFORE' : ['EXECUTES', 'DML', 'SEQUENCES', 'TABLES', 'INDEXES', 'CONSTRAINTS', 'SYNONYMS'],
                'PROGRAM': ['TYPES', 'PACKAGES', 'PROCEDURES', 'FUNCTIONS', 'VIEWS', 'MVIEWS', 'TRIGGERS', 'PACKAGE_BODIES'],
                'AFTER'  : ['DML', 'EXECUTES', 'GRANTS', 'SETTINGS']
        ];
        this.sqlAppDir = 'DB'
        this.releaseTag = 'HEAD';
        this.sinceTag = 'master';
        this.versions = ['do.se.sql': '.ee.sql',
                         'do.ee.sql': '.se.sql'
        ];
        this.singleFile = false;
        // this.dirOrderMode = 'only';
    }


}
