# Gradle SQL plugin

## Usage

To use the SQL plugin, include the following in you build script
```
apply plugin: 'sql'
```

## Tasks

**Sql plugin tasks**

| Task name | DependsOn | Type | Description                             |
|-----------|-----------|------|-----------------------------------------|
| sql       |           | Sql  | Assembles the database SQL distributive |

**Sql plugin additional task dependencies**

| Task name | Depends On |
|-----------|------------|
| assemble  | sql        |

## Project layout

**Sql plugin project layout**

| Directory | Meaning                                                      |
|-----------|--------------------------------------------------------------|
| DB        | Sql resources such as DDL, DML and other incremental scripts |

## Convention properties

**Sql plugin convention properties**

| Property name | Type       | Default value | Description                           |
|---------------|------------|---------------|---------------------------------------|
| sqlAppDir     | String     | DB            | The name of scripts source directory  |
| dirOrder      | MultiArray | see above*    | Path ordering of project source files |

**dirOrder default value**
```groovy
[
    'BEFORE' : ['CONSTRAINTS', 'DML', 'EXECUTES', 'INDEXES', 'SEQUENCES', 'SYNONYMS', 'TABLES'],
    'PROGRAM': ['TYPES', 'PACKAGES', 'PROCEDURES', 'FUNCTIONS', 'VIEWS', 'MVIEWS', 'TRIGGERS', 'PACKAGE_BODIES'],
    'AFTER'  : ['DML', 'EXECUTES', 'GRANTS', 'SETTINGS']
];
```
For example: `'BEFORE' : ['CONSTRAINTS']` means than the path `BEFORE/CONSTRAINTS` should be included into build and must be first in build ordering in `do.sql` file.

## Sql

The default behaviour of Sql task is to copy of the content `sqlAppDir` include and ordering by `dirOrder` to the root of the archive filtering by comparing git current branch HEAD and master branch and create `start.sql` and `do.sql` files.

## Customizing

Here is an example with the most important customization options:

**build.gradle**
```groovy
project.version = 'x.x.x' 

buildscript {
    repositories {
        maven { url 'http://still:8081/artifactory/libs-release-local' }
    }
    dependencies {
        classpath group: 'ru.ftc.plugins', name: 'sql', version: '1.0.0'
    }
}

apply plugin: "sql"

sql {
    // use this user-dir ordering in start.sql file
    userOrder = ['TRCARD', 'SOCMMO', 'NKEYS', 'CARDS', 'GATE', 'ETICKET', 'TT_SRV', 'OPEN_CFG', 'TC_MAN', 'MPS', 'ARM_MAN', 'ARM_PTK', 'ARM_CETK', 'ARM_OT', 'RETK', 'REPROC']

    // base name of archive
    baseName = "core"

    // filter to find files, which should been apply with oracle database WRAP command
    wrapped = ['**/PROGRAM/**/PACKAGE_BODIES/**', '**/PROGRAM/**/FUNCTIONS/**', '**/PROGRAM/**/PROCEDURES/**']

    // Delimeter text for each schema ( user in userOrder declaration )
    // For example, you can use this var for template to connect to each schema before scripts must be executed
    // Also, you can use bind variable :user, which been binded with user in userOrder variable
    beforeEachSchema = "CONN bazooka[:user]/&&bazooka_pwd.&&dbhost\n" +
            "PROMPT :user installation ...\n\n"

}
```

## Wrap action

You can wrap files in two ways. First way is just install full oracle database client (>=10g) on building machine. Second way is use installed Docker. Builder decides which variant can be used and wrap if you configure it. By default this action is not applying.

## Structure of the archive

```
sql-X.X.X.tar -- task artifact
|- start.sql  -- executable file to apply patch
|- do.ee.sql -- this file contains patch ordering for Enterprise Edtion Database
|- do.se.sql -- this file contains patch ordering for Standart/Express Edition Database
|- changelog.txt -- this file contains commits between current and previous releases
|- DB/..  -- this directory contains files with patch increment
```