# Platinum

Platinum is a tool to run the [HMF cancer analysis pipeline](https://github.com/hartwigmedical/pipeline5) for any number of tumor samples in one easy command.

The HMF cancer analysis pipeline is a comprehensive pipeline specifically designed for analysing WGS tumor data with the following properties and constraints:
 - FASTQ is assumed to be available for the sample(s) and is the starting point for the pipeline. 
 - Any number of reference samples can be provided to be used as a reference for the tumor DNA.
    - Zero reference samples is a special case of "tumor-only" mode. 
 - Both HG19 and HG38 reference genomes are supported throughout the pipeline and lead to comparable analysis quality.  
  
The HMF pipeline primarily uses algorithms developed by HMF and for which the code is open-sourced and available via [hmftools](https://github.com/hartwigmedical/hmftools).
In addition to the HMF algorithms, Platinum depends on a number of resources (databases) and some external tools and algorithms.

The pipeline images available through Platinum depend only on free-to-use and open-source resources such that Platinum comes with no restrictions for end-users.
The following tables describe which external resources are used by Platinum and hence by any user of Platinum, along with a link to their license. 

### Resources used by Platinum
Resource  | Purpose | Disclaimer
:-:|:-:|:-:
[<img src="logos/grc.png" title="Genome Reference Consortium" height=50>](https://www.ncbi.nlm.nih.gov/grc)  | TODO | N/A
[<img src="logos/ensembl.png" title="Ensembl" height=50>](http://www.ensembl.org)  | The ensembl database is used extensively throughout our algorithms: 
    <br/> - The exome is roughly defined as the exons of all canonical transcripts of all genes as defined by ensembl 
    <br/> - Protein features and splice data used for interpretation of structural variants is gathered via ensembl
    | [link](http://www.ensembl.org/info/about/legal/disclaimer.html)
[<img src="logos/civic.png" title="Clinical Interpretations of Variants in Cancer" height=50>](https://civicdb.org)  | TODO | [link](https://civic.readthedocs.io/en/latest/about.html)
[<img src="logos/docm.png" title="Database of Curated Mutations" height=50>](http://www.docm.info)  | TODO | [link](http://www.docm.info/about)
[<img src="logos/cgi.png" title="Cancer Genome Interpreter" height=50>](https://www.cancergenomeinterpreter.org)  | TODO | [link](https://www.cancergenomeinterpreter.org/about)
[<img src="logos/clinvar.png" title="ClinVar" height=50>](https://www.ncbi.nlm.nih.gov/clinvar/)| TODO | [link](https://www.ncbi.nlm.nih.gov/clinvar/intro/)
[<img src="logos/virushostdb.png" title="Virus Host DB" height=50>](https://www.genome.jp/virushostdb)  | TODO | [link](https://www.genome.jp/virushostdb/note.html)
[<img src="logos/repeatmasker.png" title="Repeat Masker" height=50>](http://www.repeatmasker.org) | TODO | N/A
[<img src="logos/ucsc.png" title="UCSC" height=50>](https://genome.ucsc.edu)| TODO | [link](https://genome.ucsc.edu/conditions.html)
[<img src="logos/giab.png" title="Genome in a Bottle" height=50>](https://www.nist.gov/programs-projects/genome-bottle) | TODO | N/A
[<img src="logos/snpeff.png" title="SnpEFF" height=50>](http://snpeff.sourceforge.net/) | TODO | [link](https://pcingola.github.io/SnpEff/SnpEff.html#license)
[<img src="logos/encode.png" title="ENCODE" height=50>](https://www.encodeproject.org) | TODO | [link](https://www.encodeproject.org/help/citing-encode/)
 
 


## Before you begin

Platinum runs on the Google Cloud Platform. We've tried to automate as much of the setup as possible, but there are still
one or two things to configure.

To start you'll need:
- A GCP account. You can get started with the credit they offer and a
  credit card (for verification). See Google's [docs](https://cloud.google.com/free/docs/gcp-free-tier).
- [A GCP project](https://cloud.google.com/resource-manager/docs/creating-managing-projects) 
  and a user within that project with the [Owner role](https://cloud.google.com/iam/docs/understanding-roles).
- [A region](https://cloud.google.com/compute/docs/regions-zones) where you plan to store your data and run your workload 
  (hint: pick the region closest to where your data currently resides)

You'll also need a machine to checkout this repo and run Platinum with
these installed:
* [Java 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html).
* [gcloud SDK](https://cloud.google.com/sdk/docs/downloads-interactive)
  (configured to access your new project)
* [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)

### Quickstart

Run the following from the root of this repo where `examples/quickstart/input.json` is your input
file (make sure to adjust and uncomment the `export` lines):

```shell script
export PROJECT=$(gcloud projects list | grep 'your project name from above' | awk '{print $1}') 
export REGION='your region'
# Experiment name is just a unique id we'll use to name resources. Call it anything for now.
export EXPERIMENT_NAME='experiment_name'
./platinum configure -p $PROJECT -r $REGION
./platinum login
./platinum run -n $EXPERIMENT_NAME -p $PROJECT -r $REGION -i examples/quickstart/input.json
./platinum status
# Keep checking this until you see the pod is complete. Then cleanup
./platinum cleanup -n $EXPERIMENT_NAME -p $PROJECT -r $REGION
# Results are waiting in Google Cloud Storage
gsutil ls gs://platinum-output-$EXPERIMENT_NAME
```

### Configuring your GCP Project

There are a couple things requiring a one time configuration in your project:
- Enabling private access such that your VMs will not be exposed to the public internet
- Enabling the compute and kubernetes apis

Checkout this repository on your local machine and run the following from the repo root:

```shell script
./platinum configure -p your_project -r your_region 
```  

You only need to run this once for a project and region where you want to run platinum.

### Logging In

You must "login" to GCP locally to configure the credentials platinum needs to work with GCP. 

```shell script
./platinum login
``` 

This command performs two logins, once as a user and another time as the application default. This ensures all 
subsequent operations will use the correct credentials. 

You should run this command at least once, and whenever you use different credentials to interact with GCP.

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

Make sure you clean up when the run is complete, as you now have a small Kubernetes cluster:

```shell script
./platinum cleanup -n EXPERIMENT_NAME -p PROJECT -r REGION
```



