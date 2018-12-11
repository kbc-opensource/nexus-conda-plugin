# Nexus repository Anaconda plugin

Compatibility matrix
| Plugin version | Nexus repository version |
|----------------|--------------------------|
| v1.0.2         | 3.11.0                   |

### Build
* Clone the project
  `git clone ...`
  
* Build the plugin:
  ```
  cd nexus-conda-plugin
  mvn clean install
  ```
  
### Install

* Stop nexus
  `service nexus stop`
  or
  
    ```
    cd <nexus_dir>/bin
    ./nexus stop
    ```
    
* Copy the bundle into `<nexus_dir>/system/be/kbc/eap/nexus/nexus-conda-plugin/1.0.2/nexus-conda-plugin-1.0.2.jar`
* Make the following additions marked with + to `<nexus_dir>/system/org/sonatype/nexus/assemblies/nexus-core-feature/3.x.y/nexus-core-feature-3.x.y-features.xml`
   ```
         <feature prerequisite="false" dependency="false">nexus-repository-maven</feature>
   +     <feature prerequisite="false" dependency="false">nexus-conda-plugin</feature>
     </feature>
   ```
   And
   ```
   + <feature name="nexus-conda-plugin" description="be.kbc.eap:nexus-conda-plugin" version="1.0.2">
   +    <details>be.kbc.eap.nexus:nexus-conda-plugin</details>
   +    <bundle>mvn:be.kbc.eap.nexus/nexus-conda-plugin/x.y.z</bundle>
   +    <bundle>mvn:com.google.code.gson/gson/2.3.1</bundle>
   + </feature>
    </features>
   ```
 

This will cause the plugin to be loaded and started with each startup of Nexus Repository.

## Usage

* Setup proxy, group and hosted repo's as you would normally do
* To upload a package to conda use curl:
  `curl -F conda.asset1.path=win-64/<package>.tar.bz2 -F "conda.asset1.index=<index.json" -F conda.asset1=@package.tar.bz2  https://<server>/service/rest/beta/components?repository=<repository-name>`

## The Fine Print

It is worth noting that this is **NOT SUPPORTED** by Sonatype, and is a contribution of Bart Devriendt at KBC Belgium
plus us to the open source community (read: you!)

Remember:

* Use this contribution at the risk tolerance that you have
* Do **NOT** file Sonatype support tickets related to Conda support
* **DO** file issues here on GitHub, so that the community can pitch in

## Getting help

Looking to contribute to our code but need some help? There's a few ways to get information:

* Check out the [Nexus3](http://stackoverflow.com/questions/tagged/nexus3) tag on Stack Overflow
* Check out the [Nexus Repository User List](https://groups.google.com/a/glists.sonatype.com/forum/?hl=en#!forum/nexus-users)