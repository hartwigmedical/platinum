#!/usr/bin/env bash

[[ $# -ne 1 ]] && echo "Provide: [name of the cluster]" && exit 1

echo -n "Checking gcloud..."
which gcloud 2>&1 >/dev/null
[[ $? -ne 0 ]] && echo "cannot find gcloud!" && exit 1
kubectl_component_installed=$(gcloud components list 2>/dev/null | grep kubectl | grep -cv 'Not Installed')
[[ "$kubectl_component_installed" != "1" ]] && echo "the gcloud \"kubectl\" component does not look to be installed!" && exit 1
echo "OK"
echo -n "Checking kubectl..."
which kubectl 2>&1 >/dev/null
[[ $? -ne 0 ]] && echo "Cannot find kubectl!" && exit 1
echo "OK"

project_id="$(gcloud config get-value project)"
cluster_name="$1"

read -p "Using GCP project=${project_id}, cluster=${cluster_name}. Press Ctrl-C to abort, Enter to continue: " ignore
gcloud container clusters create "${cluster_name}" --release-channel regular 
gcloud container clusters get-credentials "${cluster_name}"

# An expiry will not be written by the `get-credentials` call which leads to confusing errors; running any `kubectl` command will
# populate it.
kubectl get pods 2>&1 >/dev/null
[[ $? -ne 0 ]] && echo "Error calling kubectl, check other messages" && exit 1

echo "Setup complete, your cluster should be ready to use"

