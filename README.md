<h1 align="center">🔷 unitoken Platform Node</h1>

<p align="center">

  <a href="https://travis-ci.org/decentralchain/unitoken" target="_blank">
    <img alt="Build status" src="https://travis-ci.org/decentralchain/unitoken.svg?branch=master"  />
  </a>
  <a href="https://github.com/decentralchain/unitoken/releases" target="_blank">
    <img alt="Downloads" src="https://img.shields.io/github/downloads/decentralchain/unitoken/total?cache=false&style=flat-square&style=flat-square" />
  </a>
  <a href="https://github.com/decentralchain/unitoken/blob/master/LICENSE" target="_blank">
    <img alt="License: MIT" src="https://badgen.net/github/license/decentralchain/unitoken"/>
  </a>
  <a href="https://github.com/decentralchain/unitoken/issues" target="_blank">
    <img alt="Open Issues" src="https://badgen.net/github/open-issues/decentralchain/unitoken" />
  </a>
  <a href="https://twitter.com/decentralchain" target="_blank">
    <img alt="Twitter: decentralchain" src="https://badgen.net/twitter/follow/unitokenglobalnews" />
  </a>
</p>

> unitoken is an open source [blockchain platform](https://decentralchain.com/).<br /> 
You can use it to build your own decentralised applications. unitoken provides full blockchain ecosystem including smart contracts language called RIDE.


## ✨ Demo

<p align="center">
    <img src="https://user-images.githubusercontent.com/1945126/78667964-88209480-78e2-11ea-9304-72178a6a5974.gif" alt="unitoken Node Run Demo">
</p>

unitoken node is a host connected to the blockchain network with the next functions:

- Processing and validation of [transactions](https://docs.decentralchain.com/en/blockchain/transaction/transaction-validation.html)
- Generation and storage of [blocks](https://docs.decentralchain.com/en/blockchain/block.html)
- Network [communication](https://docs.decentralchain.com/en/blockchain/transaction.html) with other nodes
- [REST API](https://docs.decentralchain.com/en/building-apps/how-to/basic/retrieve)
- [Extensions](https://docs.decentralchain.com/en/unitoken-node/extensions/) management

Learn more about unitoken Node in the [documentation](https://docs.decentralchain.com/en/unitoken-node/what-is-a-full-node.html).

## 🚀️ Getting started

A quick introduction of the minimal setup you need to get a running node. 

*Prerequisites:*
- configuration file for a needed network from [here](https://github.com/decentralchain/unitoken/tree/master/node)
- `unitoken-all*.jar` file from [releases](https://github.com/decentralchain/unitoken/releases) 

Linux systems:
```bash
sudo apt-get update
sudo apt-get install openjdk-8-jre
java -jar node/target/unitoken-all*.jar path/to/config/unitoken-{network}.conf
```

Mac systems (assuming already installed homebrew):
```bash
brew cask install adoptopenjdk/openjdk/adoptopenjdk8
java -jar node/target/unitoken-all*.jar path/to/config/unitoken-{network}.conf
```

Windows systems (assuming already installed OpenJDK 8):
```bash
java -jar node/target/unitoken-all*.jar path/to/config/unitoken-{network}.conf
```

Using docker image:
```bash
docker run -p 6869:6869 -p 6868:6868 -e unitoken_NETWORK=MAINNET -e unitoken_LOG_LEVEL=DEBUG -e unitoken_HEAP_SIZE=2g -v YOUR_LOCAL_PATH_HERE:/unitoken decentralchain/node
```

> More details on how to install a node for different platforms you can [find in the documentation](https://docs.decentralchain.com/en/unitoken-node/how-to-install-a-node/how-to-install-a-node#system-requirements). 

## 🔧 Configuration

The best starting point to understand available configuration parameters is the [this article](https://docs.decentralchain.com/en/unitoken-node/node-configuration).

The easiest way to start playing around with configurations is to use default configuration files for different networks, they're available [here](./node).

Logging configuration with all available levels and parameters is described [here](https://docs.decentralchain.com/en/unitoken-node/logging-configuration).

## 👨‍💻 Development

The node can be built and installed wherever Java can run. 
To build and test this project, you will have to follow these steps:

<details><summary><b>Show instructions</b></summary>

*1. Setup the environment.*
- Install Java for your platform:

```bash
sudo apt-get update
sudo apt-get install openjdk-8-jre                     # Ubuntu
# or
# brew cask install adoptopenjdk/openjdk/adoptopenjdk8 # Mac
```

- Install SBT (Scala Build Tool)

Please follow the SBT installation instructions depending on your platform ([Linux](https://www.scala-sbt.org/1.0/docs/Installing-sbt-on-Linux.html), [Mac](https://www.scala-sbt.org/1.0/docs/Installing-sbt-on-Mac.html), [Windows](https://www.scala-sbt.org/1.0/docs/Installing-sbt-on-Windows.html))

*2. Clone this repo*

```bash
git clone https://github.com/decentralchain/unitoken.git
cd unitoken
```

*3. Compile and run tests*

```bash
sbt checkPR
```

*4. Run integration tests (optional)*

Create a Docker image before you run any test: 
```bash
sbt node-it/docker
```

- Run all tests. You can increase or decrease number of parallel running tests by changing `unitoken.it.max-parallel-suites`
system property:
```bash
sbt -Dunitoken.it.max-parallel-suites=1 node-it/test
```

- Run one test:
```bash
sbt node-it/testOnly *.TestClassName
# or 
# bash node-it/testOnly full.package.TestClassName
```

*5. Build packages* 

```bash
sbt packageAll                   # Mainnet
sbt -Dnetwork=testnet packageAll # Testnet
```

`sbt packageAll` ‌produces only `deb` package along with a fat `jar`. 

*6. Install DEB package*

`deb` package is located in target folder. You can replace '*' with actual package name:

```bash
sudo dpkg -i node/target/*.deb
```


*7. Run an extension project locally during development (optional)*

```bash
sbt "extension-module/run /path/to/configuration"
```

*8. Configure IntelliJ IDEA (optional)*

The majority of contributors to this project use IntelliJ IDEA for development, if you want to use it as well please follow these steps:

1. Click on `Add configuration` (or `Edit configurations...`)
2. Click on `+` to add a new configuration, choose `Application`
3. Specify:
   - Main class: `com.decentralchain.Application`
   - Program arguments: `/path/to/configuration`
   - Use classpath of module: `extension-module`
4. Click on `OK`
5. Run this configuration

</details>

## 🤝 Contributing

If you'd like to contribute, please fork the repository and use a feature branch. Pull requests are warmly welcome.

For major changes, please open an issue first to discuss what you would like to change. Please make sure to update tests as appropriate.

Please follow the [code of conduct](./CODE_OF_CONDUCT.md) during communication with the each other. 

## ℹ️ Support (get help)

Keep up with the latest news and articles, and find out all about events happening on the [unitoken Platform](https://decentralchain.com/).

- [Telegram Dev Chat](https://t.me/unitoken_ride_dapps_dev)
- [unitoken Blog](https://blog.decentralchain.com/)

## ⛓ Links

- [Documentation](https://docs.decentralchain.com/)
- Blockchain clients for Mainnet: [unitoken Exchange](https://unitoken.exchange/), [unitoken FX](https://github.com/unitokenfx), [SIGN app](https://www.sign-web.app/)
- Blockchain clients for Testnet: [unitoken Exchange](https://testnet.unitoken.exchange/)
- Blockchain Explorer: [Mainnet](https://unitokenexplorer.com/), [Testnet](https://unitokenexplorer.com/testnet), [Stagenet](https://unitokenexplorer.com/stagenet) 
- [Ride Online IDE](https://ide.decentralchain.com/)

## 📝 Licence

The code in this project is licensed under [MIT license](./LICENSE)

## 👏 Acknowledgements

[<img src="https://camo.githubusercontent.com/97fa03cac759a772255b93c64ab1c9f76a103681/68747470733a2f2f7777772e796f75726b69742e636f6d2f696d616765732f796b6c6f676f2e706e67">](https://www.yourkit.com/)

We use YourKit full-featured Java Profiler to make unitoken node faster. YourKit, LLC is the creator of innovative and intelligent tools for profiling Java and .NET applications.

Take a look at YourKit's leading software products: YourKit Java Profiler and YourKit .NET Profiler.
