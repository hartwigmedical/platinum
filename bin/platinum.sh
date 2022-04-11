#!/bin/bash

gcloud auth activate-service-account --key-file=/google-key.json
gcloud container clusters get-credentials patient-cluster-verification-1 --region europe-west4 --project hmf-crunch
kubectl create clusterrolebinding default-role --clusterrole=edit --serviceaccount=default:default --namespace=default
/usr/bin/java ${JAVA_OPTS} -jar /usr/share/platinum/bootstrap.jar "$@"
status=$?
if [ ${status} -ne 0 ]; then
  echo "Failed to start bootstrap: $status"
  exit ${status}
fi

