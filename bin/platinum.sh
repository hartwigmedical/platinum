#!/bin/bash

echo "Running as: $(whoami)"
echo "Environment:"
env
echo "Specifically ADC: $GOOGLE_APPLICATION_CREDENTIALS"
echo "Gcloud config:"
gcloud config list
echo "Config directory:"
ls -l /root/.config/gcloud
echo "Getting clusters:"
gcloud container clusters get-credentials patient-cluster-verification-1 --region europe-west4 --project hmf-crunch
echo "Containers:"
gcloud container clusters list
echo "Alright running Platinum"
/usr/bin/java ${JAVA_OPTS} -jar /usr/share/platinum/bootstrap.jar "$@"
status=$?
if [ ${status} -ne 0 ]; then
  echo "Failed to start bootstrap: $status"
  exit ${status}
fi

