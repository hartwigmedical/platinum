#!/bin/bash

echo "Running as: $(whoami)"
echo "Environment:"
env
echo "Specifically ADC: $GOOGLE_APPLICATION_CREDENTIALS"
echo "Access token: $(gcloud auth application-default print-access-token)"
echo "Key contents: $(cat $GOOGLE_APPLICATION_CREDENTIALS)"
gcloud auth activate-service-account --key-file=/google-key.json
echo "Gcloud config:"
gcloud config list
echo "Config directory:"
ls -l /root/.config/gcloud
echo "Getting clusters:"
gcloud container clusters get-credentials patient-cluster-verification-1 --region europe-west4 --project hmf-crunch
echo "Containers:"
gcloud container clusters list
echo "Kubectl config:"
kubectl config view
echo "Control server:"
echo "Ping:"
ping -c1 192.168.23.50
echo "Curl to https:"
curl https://192.168.23.50
echo "Alright running Platinum"
export GOOGLE_APPLICATION_CREDENTIALS=$GOOGLE_APPLICATION_CREDENTIALS
/usr/bin/java ${JAVA_OPTS} -jar /usr/share/platinum/bootstrap.jar "$@"
status=$?
if [ ${status} -ne 0 ]; then
  echo "Failed to start bootstrap: $status"
  exit ${status}
fi

