<Server port="8005" shutdown="SHUTDOWN">

	<Listener
		className="org.apache.catalina.core.AprLifecycleListener"
		SSLEngine="on"/>

	<GlobalNamingResources>

		<Resource
			name="UserDatabase"
			auth="Container"
			type="org.apache.catalina.UserDatabase"
			description="User database that can be updated and saved"
			factory="org.apache.catalina.users.MemoryUserDatabaseFactory"
			pathname="conf/tomcat-users.xml"/>

	</GlobalNamingResources>

	<Service name="api">

		<Executor
			name="api-executor"
			namePrefix="api-exec-"
			maxThreads="150"
			minSpareThreads="4"/>

		<Connector
			port="8081"
			protocol="HTTP/1.1"
			connectionTimeout="20000"
			redirectPort="8444" />

		<Engine
			name="api-engine"
			defaultHost="localhost">

			<Realm
				className="org.apache.catalina.realm.UserDatabaseRealm"
				resourceName="UserDatabase"/>

			<Host
				name="localhost"
				appBase="apps/api"
				unpackWARs="false"
				autoDeploy="false">

				<Context
					docBase="ROOT"
					path=""
					reloadable="true"/>

			</Host>

		</Engine>

	</Service>

</Server>