# TestwiseExecutionListener
Testwise Coverage Listener for the Teamscale JaCoCo Agent

# Usage
Install locally using the command

```mvn clean install```

Then you can add it as a dependency to your maven or gradle project with 

Maven:
```
<dependency>
  <groupId>eu.cqse</groupId>
  <artifactId>TestwiseExecutionListener</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

Gradle:

Add the repository
```
repositories {
  mavenLocal()
}
```
And then the dependency
```
implementation 'eu.cqse:TestwiseExecutionListener:1.0-SNAPSHOT'
```

Finally, you need to tell the service loader to use the test listener.
To Achieve this, put a file named ```org.junit.platform.launcher.TestExecutionListener``` in the resources folder of your project.
The content of the file should be ```eu.cqse.TestwiseExecutionListener```.

# Configuration
The only thing that needs to be set is the URL of the Teamscale JaCoCo Agent.
This can be done with System Properties. 
For example in maven surefire, it can be set as follows
```
<configuration>
  <systemPropertyVariables>
    <JACOCO_AGENT_URL>http://localhost:8123</JACOCO_AGENT_URL>
  </systemPropertyVariables>
</configuration>
```
