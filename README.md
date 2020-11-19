# Platinum

- [About Platinum](#about-platinum)
  * [Introduction](#introduction)
  * [Resources used by Platinum](#resources-used-by-platinum)
  * [Cost and Performance](#cost-and-performance)
- [Running Platinum](#running-platinum)
  * [Before You Begin](#before-you-begin)
  * [Quickstart](#quickstart)
  * [Configuring your GCP Project](#configuring-your-gcp-project)
  * [Logging In](#logging-in)
  * [Configuring Input](#configuring-input)
  * [Running Pipelines](#running-pipelines)
  * [Scaling Up](#scaling-up)

## About Platinum

### Introduction

Platinum is a tool to run the [HMF cancer analysis pipeline](https://github.com/hartwigmedical/pipeline5) for any number of tumor samples in one easy command.

The HMF cancer analysis pipeline is a comprehensive pipeline specifically designed for analysing WGS tumor data with the following properties and constraints:
 - FASTQ is assumed to be available for the sample(s) and is the starting point for the pipeline. 
 - The pipeline assumes that the tumor DNA sample is analysed against a single reference DNA sample.
    - Do note that some individual algorithms support zero and/or multiple reference samples to allow "tumor-only" mode. These modes are not supported yet throughout the entire platinum process though.
 - Both HG19 and HG38 reference genomes are supported throughout the pipeline and lead to comparable analysis quality.  
  
The HMF pipeline primarily uses algorithms developed by HMF open-source and available via [hmftools](https://github.com/hartwigmedical/hmftools).
In addition to the HMF algorithms, Platinum depends on a number of resources (databases) and some external tools and algorithms.

The pipeline images available through Platinum depend only on free-to-use and open-source resources such that Platinum comes with no restrictions for end-users.
The following tables describe which external resources are used by Platinum and hence by any user of Platinum, along with a link to their disclaimer/notes and/or publication when available. 

### Resources used by Platinum
Resource | Purpose | References
:-:|---|:-:
[<img src="logos/grc.png" title="Genome Reference Consortium" height=100>](https://www.ncbi.nlm.nih.gov/grc)  | GRC makes available the human reference genome (HG19 or HG38) used in nearly every step of the pipeline. | N/A
[<img src="logos/ensembl.png" title="Ensembl">](http://www.ensembl.org)  | The ensembl database is used extensively throughout our algorithms as the source for all gene and transcript annotations. | [disclaimer](http://www.ensembl.org/info/about/legal/disclaimer.html)
[<img src="logos/civic.png" title="Clinical Interpretations of Variants in Cancer">](https://civicdb.org)  | CIViC is a knowledgebase containing (pathogenic) mutations and linking them to treatments. In terms of the pipeline, CIViC's list of pathogenic variants contribute to the hotspot list used as our most sensitive calling tier and used for downstream interpretation of drivers. | - [about](https://civic.readthedocs.io/en/latest/about.html) <br /> - [reference](https://www.nature.com/articles/ng.3774)
[<img src="logos/docm.png" title="Database of Curated Mutations">](http://www.docm.info)  | DoCM is a knowledgebase containing (pathogenic) mutations. In terms of the pipeline, DoCM's list of pathogenic variants contribute to the hotspot list used as our most sensitive calling tier and used for downstream interpretation of drivers. | - [about](http://www.docm.info/about) <br /> - [reference](http://www.nature.com/nmeth/journal/v13/n10/full/nmeth.4000.html)
[<img src="logos/cgi.png" title="Cancer Genome Interpreter">](https://www.cancergenomeinterpreter.org)  | CGI is a knowledgebase containing (pathogenic) mutations and linking them to treatments. In terms of the pipeline, CGI's list of pathogenic variants contribute to the hotspot list used as our most sensitive calling tier and used for downstream interpretation of drivers. | - [about](https://www.cancergenomeinterpreter.org/about) <br /> - [reference](https://doi.org/10.1101/140475)
[<img src="logos/clinvar.png" title="ClinVar" height=150>](https://www.ncbi.nlm.nih.gov/clinvar/)| ClinVar contains evidence on pathogenicity of variants and is used for determining the pathogenicity of germline variants (in case a reference sample is supplied). | [intro](https://www.ncbi.nlm.nih.gov/clinvar/intro/)
[<img src="logos/virushostdb.png" title="Virus Host DB">](https://www.genome.jp/virushostdb)  | The pipeline searches for evidence of viral integrations in the tumor DNA. The database from Virus Host DB is used as the source for which viruses to look for. | - [about](https://www.genome.jp/virushostdb/note.html) <br /> - [reference](https://pubmed.ncbi.nlm.nih.gov/26938550)
[<img src="logos/repeatmasker.png" title="Repeat Masker">](http://www.repeatmasker.org) | The repeat masker database is largely derived from the GRC reference genome. This is used to interpret single end breaks in the DNA by mapping the other side of a single end break against a number of repeat masks.   | N/A
[<img src="logos/giab.png" title="Genome in a Bottle">](https://www.nist.gov/programs-projects/genome-bottle) | The GIAB consortium's NA12878 high confidence regions are used by the pipeline. Thresholds are lowered when calling variants in a high confidence region vs a low confidence region. | N/A
[<img src="logos/snpeff.png" title="SnpEff">](http://snpeff.sourceforge.net/) | SnpEff maintains a database largely derived from ensembl and GRC, which the pipeline uses to annotate variants in terms of coding impact. | [license](https://pcingola.github.io/SnpEff/SnpEff.html#license)
[<img src="logos/encode.png" title="ENCODE">](https://www.encodeproject.org) | ENCODE database is used for blacklisting regions for structural variant calling. Hela replication timing is also used to annotate the replication timing of structural variant breakends| - [about](https://www.encodeproject.org/help/citing-encode/) <br /> - [reference](https://pubmed.ncbi.nlm.nih.gov/22955616)
 
### Cost and Performance

Different inputs can lead to variation in cost and runtime, but to give some indication of what to expect, we have benchmarked Platinum against COLO829:
* Reference DNA 30x depth and 4 lanes
* Tumor DNA 100x depth and 4 lanes
* The following minimum quotas (see [Scaling Up](#scaling-up) for more info on Quotas)

Quota | Value |
----- | ------ |
CPU   | 768
CPU_ALL_REGIONS | 768 |
PREEMPTIBLE_LOCAL_SSD_TOTAL_GB | 9TB |
PERSISTENT_DISK_SSD_GB | 1TB |

With these settings we get a cost of approximately €20 and runtime of 15 hours.

When evaluating your own performance, a few things to keep in mind:
- We map every FASTQ lane to the reference genome in parallel, so consolidating into less lanes (for instance, after converting back from BAM) will increase runtime.
- We use [pre-emptible VMs](https://cloud.google.com/compute/docs/instances/preemptible) to save cost. These can be pre-empted (stopped and reclaimed) by Google, adding to the total runtime. 
The pipeline will handle pre-emptions and its well worth it for the cost impact. 
- New projects and GCP accounts are constrained by small quotas. You can request to [raise them through the console](https://cloud.google.com/compute/quotas).
 
## Running Platinum

### Before You Begin

Platinum runs on the Google Cloud Platform. To start you'll need:
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
file (make sure to adjust the `export` lines):

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

The HMF pipeline takes paired-end FASTQ as input. This input should be uploaded to a bucket in [Google Cloud Storage](https://cloud.google.com/storage) (GCS) before running platinum. 
Once the input FASTQ is in GCS you define a JSON configuration in the following format.

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

### Running with HG38 Reference Genome

Platinum defaults to using the HG19 reference genome. To use HG38 instead, include these lines at the top of your input file,
above the `samples` object:

```json
"argumentOverrides": {
  "ref_genome_version": "HG38",
},
"samples": {
    ...
```

### Running Pipelines

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

### Scaling Up

Using GCP infrastructure, Platinum can run all your pipelines in parallel, giving you the same total runtime with 1000 samples as a single sample. 
That said, to take advantage your GCP project must have been granted enough quota to support your workload. Here we review the quota limits
frequently reached by Platinum and appropriate values to request from google.  

First, please review GCP's documentation on [Raising Quotas](https://cloud.google.com/compute/quotas) and the request process. 

An overview of the key quota limits are defined below. All the peaks occur during the alignment process, which uses many VMs with large core counts.
In our 4 ref + 4 tumor lane benchmark, the peak lasts approx. 45 minutes.  

Quota | Peak | Description |
--- | --- | --- |
CPU | 96 x # of lanes | Each lane is aligned individually on a 96 core VM. While we use preemptible VMs, CPU count in the selected region is still contrained by this quota.|
CPU_ALL_REGIONS |  96 x # of lanes | This quota is another limit on CPUs, but also includes any CPUs used in other regions |
PREEMPTIBLE_LOCAL_SSD_TOTAL_GB | 1.125 TB  x # of lanes  | Local SSDs can be attaches to a VM in 375Gb increments. Attaching 3 local SSDs to each VM ensures we have enough space for the input, output and temporary files involved in alignment and somatic calling. |
PERSISTENT_DISK_SSD_GB | 200GB  x # of lanes  | Used for the O/S of each VM, along with HMF resources and tools |

Getting large quota increases can be difficult if you have a new GCP account without a billing track record. Also, quotas are generally allocated for sustained use, 
and not the bursty requirements of running a large pipeline. You may need to contact Google in order to explain your requirements. If you are having trouble getting
the quotas you need for a large experiment, please reach out to us and we can help put you in touch with the right people. 
