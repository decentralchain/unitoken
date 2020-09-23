#!/bin/bash
NETWORKS="mainnet testnet stagenet"

mkdir -p /var/lib/unitoken/log
if [ ! -f /etc/unitoken/unitoken.conf ]; then
  echo "Custom '/etc/unitoken/unitoken.conf' not found. Using a default one for '${unitoken_NETWORK,,}' network." | tee -a /var/log/unitoken/unitoken.log
  if [[ $NETWORKS == *"${unitoken_NETWORK,,}"* ]]; then
    cp /usr/share/unitoken/conf/unitoken-${unitoken_NETWORK}.conf /etc/unitoken/unitoken.conf
    sed -i 's/include "local.conf"//' /etc/unitoken/unitoken.conf
    for f in /etc/unitoken/ext/*.conf; do
      echo "Adding $f extension config to unitoken.conf";
      echo "include required(\"$f\")" >> /etc/unitoken/unitoken.conf
    done
    echo 'include "local.conf"' >> /etc/unitoken/unitoken.conf
  else
    echo "Network '${unitoken_NETWORK,,}' not found. Exiting."
    exit 1
  fi
else
  echo "Found custom '/etc/unitoken/unitoken.conf'. Using it."
fi

if [ "${unitoken_VERSION}" == "latest" ]; then
  filename=$(find /usr/share/unitoken/lib -name unitoken-all* -printf '%f\n')
  export unitoken_VERSION=$(echo ${filename##*-} | cut -d\. -f1-3)
fi

[ -n "${unitoken_WALLET_PASSWORD}" ] && JAVA_OPTS="${JAVA_OPTS} -Dunitoken.wallet.password=${unitoken_WALLET_PASSWORD}"
[ -n "${unitoken_WALLET_SEED}" ] && JAVA_OPTS="${JAVA_OPTS} -Dunitoken.wallet.seed=${unitoken_WALLET_SEED}"

JAVA_OPTS="${JAVA_OPTS} -Dunitoken.data-directory=/var/lib/unitoken/data -Dunitoken.directory=/var/lib/unitoken"

echo "Node is starting..." | tee -a /var/log/unitoken/unitoken.log
echo "unitoken_HEAP_SIZE='${unitoken_HEAP_SIZE}'" | tee -a /var/log/unitoken/unitoken.log
echo "unitoken_LOG_LEVEL='${unitoken_LOG_LEVEL}'" | tee -a /var/log/unitoken/unitoken.log
echo "unitoken_VERSION='${unitoken_VERSION}'" | tee -a /var/log/unitoken/unitoken.log
echo "unitoken_NETWORK='${unitoken_NETWORK}'" | tee -a /var/log/unitoken/unitoken.log
echo "unitoken_WALLET_SEED='${unitoken_WALLET_SEED}'" | tee -a /var/log/unitoken/unitoken.log
echo "unitoken_WALLET_PASSWORD='${unitoken_WALLET_PASSWORD}'" | tee -a /var/log/unitoken/unitoken.log
echo "JAVA_OPTS='${JAVA_OPTS}'" | tee -a /var/log/unitoken/unitoken.log

exec java -Dlogback.stdout.level=${unitoken_LOG_LEVEL} \
  -XX:+ExitOnOutOfMemoryError \
  -Xmx${unitoken_HEAP_SIZE} \
  -Dlogback.file.directory=/var/log/unitoken \
  -Dconfig.override_with_env_vars=true \
  ${JAVA_OPTS} \
  -cp "/usr/share/unitoken/lib/plugins/*:/usr/share/unitoken/lib/*" \
  com.decentralchain.Application \
  /etc/unitoken/unitoken.conf
