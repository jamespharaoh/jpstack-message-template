-- TODO:
-- - verify plugin dependencies
-- - establish and use correct plugin order
-- - output eclipse configs
-- - output web configs

{-# LANGUAGE Arrows #-}
{-# LANGUAGE NoMonomorphismRestriction #-}

module Main where

import Data.List
import Data.String.Utils (replace)

import Text.XML.HXT.Core

import Wbs.Config

writeBuildFile ::
	WorldConfig ->
	IO ()

writeBuildFile worldConfig = do

	let buildConfig =
		wcBuild worldConfig

	let projectsAndPlugins =
		wcProjectsAndPlugins worldConfig

	let makeProperties =
		[
			mkelem "property" [
				sattr "environment" "env"
			] [],
			mkelem "property" [
				sattr "file" "${env.WBS_BUILD_PROPERTIES}"
			] []
		]

	let makeClasspath =
		[
			mkelem "path" [
				sattr "id" "classpath"
			] [
				mkelem "fileset" [
					sattr "dir" "lib",
					sattr "includes" "*.jar"
				] [],
				mkelem "pathelement" [
					sattr "path" "work/bin"
				] []
			]
		]

	let makeComboTarget name parts =
		mkelem "target" [
			sattr "name" name,
			sattr "depends" $
				intercalate ", " $
					map ("just-" ++) parts
		] []

	let makeComboTargets =
		[

			makeComboTarget "clean" [
				"clean" ],

			makeComboTarget "build" [
				"build-framework",
				"build-rest" ],

			makeComboTarget "framework-jar" [
				"build-framework",
				"framework-jar" ],

			makeComboTarget "console-live" [
				"build-framework",
				"build-rest",
				"console-live" ],

			makeComboTarget "console-test" [
				"build-framework",
				"build-rest",
				"console-test" ],

			makeComboTarget "console-auto" [
				"build-framework",
				"build-rest",
				"console-live",
				"console-restart" ],

			makeComboTarget "api-live" [
				"build-framework",
				"build-rest",
				"api-live" ],

			makeComboTarget "api-test" [
				"build-framework",
				"build-rest",
				"api-test" ],

			makeComboTarget "api-auto" [
				"build-framework",
				"build-rest",
				"api-live",
				"api-restart" ],

			makeComboTarget "daemon-auto" [
				"build-framework",
				"build-rest",
				"daemon-restart" ],

			makeComboTarget "all-auto" [
				"build-framework",
				"build-rest",
				"api-live",
				"api-restart",
				"console-live",
				"console-restart",
				"daemon-restart" ],

			makeComboTarget "javadoc" [
				"javadoc" ],

			makeComboTarget "fixtures" [
				"build-framework",
				"build-rest",
				"db-drop",
				"db-create",
				"schema-create",
				"sql-schema",
				"sql-data",
				"fixtures" ],

			makeComboTarget "tomcat-test" [
				"build-framework",
				"build-rest",
				"console-test",
				"api-test",
				"tomcat-test" ]

		]

	let makeSimpleTarget name elems =
		mkelem "target" [
			sattr "name" $ "just-" ++ name
		] elems

	let makeDeleteDir dir =
		mkelem "delete" [
			sattr "dir" dir
		] []

	let makeMkdir dir =
		mkelem "mkdir" [
			sattr "dir" dir
		] []

	let makeJavacTask attrs =
		mkelem "javac" ([
			sattr "destdir" "work/bin",
			sattr "debug" "on",
			sattr "includeantruntime" "false",
			sattr "srcdir" "src",
			sattr "classpathref" "classpath",
			sattr "encoding" "utf8"
		] ++ attrs) []

	let makeCleanTargets =
		[
			makeSimpleTarget "clean" [
				makeDeleteDir "work/bin",
				makeDeleteDir "work/test"
			]
		]

	let makeIncludeName name =
		mkelem "include" [
			sattr "name" name
		] []

	let makeExcludeName name =
		mkelem "exclude" [
			sattr "name" name
		] []

	let makeFilesetDir dir elems =
		mkelem "fileset" [
			sattr "dir" dir
		] elems

	let makeCopyToDir todir elems =
		mkelem "copy" [
			sattr "todir" todir
		] elems

	let makeCopyDirToDir dir todir =
		makeCopyToDir todir [
			makeFilesetDir dir []
		]

	let makeCopyToDirNofail todir elems =
		mkelem "copy" [
			sattr "todir" todir,
			sattr "failonerror" "false"
		] elems

	let makeCopyFileToDir file todir elems =
		mkelem "copy" [
			sattr "file" file,
			sattr "todir" todir
		] elems

	let makeCopyFileToFile file tofile =
		mkelem "copy" [
			sattr "file" file,
			sattr "tofile" tofile
		] []

	let makeMoveFileToFile file tofile =
		mkelem "move" [
			sattr "file" file,
			sattr "tofile" tofile
		] []

	let makeBuildTargets =
		[
			makeSimpleTarget "build-framework" [
				makeMkdir "work/bin",
				makeJavacTask [
					sattr "includes" "wbs/framework/**"
				],
				makeMkdir "work/bin/META-INF/services",
				mkelem "echo" [
					sattr "file" "work/bin/META-INF/services/javax.annotation.processing.Processor",
					sattr "message" "wbs.framework.object.ObjectHelperAnnotationProcessor"
				] []
			],
			makeSimpleTarget "framework-jar" [
				mkelem "jar" [
					sattr "destfile" "work/wbs-framework.jar",
					sattr "basedir" "work/bin"
				] [
					mkelem "include" [
						sattr "name" "wbs/framework/**"
					] [],
					mkelem "service" [
						sattr "type" "javax.annotation.processing.Processor",
						sattr "provider" "wbs.framework.object.ObjectHelperAnnotationProcessor"
					] []
				]
			],
			makeSimpleTarget "build-rest" [
				makeJavacTask [
					sattr "excludes" "wbs/framework/**"
				],
				makeCopyToDir "work/bin" [
					makeFilesetDir "src" [
						makeIncludeName "**/*.xml",
						makeIncludeName "log4j.properties"
					]
				],
				makeCopyFileToDir "wbs-build.xml" "work/bin" []
			]
		]


	let pluginDir projectConfig pluginConfig =
		"src/" ++
		(replace "." "/" $ prcPackage projectConfig) ++
		"/" ++
		(replace "." "/" $ plcPackage pluginConfig)

	let makeWebTarget name env = let

		workDir =
			"work/" ++ env ++ "/" ++ name

		pluginDir (projectConfig, pluginConfig) =
			"src/" ++
			(replace "." "/" $ prcPackage projectConfig) ++
			"/" ++
			(replace "." "/" $ plcPackage pluginConfig)

		makeWebFileset projectAndPlugin = let

			thisPluginDir =
				pluginDir projectAndPlugin

			in makeFilesetDir (
				if isInfixOf ("/" ++ name ++ "/") thisPluginDir
				then (
					(pluginDir projectAndPlugin) ++
					"/files"
				) else (
					(pluginDir projectAndPlugin) ++
					"/" ++
					name ++
					"/files"
				)
			) []

		in makeSimpleTarget (name ++ "-" ++ env) [

			makeMkdir workDir,
			makeMkdir $ workDir ++ "/WEB-INF",
			makeMkdir $ workDir ++ "/WEB-INF/classes",
			makeMkdir $ workDir ++ "/WEB-INF/lib",

			makeCopyToDirNofail workDir $
				map makeWebFileset projectsAndPlugins,

			makeCopyToDir (workDir ++ "/WEB-INF/classes") [
				makeFilesetDir "work/bin" []
			],

			makeCopyToDir (workDir ++ "/WEB-INF/lib") [
				makeFilesetDir "lib" [
					makeExcludeName "service-api.jar"
				]
			],

			makeCopyFileToFile
				(name ++ "/web-" ++ env ++ ".xml")
				(workDir ++ "/WEB-INF/web.xml")

		]

	let makeArgLine line =
		mkelem "arg" [
			sattr "line" line
		] []

	let makeArgValue value =
		mkelem "arg" [
			sattr "value" value
		] []

	let makeExec exec elems =
		mkelem "exec" [
			sattr "failonerror" "false",
			sattr "executable" exec
		] elems

	let makeExecNoFail exec elems =
		mkelem "exec" [
			sattr "failonerror" "true",
			sattr "executable" exec
		] elems

	let makeExecDir dir exec elems =
		mkelem "exec" [
			sattr "failonerror" "true",
			sattr "dir" dir,
			sattr "executable" exec
		] elems

	let makeTomcatTarget env = let

		workDir =
			"work/" ++ env

		tomcatDir =
			 workDir ++ "/tomcat"

		in makeSimpleTarget ("tomcat-" ++ env) [

			-- deploy tomcat

			makeMkdir "temp",

			makeExecDir "temp" "tar" [
				makeArgLine "--extract",
				makeArgLine "--file ../binaries/packages/apache-tomcat-6.0.37.tar.gz"
			],

			makeDeleteDir $ tomcatDir ++ "/**",
			makeMoveFileToFile "temp/apache-tomcat-6.0.37" tomcatDir,

			-- configure tomcat

			makeCopyFileToFile
				("console/server-" ++ env ++ ".xml")
				(tomcatDir ++ "/conf/server.xml"),

			makeCopyFileToFile
				"conf/tomcat-users.xml"
				(tomcatDir ++ "/conf/tomcat-users.xml"),

			-- deploy console

			makeDeleteDir $ tomcatDir ++ "/apps/console/ROOT",

			makeCopyDirToDir
				(workDir ++ "/console")
				(tomcatDir ++ "/apps/console/ROOT"),

			makeCopyDirToDir
				(tomcatDir ++ "/webapps/manager")
				(tomcatDir ++ "/apps/console/manager"),

			makeCopyDirToDir
				(tomcatDir ++ "/webapps/host-manager")
				(tomcatDir ++ "/apps/console/host-manager"),

			-- deploy api

			makeDeleteDir $ tomcatDir ++ "/apps/api/ROOT",

			makeCopyDirToDir
				(workDir ++ "/api")
				(tomcatDir ++ "/apps/api/ROOT"),

			makeCopyDirToDir
				(tomcatDir ++ "/webapps/manager")
				(tomcatDir ++ "/apps/api/manager"),

			makeCopyDirToDir
				(tomcatDir ++ "/webapps/host-manager")
				(tomcatDir ++ "/apps/api/host-manager"),

			makeExec (tomcatDir ++ "/bin/catalina.sh") [
				makeArgLine "run"
			]

		]

	let makeWebTargets env =
		[
			makeWebTarget "console" env,
			makeWebTarget "api" env,
			makeTomcatTarget env
		]

	let makeServiceTarget name service action =
		makeSimpleTarget (name ++ "-" ++ action) [

			makeExec "./service" [
				makeArgValue service,
				makeArgValue action
			]

		]

	let makeServiceTargets =
		[
			makeServiceTarget "api" "tomcat_api" "restart",
			makeServiceTarget "console" "tomcat_console" "restart",
			makeServiceTarget "daemon" "daemon" "restart"
		]

	let makeJavadocTargets =
		[
			makeSimpleTarget "javadoc" [

				makeMkdir "javadoc",

				mkelem "javadoc" [
					sattr "destdir" "javadoc",
					sattr "access" "private",
					sattr "linksource" "yes"
				] [
					makeFilesetDir "src" [],
					mkelem "classpath" [
						sattr "refid" "classpath"
					] [],
					mkelem "link" [
						sattr "href" "http://java.sun.com/j2se/1.6.0/docs/api"
					] [],
					mkelem "link" [
						sattr "href" "http://logging.apache.org/log4j/docs/api"
					] [],
					mkelem "link" [
						sattr "href" "http://www.hibernate.org/hib_docs/v3/api"
					] [],
					mkelem "link" [
						sattr "href" "http://www.xom.nu/apidocs"
					] []
				]

			]
		]

	let makeDatabaseTargets =
		[
			makeSimpleTarget "db-drop" [
				makeExec "dropdb" [
					makeArgLine "${database.name}"
				]
			],
			makeSimpleTarget "db-create" [
				makeExecNoFail "createdb" [
					makeArgLine "${database.name}"
				]
			]
		]

	let makeScriptName name =
		mkelem "script" [
			sattr "name" name
		] []

	let makeSqlScripts getter suffix (projectConfig, pluginConfig) =
		map makeOne (getter pluginConfig)
		where makeOne scriptName =
			makeScriptName $
				"src/" ++
				(replace "." "/" $ prcPackage projectConfig) ++
				"/" ++
				(replace "." "/" $ plcPackage pluginConfig) ++
				"/model/" ++
				scriptName ++
				suffix ++
				".sql"

	let makeSqlTarget name getter suffix =
		makeSimpleTarget name [

			mkelem "taskdef" [
				sattr "name" "database-init",
				sattr "classname" "wbs.framework.utils.ant.DatabaseInitTask",
				sattr "classpathref" "classpath"
			] [],

			mkelem "database-init" [] $
				concat $
					map (makeSqlScripts getter suffix) projectsAndPlugins

		]

	let makeSqlTargets =
		[
			makeSqlTarget "sql-schema" plcSqlSchemas "",
			makeSqlTarget "sql-data" plcSqlDatas ""
		]

	let makeSchemaTargets =
		[
			makeSimpleTarget "schema-create" [

				mkelem "java" [
					sattr "classname"
						"wbs.platform.application.tools.BeanRunner",
					sattr "classpathref" "classpath",
					sattr "failonerror" "true"
				] [
					makeArgValue "wbs-test",
					makeArgValue "wbs.test",
					makeArgValue "config,data,entity,schema,sql,schema-tool",
					makeArgValue "test",
					makeArgValue "wbs.framework.schema.tool.SchemaTool",
					makeArgValue "schemaCreate"
				]

			]
		]

	let makeFixtureTargets =
		[
			makeSimpleTarget "fixtures" [

				mkelem "java" [
					sattr "classname"
						"wbs.platform.application.tools.BeanRunner",
					sattr "classpathref" "classpath",
					sattr "failonerror" "true"
				] [
					makeArgValue "wbs-test",
					makeArgValue "wbs.test",
					makeArgValue ("config,data,entity,schema,sql,model," ++
						"hibernate,object,logic,fixture"),
					makeArgValue "test,hibernate",
					makeArgValue "wbs.framework.fixtures.FixturesTool",
					makeArgValue "createFixtures"
				]

			]
		]

	let makeProject =
		root [] [
			mkelem "project" [
				sattr "name" $ bcName buildConfig,
				sattr "basedir" ".",
				sattr "default" "build"
			] (
				makeProperties ++
				makeClasspath ++
				makeComboTargets ++
				makeCleanTargets ++
				makeBuildTargets ++
				makeWebTargets "live" ++
				makeWebTargets "test" ++
				makeServiceTargets ++
				makeJavadocTargets ++
				makeDatabaseTargets ++
				makeSqlTargets ++
				makeSchemaTargets ++
				makeFixtureTargets
			)
		]

	let writeProject =
		writeDocument [withIndent yes] "build.xml"

	runX (makeProject >>> writeProject)

	return ()

main ::
	IO ()

main = do

	worldConfig <-
		loadWorld

	writeBuildFile worldConfig

	return ()
