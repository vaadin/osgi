# Vaadin OSGi

This project provides an integration with Vaadin and OSGi environments.

| Branch       | Vaadin/Flow version       | Java version |
|--------------|---------------------------|--------------|
| master (8.1) | Vaadin 23.3+ / Flow 23.3+ | JDK11        |
| 8.0          | Vaadin 23.2 / Flow 23.2   | JDK11        |
| 7.0          | Vaadin 22 / Flow 9.0      | JDK8         |

## Build and test

### Build required Flow modules

In order to execute OSGi add-on tests, you need to build locally a couple of Flow modules
that are not available on remote maven repositories.

Checkout the Flow branch specific for the OSGi add-on branch, and then run

```terminal
mvn -DskipTests -am -pl flow-tests,flow-plugins/flow-maven-plugin,flow-html-components-testbench,flow,vaadin-dev-server clean install
mvn -f flow-tests/test-common clean install
```

For Flow 23.3+, the `test-root-context` module, that contains the Flow views and the test code, must be packaged as a JAR instead of WAR.
Edit the `flow-tests/test-root-context/pom.xml` file and modify the `<packaging>` tag from `war` to `jar`, then build the module.

```terminal
mvn -f flow-tests/test-root-context -DskipTests -Prun-tests clean install
```

### Build and run OSGi tests

To build all modules without executing, tests run `mvn -DskipTests clean install`.

To run validation for non bnd-tools container, execute

```terminal
mvn -P\!bnd-tools -Dvaadin.allow.appshell.annotations=true verify
```

To run validation for bnd-tools container, execute

```terminal
mvn -Pbnd-tools -Dvaadin.allow.appshell.annotations=true verify
```

The following system properties can be added to the command line:

| Property name                               | Description                                                                                                                                            | Example                                                                                                |
|---------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| flow.version                                | Execute agains a specific flow version                                                                                                                 | `-Dflow.version=9.1-SNAPSHOT`                                                                          |
| testbench.version                           | Execute agains a specific Testbench version                                                                                                            | `-Dtestbench.version=7.1-SNAPSHOT`                                                                     |
| webdriver.chrome.driver                     | Use a local chromedriver executable. Useful when the Selenium manager is not able to fetch the correct driver for the local Google Chrome installation | `-Dwebdriver.chrome.driver=/home/user/.cache/selenium/chromedriver/linux64/116.0.5845.96/chromedriver` |
| com.vaadin.testbench.Parameters.maxAttempts | To rerun failed test, in case of flakyness                                                                                                             | `-Dcom.vaadin.testbench.Parameters.maxAttempts=2`                                                      |


### Troubleshooting

If the test fail because of errors in the browser console, try to delete the `pnpm` cache (e.g. `$HOME/.cache/pnpm`)
and rebuild the `test-root-context`. 