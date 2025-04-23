# Sonatype Nexus repository Anaconda plugin

Compatibility matrix
---------------------------------------------
| Plugin version | Nexus repository version | Changes                                |
|----------------|--------------------------|----------------------------------------|
| v1.0.4         | 3.15.0                   |                                        |
| v1.0.6         | 3.20.0                   |                                        |
| v1.0.7         | 3.29.0                   |                                        |
| v1.0.8         | 3.29.0                   |                                        |
| ~~v1.0.9~~     | 3.38.1                   |                                        |
| v1.1.0         | 3.38.1                   |                                        |
| ~~v1.2.0~~     | 3.38.1                   |                                        |
| v1.3.0         | 3.38.1                   |                                        |
| v1.4.0         | 3.54.1                   |                                        |
| v1.4.1         | 3.54.1                   | Adds API routes for Conda repositories |
| v1.6.0         | 3.61.0                   | Adds Support for ZST files             |
| v1.8.0         | 3.68.1                   | Adds Support for PostgreSQL DB         |
| v1.10.0        | 3.70.2                   | Fixes some issues                      |
| v1.13.0| 3.70.2 | Adds unit tests and a fix for nested grouping<br> Fixes caching in group repositories |
| v1.14.0| 3.77.2 | Version bump |

> 3.77.2 is the last version that supports loading plugins from the deploy folder until Sonatype adds support again for plugins in a future version.

### Build
* Clone the project
  `git clone ...`

* Build the plugin:
  ```
  cd nexus-conda-plugin
  mvn clean install -PbuildKar
  ```

* This will create a kar file:  `target/nexus-conda-plugin-1.8.0-bundle.kar`

### Install

* Stop nexus
  `dzdo systemctl stop nexus`
  or
    ```
    cd <nexus_dir>/bin
    ./nexus stop
    ```

* Copy the bundle into `<nexus_dir>/deploy`

This will cause the plugin to be loaded and started with each startup of Nexus Repository.

* Also make sure that you disable all `nexus-repository-conda` plugins, in every xml of Nexus. Otherwise Nexus will crash.
  Locations of xml's:
  `<nexus_dir>/system/org/sonatype/nexus/assemblies`
  `<nexus_dir>/system/com/sonatype/nexus/assemblies/`

## Usage

* Setup proxy, group and hosted repo's as you would normally do
* To upload a package to conda use curl:
  `curl -F conda.asset.path=win-64/<package>.tar.bz2 -F "conda.asset.index=<index.json" -F conda.asset=@package.tar.bz2  https://<server>/service/rest/v1/components?repository=<repository-name>`
  index.json contains the index.json file contained in the tar.bz2 file

## The Fine Print

It is worth noting that this is **NOT SUPPORTED** by Sonatype, and is a contribution of Bart Devriendt at KBC Belgium
to the open source community (read: you!)

Remember:

* Use this contribution at the risk tolerance that you have
* Do **NOT** file Sonatype support tickets related to Conda support
* **DO** file issues here on GitHub, so that the community can pitch in

## Getting help

Looking to contribute to our code but need some help? There's a few ways to get information:

* Check out the [Nexus3](http://stackoverflow.com/questions/tagged/nexus3) tag on Stack Overflow
* Check out the [Nexus Repository User List](https://groups.google.com/a/glists.sonatype.com/forum/?hl=en#!forum/nexus-users)
