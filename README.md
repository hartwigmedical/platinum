# Platinum

Platinum is a tool to run the HMF pipeline for any number of samples in one easy command. 

### Before you begin

Platinum runs on the Google Cloud Platform. We've tried to automate as much of the setup as possible, but there are still
one or two things to configure.

To start you'll need:
- [A GCP project](https://cloud.google.com/resource-manager/docs/creating-managing-projects)
- An account within that project with the [Project Owner role](https://cloud.google.com/iam/docs/understanding-roles)
- [A region](https://cloud.google.com/compute/docs/regions-zones) where you plan to store your data and run your workload (hint: pick the region closest to where your data currently resides)

__IMPORTANT__
There is one [GCP Quota](https://cloud.google.com/compute/quotas) which requires resizing to run even a single pipeline, the `Persistent Disk SSD` this should be raised to `2TB`.
Follow the process described [here](https://cloud.google.com/compute/quotas#requesting_additional_quota).

You'll also need a machine to checkout this repo and run platinum. Platinum requires the follwing to be installed: 
* [Java 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html).
* [gcloud SDK](https://cloud.google.com/sdk/docs/downloads-interactive)
* [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)

### Quickstart

Run the following from the root of this repo:

```shell script
# PROJECT is your GCP Project
# REGION is your closest GCP region
# EXPERIMENT_NAME is some unique name for your experiment run 
# input.json is an example input pointing at test data HMF has exposed for this demo
./platinum configure -p PROJECT -r REGION
./platinum run -n EXPERIMENT_NAME -p PROJECT -r REGION -i examples/quickstart/input.json
./platinum status
# Keep checking this until you see the pod is complete
gsutil ls gs://platinum-output-EXPERIMENT_NAME
```

### Configuring your GCP Project

There are a couple things requiring a one time configuration in your project:
- Enabling private access such that your VMs will not be exposed to the public internet
- Enabling the compute and kubernetes apis

Checkout this repository on your local machine and run the following from the repo root:

```shell script
./platinum configure -p your_project -r your_region 
```  

### Configuring Input

The HMF pipeline takes pair-end fastq as input. This input should be uploaded to a bucket in [Google Cloud Storage](https://cloud.google.com/storage) (GCS) before running platinum. 
Once the input fastq is in GCS you define a JSON configuration in the following format.

Notes:
- Reference in this context is our internal terminology for the blood or normal sample.
- The first part of the path is the bucket, with no `gs://` prefix.


```json
{
  "samples": {
    "SAMPLE_1": {
      "tumor": {
        "type": "TUMOR",
        "name": "SAMPLE_1_TUMOR",
        "lanes": [
          {
            "laneNumber": "1",
            "firstOfPairPath": "path/to/your/tumor_r1.fastq.gz",
            "secondOfPairPath":  "path/to/your/tumor_r2.fastq.gz"
          }
        ]
      },
      "reference": {
        "type": "REFERENCE",
        "name": "SAMPLE_1_REFERENCE",
        "lanes": [
          {
            "laneNumber": "1",
            "firstOfPairPath": "path/to/your/reference_r1.fastq.gz",
            "secondOfPairPath": "path/to/your/reference_r2.fastq.gz"
          }
        ]
      }
    }
  }
}
```

### Running Platinum

Use the following command to run platinum:

```shell script
./platinum run -n EXPERIMENT_NAME -p PROJECT -r REGION -i examples/quickstart/input.json
```

Before doing anything you'll be prompted to login, twice 

This command will read your input json and create a platinum run in the project and region you've specified. EXPERIMENT_NAME 
should be a unique and meaningful name (no spaces or special chars) which will be used to identify all the cloud resources used
for your run. 

Platinum is asynchronous, you can keep eye on the progress use the following command:

```shell script
Pauls-MacBook-Pro:platinum pwolfe$ ./platinum status
NAME                                READY   STATUS    RESTARTS   AGE
cpct12345678-5qb2s   1/1     Running   0          172m
```

To check the logs of an individual pipeline use the platinum logs command.

```shell script
Pauls-MacBook-Pro:platinum pwolfe$ ./platinum logs cpct12345678-5qb2s
2020-08-20 18:00:10 INFO - Version of pipeline5 is [5.14.1742] 
...
```

Once the run is complete, all results will end up in a bucket in your project, named of the format `gs://platinum-output-EXPERIMENT_NAME`.




