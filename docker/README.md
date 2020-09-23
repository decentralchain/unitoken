# unitoken Node in Docker

## About unitoken
unitoken is a decentralized platform that allows any user to issue, transfer, swap and trade custom blockchain tokens on an integrated peer-to-peer exchange. You can find more information about unitoken at [decentralchain.com](https://decentralchain.com) and in the official [documentation]((https://docs.decentralchain.com)).


## About the image
This Docker image contains scripts and configs to run unitoken Node for `mainnet`, 'testnet' or 'stagenet' networks.
The image is focused on fast and convenient deployment of unitoken Node.

## Prerequisites
It is highly recommended to read more about [unitoken Node configuration](https://docs.decentralchain.com/en/unitoken-Node/Node-configuration.html) before running the container.

## Building Docker image

Dockerfile supports 3 main scenarios:
1. Basic scenario `docker build .` - build an image with the latest unitoken Node release available
*Note*: pre-releases are skipped
2. Existing Version scenario `docker build --build-arg unitoken_VERSION=1.1.1` - specify the version of unitoken Node available in GitHub Releases. If this version does not exist, this is the next scenario.
3. Build scenario `docker build --build-arg unitoken_VERSION=99.99.99 --build-arg BRANCH=version-0.17.x` - this scenario assumes that you want to build unitoken Node from sources. Use `unitoken_VERSION` build argument to specify a Git tag ('v' is added automatically) and `BRANCH` to specify a Git branch to checkout to. Make sure you specify a tag that does not exist in the repo, otherwise it is the previous scenario.

**You can specify following aarguments when building the inage:**


|Argument              | Default value |Description   |
|----------------------|-------------------|--------------|
|`unitoken_NETWORK`       | `mainnet`         | unitoken Blockchain network. Available values are `mainnet`, `testnet`, `stagenet`. Can be overridden in a runtime using environment variable with the same name.|
|`unitoken_VERSION`       | `latest`            | A node version which corresponds to the Git tag we want to use/create. |
|`BRANCH`              | `version-0.17.x`    | Relevant if Git tag 'v`unitoken_VERSION`' does not exist in the public repository. This option represents a Git branch we will use to compile unitoken node and set a Git tag on.|
|`SBT_VERSION`         | `1.2.8` 	       | Scala build tool version.|
|`unitoken_LOG_LEVEL`     | `DEBUG`           | Default unitoken Node log level. Available values: `OFF`, `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`. More details about logging are available [here](https://docs.decentralchain.com/en/unitoken-Node/logging-configuration.html). Can be overridden in a runtime using environment variable with the same name. |
|`unitoken_HEAP_SIZE`     | `2g`              | Default unitoken Node JVM Heap Size limit in -X Command-line Options notation (`-Xms=[your value]`). More details [here](https://docs.oracle.com/cd/E13150_01/jrockit_jvm/jrockit/jrdocs/refman/optionX.html). Can be overridden in a runtime using environment variable with the same name. |

**Note: All build arguments are optional.**  

## Running Docker image

### Configuration options

1. The image supports unitoken Node config customization. To change a config field use corrresponding JVM options. JVM options can be sent to JVM using `JAVA_OPTS` environment variable. Please refer to ([complete configuration file](https://raw.githubusercontent.com/decentralchain/unitoken/2634f71899e3100808c44c5ed70b8efdbb600b05/Node/src/main/resources/application.conf)) to get the full path of the configuration item you want to change.

```
docker run -v /docker/unitoken/unitoken-data:/var/lib/unitoken -v /docker/unitoken/unitoken-config:/etc/unitoken -p 6869:6869 -p 6862:6862 -e JAVA_OPTS="-Dunitoken.rest-api.enable=yes -Dunitoken.rest-api.bind-address=0.0.0.0 -Dunitoken.wallet.password=myWalletSuperPassword" -e unitoken_NETWORK=stagenet -ti decentralchain/unitokennode
```

2. unitoken Node is looking for a config in the directory `/etc/unitoken/unitoken.conf` which can be mounted using Docker volumes. If this directory does not exist, a default configuration will be copied to this directory. Default configuration is chosen depending on `unitoken_NETWORK` environment variable. If the value of `unitoken_NETWORK` is not `mainnet`, `testnet` or `stagenet`, default configuration won't be applied. This is a scenario of using `CUSTOM` network - correct configuration must be provided. If you use `CUSTOM` network and `/etc/unitoken/unitoken.conf` is NOT found unitoken Node container will exit.

3. By default, `/etc/unitoken/unitoken.conf` config includes `/etc/unitoken/local.conf`. Custom `/etc/unitoken/local.conf` can be used to override default config entries. Custom `/etc/unitoken/unitoken.conf` can be used to override or the whole configuration. For additional information about Docker volumes mapping please refer to `Managing data` item.

### Environment Variables

**You can run container with predefined environment variables:**

| Env variable                      | Description  |
|-----------------------------------|--------------|
| `unitoken_WALLET_SEED`        		| Base58 encoded seed. Overrides `-Dunitoken.wallet.seed` JVM config option. |
| `unitoken_WALLET_PASSWORD`           | Password for the wallet file. Overrides `-Dunitoken.wallet.password` JVM config option. |
| `unitoken_LOG_LEVEL`                 | Node logging level. Available values: `OFF`, `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`. More details about logging are available [here](https://docs.decentralchain.com/en/unitoken-Node/logging-configuration.html).|
| `unitoken_HEAP_SIZE`                 | Default Java Heap Size limit in -X Command-line Options notation (`-Xms=[your value]`). More details [here](https://docs.oracle.com/cd/E13150_01/jrockit_jvm/jrockit/jrdocs/refman/optionX.html). |
|`unitoken_NETWORK`                    | unitoken Blockchain network. Available values are `mainnet`, `testnet`, `stagenet`.|
|`JAVA_OPTS`                        | Additional unitoken Node JVM configuration options. 	|

**Note: All variables are optional.**  

**Note: Environment variables override values in the configuration file.** 


### Managing data
We recommend to store the blockchain state as well as unitoken configuration on the host side. As such, consider using Docker volumes mapping to map host directories inside the container:

**Example:**

1. Create a directory to store unitoken data:

```
mkdir -p /docker/unitoken
mkdir /docker/unitoken/unitoken-data
mkdir /docker/unitoken/unitoken-config
```

Once container is launched it will create:

- three subdirectories in `/docker/unitoken/unitoken-data`:
```
/docker/unitoken/unitoken-data/log    - unitoken Node logs
/docker/unitoken/unitoken-data/data   - unitoken Blockchain state
/docker/unitoken/unitoken-data/wallet - unitoken Wallet data
```
- `/docker/unitoken/unitoken-config/unitoken.conf` - default unitoken config


3. If you already have unitoken Node configuration/data - place it in the corresponsing directories


4. *Configure access permissions*. We use `unitoken` user with predefined uid/gid `143/143` to launch the container. As such, either change permissions of the created directories or change their owner:

```
sudo chmod -R 777 /docker/unitoken
```
or
```
sudo chown -R 143:143 /docker/unitoken      <-- prefered
```

5. Add the appropriate arguments to ```docker run``` command: 
```
docker run -v /docker/unitoken/unitoken-data:/var/lib/unitoken -v /docker/unitoken/unitoken-config:/etc/unitoken -e unitoken_NETWORK=stagenet -e unitoken_WALLET_PASSWORD=myWalletSuperPassword -ti decentralchain/unitokennode
```

### Blockchain state

If you are a unitoken Blockchain newbie and launching unitoken Node for the first time be aware that after launch it will start downloading the whole blockchain state from the other nodes. During this download it will be verifying all blocks one after another. This procesure can take some time.

You can speed this process up by downloading a compressed blockchain state from our official resources, extract it and mount inside the container (as discussed in the previous section). In this scenario unitoken Node skips block verifying. This is a reason why it takes less time. This is also a reason why you must download blockchain state *only from our official resources*.

**Note**: We do not guarantee the state consistency if it's downloaded from third-parties.

|Network     |Link          |
|------------|--------------|
|`mainnet`   | http://blockchain.decentralchain.com/blockchain_last.tar |
|`testnet`   | http://blockchain-testnet.decentralchain.com/blockchain_last.tar  |
|`stagenet`  | http://blockchain-stagenet.decentralchain.com/blockchain_last.tar |


**Example:**
```
mkdir -p /docker/unitoken/unitoken-data

wget -qO- http://blockchain-stagenet.decentralchain.com/blockchain_last.tar --show-progress | tar -xvf - -C /docker/unitoken/unitoken-data

chown -R 143:143 /docker/unitoken/unitoken-data

docker run -v /docker/unitoken/unitoken-data:/var/lib/unitoken decentralchain/Node -e unitoken_NETWORK=stagenet -e unitoken_WALLET_PASSWORD=myWalletSuperPassword -ti decentralchain/unitokennode
```

### Network Ports

1. REST-API interaction with Node. Details are available [here](https://docs.decentralchain.com/en/unitoken-Node/Node-configuration.html#section-530adfd0788eec3f856da976e4ce7ce7).

2. unitoken Node communication port for incoming connections. Details are available [here](https://docs.decentralchain.com/en/unitoken-Node/Node-configuration.html#section-fd33d7a83e3b2854f614fd9d5ae733ba).


**Example:**
Below command will launch a container:
- with REST-API port enabled and configured on the socket `0.0.0.0:6870`
- unitoken node communication port enabled and configured on the socket `0.0.0.0:6868`
- Ports `6868` and `6870` mapped from the host to the container

```
docker run -v /docker/unitoken/unitoken-data:/var/lib/unitoken -v /docker/unitoken/unitoken-config:/etc/unitoken -p 6870:6870 -p 6868:6868 -e JAVA_OPTS="-Dunitoken.network.declared-address=0.0.0.0:6868 -Dunitoken.rest-api.port=6870 -Dunitoken.rest-api.bind-address=0.0.0.0 -Dunitoken.rest-api.enable=yes" -e unitoken_WALLET_PASSWORD=myWalletSuperPassword -ti  decentralchain/unitokennode
```

Check that REST API is up by navigating to the following URL from the host side:
http://localhost:6870/api-docs/index.html

### Extensions
You can run custom extensions in this way:
1. Copy all lib/*.jar files from extension to any directory, lets say `plugins`
2. Add extension class to configuration file, lets say `local.conf`:
```hocon
unitoken.extensions += com.johndoe.unitokenExtension
```
3. Run `docker run -v "$(pwd)/plugins:/usr/share/unitoken/lib/plugins" -v "$(pwd)/local.conf:/etc/unitoken/local.conf" -i decentralchain/unitokennode`
