# Platinum

Platinum is a tool to run the HMF pipeline from any GCP project for any number of samples in one easy command. 

Internally platinum runs the Pipeline5 docker container on an ephemeral Kubernetes cluster.

# Quickstart

```shell script
# PROJECT is your GCP Project
# REGION is your closest GCP region
# EXPERIMENT_NAME is some unique name for your experiment run 
# input.json is an example input pointing at test data HMF has exposed for this demo
./platinum configure -p PROJECT -r REGION
./platinum run -n EXPERIMENT_NAME -p PROJECT -r REGION -i examples/quickstart/input.json
gsutil ls gs://platinum-output-EXPERIMENT_NAME/
```

# Pre-Requisites

Platinum runs on the Google Cloud Platform. We've tried to automate as much of the setup as possible, but there are still
one or two things to configure.

To start you'll need:
- [A GCP project](https://cloud.google.com/resource-manager/docs/creating-managing-projects)
- An account within that project with the [Project Owner role](https://cloud.google.com/iam/docs/understanding-roles)
- [A region](https://cloud.google.com/compute/docs/regions-zones) where you plan to store your data and run your workload (hint: pick the region closest to where your data currently resides)

# Configuring your GCP Project

When you have your project setup, install the [gcloud SDK](https://cloud.google.com/sdk/docs/downloads-interactive).

There are a couple things requiring a one time configuration in your project:
- Enabling private access such that your VMs will not be exposed to the public internet
- Enabling the compute and kubernetes apis

Checkout this repository on your local machine and run the following from the repo root:

```shell script
./platinum configure -p your_project -r your_region 
```  

# Configuring Input

The HMF pipeline takes pair-end fastq as input. This input should be uploaded to a bucket in GCP before running platinum. When
That is complete
