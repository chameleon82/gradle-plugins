package com.github.chameleon82.gradle.plugins.sql.tasks

import org.gradle.api.tasks.bundling.Tar

class Sql extends Tar { // extends DefaultTask { extends ConventionTask

    def userOrder = ['SYS', 'SYSTEM'];

    def wrapped = false

    public String beforeEachSchema = "";

    public Sql() {
        this.baseName = 'sql'
        this.from()
    }
}