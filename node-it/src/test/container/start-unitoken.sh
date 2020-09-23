#!/bin/bash
echo Options: $unitoken_OPTS
exec java $unitoken_OPTS -cp "/opt/unitoken/lib/*" com.decentralchain.Application /opt/unitoken/template.conf
