<div align="center">
  <h1>JavadocApi</h1>
</div>

JavadocApi can index source code (as a ZIP or in the file system) and generate
a SQLite database containing every exported class, field and method along with
a *structural* JSON representation of its Javadoc.

This project also contains a small fuzzy query-engine that can look up data
from multiple databases and fuzzy match it against user queries. This works
entirely by issuing SQLite commands, so RAM usage is kept quite low in
production.


## Demo

<div align="center">
  <img align="middle" src="https://github.com/I-Al-Istannen/JavadocApi/blob/master/media/example.svg?raw=true" height="400">
</div>


## Indexing a project
JavadocApi needs a json configuration as input that describes the project you
want to index. A few sample configurations can be found in
[`src/main/resources`](https://github.com/I-Al-Istannen/JavadocApi/tree/master/src/main/resources).
The format of the config is roughly this:

```js
{
  // The path to write the database to
  "outputPath": "target/Jdk-Index.db",
  // All paths to the project source code. If the project consists of multiple
  // src folders, you can list them here
  "resourcePaths": ["/usr/lib/jvm/java-17-openjdk/lib/src.zip"],
  // The packages to include in the database. `"*"` can be used as a wildcard
  // to include *all* packages.
  "allowedPackages": [
    "java.applet",
    "java.awt"
  ]
}
```

You can then simply run `java -jar JavadocApi.jar <path to config>`

### Having fun with the classpath

Sometimes knowing the classpath is quite convenient as it allows for better
resolution of link targets. JavadocApi understands (simple) maven and gradle
build files and uses them to build its classpath dynamically. The inner
workings are a bit scary, but it basically translates your gradle or maven file
into a simplified `pom.xml` and then asks maven to download and resolve all
needed dependencies.

To enable this feature, you need to set the `mavenHome` and `buildFiles` keys:

```js
{
  "outputPath": "target/JDA-Index.db",
  "resourcePaths": ["/home/i_al_istannen/.temp/Indizes/JDA.zip"],
  "allowedPackages": [
    "*"
  ],
  // All gradle/pom files you want JavadocApi to inspect
  "buildFiles": ["/home/i_al_istannen/.temp/Indizes/JDA/build.gradle.kts"],
  // Path to your maven home
  "mavenHome": "/opt/maven"
}
```
