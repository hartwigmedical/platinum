#!/bin/bash

echo "Running as: $(whoami)"
echo "Gcloud config:"
gcloud config --list
echo "Alright running Platinum"
/usr/bin/java ${JAVA_OPTS} -jar /usr/share/platinum/bootstrap.jar "$@"
status=$?
if [ ${status} -ne 0 ]; then
  echo "Failed to start bootstrap: $status"
  exit ${status}
fi

