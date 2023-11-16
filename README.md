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
    * [Re-running Platinum](#re-running-platinum)

## About Platinum

### Introduction

Platinum is a tool to run the [HMF cancer analysis pipeline](https://github.com/hartwigmedical/pipeline5) for any number of tumor samples in
one easy command.

The HMF cancer analysis pipeline is a comprehensive pipeline specifically designed for analysing WGS tumor data with the following
properties and constraints:

- FASTQ is assumed to be available for the sample(s) and is the starting point for the pipeline.
- The pipeline assumes that the tumor DNA sample is analysed against a single reference DNA sample.
    - Do note that some individual algorithms support zero and/or multiple reference samples to allow "tumor-only" mode. These modes are not
      supported yet throughout the entire platinum process though.
- Both GRCh37 and GRCh38 reference genomes are supported throughout the pipeline and lead to comparable analysis quality.

The HMF pipeline primarily uses algorithms developed by HMF open-source and available
via [hmftools](https://github.com/hartwigmedical/hmftools).
In addition to the HMF algorithms, Platinum depends on a number of resources (databases) and some external tools and algorithms.

#### Disclaimer

- Platinum aims to be based exclusively on open source code and databases that are free from any restrictions. However, the databases that
  are distributed as part of Platinum are not owned by HMF and their licenses could change. Below table lists all institutions from which we
  distribute data along with a link to their disclaimer and/or about and/or publication.
- HMF offers Platinum and the HMF cancer analysis pipeline on an ‘as-is’ basis.
- HMF assumes no liability whatsoever for general, special, incidental, consequential or any other type of damages arising out of the use or
  inability to use Platinum or a failure of Platinum to operate with any other programs. In as far as not yet sufficiently stipulated above,
  HMF expressly assumes no liability or responsibility whatsoever for the quality of the data that is being used in running Platinum or for
  the final configuration of the GCP project used by Platinum including anything related to security and encryption of data and any damages
  that may arise as a result thereof.
- HMF furthermore is not responsible for and assumes no liability whatsoever for damages resulting from the interpretation of the output of
  the HMF cancer analysis pipeline and the medical and/or scientific conclusions that are drawn on the basis of such interpretation.

### Resources used by Platinum

|                                                   Resource                                                    | Purpose                                                                                                                                                                                                                                                                            |                                                          References                                                          |
|:-------------------------------------------------------------------------------------------------------------:|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------------:|
| [<img src="logos/grc.png" title="Genome Reference Consortium" height=100>](https://www.ncbi.nlm.nih.gov/grc)  | GRC makes available the human reference genome (GRCh37 or GRCh38) used in nearly every step of the pipeline.                                                                                                                                                                       |                                                             N/A                                                              |
|                    [<img src="logos/ensembl.png" title="Ensembl">](http://www.ensembl.org)                    | The ensembl database is used extensively throughout our algorithms as the source for all gene and transcript annotations.                                                                                                                                                          |                            [disclaimer](http://www.ensembl.org/info/about/legal/disclaimer.html)                             |
|   [<img src="logos/civic.png" title="Clinical Interpretations of Variants in Cancer">](https://civicdb.org)   | CIViC is a knowledgebase containing (pathogenic) mutations and linking them to treatments. In terms of the pipeline, CIViC's list of pathogenic variants contribute to the hotspot list used as our most sensitive calling tier and used for downstream interpretation of drivers. |  - [about](https://civic.readthedocs.io/en/latest/about.html) <br /> - [reference](https://www.nature.com/articles/ng.3774)  |
|           [<img src="logos/docm.png" title="Database of Curated Mutations">](http://www.docm.info)            | DoCM is a knowledgebase containing (pathogenic) mutations. In terms of the pipeline, DoCM's list of pathogenic variants contribute to the hotspot list used as our most sensitive calling tier and used for downstream interpretation of drivers.                                  | - [about](http://www.docm.info/about) <br /> - [reference](http://www.nature.com/nmeth/journal/v13/n10/full/nmeth.4000.html) |
|    [<img src="logos/cgi.png" title="Cancer Genome Interpreter">](https://www.cancergenomeinterpreter.org)     | CGI is a knowledgebase containing (pathogenic) mutations and linking them to treatments. In terms of the pipeline, CGI's list of pathogenic variants contribute to the hotspot list used as our most sensitive calling tier and used for downstream interpretation of drivers.     |        - [about](https://www.cancergenomeinterpreter.org/about) <br /> - [reference](https://doi.org/10.1101/140475)         |
|            [<img src="logos/ncbi.png" title="NCBI">](https://www.ncbi.nlm.nih.gov/genome/viruses)             | The NCBI viral reference database is used by the pipeline when it looks for viral presence and its integration into the analysed tumor DNA                                                                                                                                         |      - [about](https://www.ncbi.nlm.nih.gov/home/about) <br /> - [reference](https://pubmed.ncbi.nlm.nih.gov/25428358)       |
|       [<img src="logos/clinvar.png" title="ClinVar" height=150>](https://www.ncbi.nlm.nih.gov/clinvar/)       | ClinVar contains evidence on pathogenicity of variants and is used for determining the pathogenicity of germline variants (in case a reference sample is supplied).                                                                                                                |                                     [intro](https://www.ncbi.nlm.nih.gov/clinvar/intro/)                                     |
|            [<img src="logos/repeatmasker.png" title="Repeat Masker">](http://www.repeatmasker.org)            | The repeat masker database is largely derived from the GRC reference genome. This is used to interpret single end breaks in the DNA by mapping the other side of a single end break against a number of repeat masks.                                                              |                                                             N/A                                                              |
| [<img src="logos/giab.png" title="Genome in a Bottle">](https://www.nist.gov/programs-projects/genome-bottle) | The GIAB consortium's NA12878 high confidence regions are used by the pipeline. Thresholds are lowered when calling variants in a high confidence region vs a low confidence region.                                                                                               |                                                             N/A                                                              |
|                  [<img src="logos/pharmgkb.png" title="PharmGKB">](https://www.pharmgkb.org)                  | PharmGKB maintains a database for pharmacogenetic evidence and is used to annotate potentially relevant haplotypes found by the pipeline.                                                                                                                                          |  - [license](https://www.pharmgkb.org/page/dataUsagePolicy) <br /> - [reference](https://pubmed.ncbi.nlm.nih.gov/22992668)   |
|                  [<img src="logos/ipd.png" title="IPD">](https://www.ebi.ac.uk/ipd/imgt/hla)                  | The IPD-IMGT/HLA database is used as the reference source for HLA types previously found in humans                                                                                                                                                                                 |     - [about](https://www.ebi.ac.uk/about) <br /> - [reference](https://academic.oup.com/nar/article/48/D1/D948/5610347)     |
|                 [<img src="logos/snpeff.png" title="SnpEff">](http://snpeff.sourceforge.net/)                 | SnpEff maintains a database largely derived from ensembl and GRC, which the pipeline uses to annotate variants in terms of coding impact.                                                                                                                                          |                               [license](https://pcingola.github.io/SnpEff/SnpEff.html#license)                               |
|                 [<img src="logos/encode.png" title="ENCODE">](https://www.encodeproject.org)                  | ENCODE database is used for blacklisting regions for structural variant calling. Hela replication timing is also used to annotate the replication timing of structural variant breakends                                                                                           | - [about](https://www.encodeproject.org/help/citing-encode/) <br /> - [reference](https://pubmed.ncbi.nlm.nih.gov/22955616)  |

### Cost and Performance

Different inputs can lead to variation in cost and runtime, but to give some indication of what to expect, we have benchmarked Platinum
against COLO829:

* Reference DNA 30x depth and 4 lanes
* Tumor DNA 100x depth and 4 lanes
* The following minimum quotas (see [Scaling Up](#scaling-up) for more info on Quotas)

| Quota                          | Value |
|--------------------------------|-------|
| CPU                            | 768   |
| CPU_ALL_REGIONS                | 768   |
| PREEMPTIBLE_LOCAL_SSD_TOTAL_GB | 9TB   |
| PERSISTENT_DISK_SSD_GB         | 1TB   |

With these settings we get a cost of approximately €20 and runtime of 15 hours.

When evaluating your own performance, a few things to keep in mind:

- We map every FASTQ lane to the reference genome in parallel, so consolidating into less lanes (for instance, after converting back from
  BAM) will increase runtime.
- We use [pre-emptible VMs](https://cloud.google.com/compute/docs/instances/preemptible) to save cost. These can be pre-empted (stopped and
  reclaimed) by Google, adding to the total runtime.
  The pipeline will handle pre-emptions and its well worth it for the cost impact.
- New projects and GCP accounts are constrained by small quotas. You can request
  to [raise them through the console](https://cloud.google.com/compute/quotas).

## Running Platinum

### Before You Begin

Platinum runs on the Google Cloud Platform. To start you'll need:

- A GCP account. You can get started with the credit they offer and a  credit card (for verification). See Google's 
  [docs](https://cloud.google.com/free/docs/gcp-free-tier).
- [A GCP project](https://cloud.google.com/resource-manager/docs/creating-managing-projects) and a user within that project with the
  [Owner role](https://cloud.google.com/iam/docs/understanding-roles).
- [A region](https://cloud.google.com/compute/docs/regions-zones) where you plan to store your data and run your workload (hint: pick the 
  region closest to where your data currently resides)

You'll also need a machine to checkout this repository and run Platinum. You should have the following installed and some basic familiarity
with how to use them:

* [git](https://git-scm.com/).
* [Java 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html).
* [gcloud SDK](https://cloud.google.com/sdk/docs/downloads-interactive)
  (configured to access your new project) and the connector module which you can install with `gcloud components install kubectl`.
* [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)

### Quickstart

The basic user-facing component of Platinum is a shell script (`platinum`) that attempts to simplify interaction with the cluster and the
Platinum software itself. In the simplest cases the following should help you get your job running.

Run the following from the root of this repo where `examples/quickstart/colomini.json` is your input file (make sure to adjust the
`export` lines):

```shell script
export PROJECT=$(gcloud projects list | grep 'your project name from above' | awk '{print $1}') 
export REGION='your region'
# Experiment name is just a unique id we'll use to name resources. Call it anything for now.
export EXPERIMENT_NAME='experiment_name'
./platinum configure -p $PROJECT -r $REGION
./platinum login
./platinum run -n $EXPERIMENT_NAME -p $PROJECT -r $REGION -i examples/quickstart/colomini.yaml
./platinum status
# Keep checking this until you see the pod is complete. Then cleanup
./platinum cleanup -n $EXPERIMENT_NAME -p $PROJECT -r $REGION
# Results are waiting in Google Cloud Storage
gsutil ls gs://platinum-output-$EXPERIMENT_NAME
```

See below for [advanced usage](#advanced-usage).

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

The HMF pipeline takes paired-end FASTQ as input. This input should be uploaded to a bucket
in [Google Cloud Storage](https://cloud.google.com/storage) (GCS) before running platinum.
Once the input FASTQ is in GCS you define a YAML or JSON configuration in the following format.

In the example below we have one sample. A sample in this context is close to synonymous with a patient or donor, and is a grouping of tumor
sequencing data with blood/normal sequencing data.

Each sample can have multiple tumors to a single normal. Note: when platinum runs it will actually run a pipeline for each pair. So in this
example, 2 pipeline will run.

Each FASTQ should be a two paths, one to each end of the pair.

```yaml
samples:
  - name: SAMPLE_NAME
    tumors:
      - name: TUMOR1
        fastq:
          - read1: "gs://hmf-public/fastq/COLO829Mini/COLO829v003T_AHHKYHDSXX_S12_L001_R1_001.fastq.gz"
            read2: "gs://hmf-public/fastq/COLO829Mini/COLO829v003T_AHHKYHDSXX_S12_L001_R2_001.fastq.gz"
          - read1: "gs://hmf-public/fastq/COLO829Mini/COLO829v003T_AHHKYHDSXX_S12_L002_R1_001.fastq.gz"
            read2: "gs://hmf-public/fastq/COLO829Mini/COLO829v003T_AHHKYHDSXX_S12_L002_R2_001.fastq.gz"
          - read1: "gs://hmf-public/fastq/COLO829Mini/COLO829v003T_AHHKYHDSXX_S12_L003_R1_001.fastq.gz"
            read2: "gs://hmf-public/fastq/COLO829Mini/COLO829v003T_AHHKYHDSXX_S12_L003_R2_001.fastq.gz"
          - read1: "gs://hmf-public/fastq/COLO829Mini/COLO829v003T_AHHKYHDSXX_S12_L004_R1_001.fastq.gz"
            read2: "gs://hmf-public/fastq/COLO829Mini/COLO829v003T_AHHKYHDSXX_S12_L004_R2_001.fastq.gz"
      - name: TUMOR2 #Optional
        fastq:
          - read1: "gs://hmf-public/fastq/COLO829Mini/COLO829v003T_AHHKYHDSXX_S12_L001_R1_001.fastq.gz"
            read2: "gs://hmf-public/fastq/COLO829Mini/COLO829v003T_AHHKYHDSXX_S12_L001_R2_001.fastq.gz"
          - read1: "gs://hmf-public/fastq/COLO829Mini/COLO829v003T_AHHKYHDSXX_S12_L002_R1_001.fastq.gz"
            read2: "gs://hmf-public/fastq/COLO829Mini/COLO829v003T_AHHKYHDSXX_S12_L002_R2_001.fastq.gz"
          - read1: "gs://hmf-public/fastq/COLO829Mini/COLO829v003T_AHHKYHDSXX_S12_L003_R1_001.fastq.gz"
            read2: "gs://hmf-public/fastq/COLO829Mini/COLO829v003T_AHHKYHDSXX_S12_L003_R2_001.fastq.gz"
          - read1: "gs://hmf-public/fastq/COLO829Mini/COLO829v003T_AHHKYHDSXX_S12_L004_R1_001.fastq.gz"
            read2: "gs://hmf-public/fastq/COLO829Mini/COLO829v003T_AHHKYHDSXX_S12_L004_R2_001.fastq.gz"
    normal:
      name: NORMAL
      fastq:
        - read1: "gs://hmf-public/fastq/COLO829Mini/COLO829v003R_AHHKYHDSXX_S13_L001_R1_001.fastq.gz"
          read2: "gs://hmf-public/fastq/COLO829Mini/COLO829v003R_AHHKYHDSXX_S13_L001_R2_001.fastq.gz"
        - read1: "gs://hmf-public/fastq/COLO829Mini/COLO829v003R_AHHKYHDSXX_S13_L002_R1_001.fastq.gz"
          read2: "gs://hmf-public/fastq/COLO829Mini/COLO829v003R_AHHKYHDSXX_S13_L002_R2_001.fastq.gz"
        - read1: "gs://hmf-public/fastq/COLO829Mini/COLO829v003R_AHHKYHDSXX_S13_L003_R1_001.fastq.gz"
          read2: "gs://hmf-public/fastq/COLO829Mini/COLO829v003R_AHHKYHDSXX_S13_L003_R2_001.fastq.gz"
        - read1: "gs://hmf-public/fastq/COLO829Mini/COLO829v003R_AHHKYHDSXX_S13_L004_R1_001.fastq.gz"
          read2: "gs://hmf-public/fastq/COLO829Mini/COLO829v003R_AHHKYHDSXX_S13_L004_R2_001.fastq.gz"
```

### Reference Genomes

Platinum can be run with either a 37 or 38 reference genome release. The default is 38, but to use 37 instead, include these lines at the
top of your input file, above the samples object:

```yaml
argumentOverrides:
  ref_genome_version: "37"
```

Internally Platinum uses the GRCh37 assembly `Homo_sapiens.GRCh37.GATK.illumina.fasta`, and the GRCh38 no-alt
assembly `GCA_000001405.15_GRCh38_no_alt_analysis_set.fna`.

There is no support for use of other assemblies or versions.

## Advanced Usage

Many use cases will be fine interacting with just the `platinum` script but its limitations start to show in some scenarios:

* The `status` subcommand is quite naive and just queries for jobs using the active Kubernetes cluster configuration. If you are interacting
  with multiple clusters and switching between them, or if your cluster is a multi-tenant arrangement you'll find the output is not really
  that useful. In that case you can use the `kubectl` command directly to isolate your jobs more effectively.
* When using `update`, in the background the script just calls out to `git` and attempts to pull the latest changes overtop what you have
  locally. If you've made any modifications this will be immediately obvious. Also there are multiple branches containing Platinum versions
  that are compatible with different underlying Pipeline5 releases. To run with different `pipeline5` versions you may have to switch to a
  different branch, which requires basic knowledge of `git`. This approach allows us to keep shipping updates without worrying about keeping
  compatability with old versions forever, while also not marooning users without a working Platinum.

### Really-Quick Kubernetes Refresher

For the purposes of Platinum:

* `kubectl` is used to interface with Kubernetes from the command line
* Platinum submits "jobs" to the Kubernetes cluster
* Each job will spawn a pod to run the associated pipeline, and if there is a failure successive pods to finish

Some useful commands:

* `kubectl get jobs`
* `kubectl get pods`
* `kubectl logs (pod name)`

Some more advanced usages are detailed below.

### Running with an existing cluster

Platinum uses ephemeral infrastructure to ease any maintenance burden, hide complexity and reduce resource contention. That said, you may
want to use existing shared infrastructure for your platinum runs.

To set this up you can pass platinum an existing service account name, cluster name and secret within that cluster which contains the
private key for the service account like so:

```yaml
serviceAccount:
  name: "your-service-account@your-service-account.iam.gserviceaccount.com",
  existingSecret: "your-secret",
  cluster: "your-cluster"
``` 

### Additional GCP Configuration

Platinum offers some additional configuration options to suit more complex GCP project setups. These extra settings are mainly geared for
setups requiring additional levels of security, in particular around the network. These settings are configured in the JSON by adding a
sections `gcp`

```yaml
gcp:
  project: "hmf-crunch",
  region: "europe-west4",
  network: "kubernetes",
  subnet: "kubernetes",
  networkTags:
    - "tag1"
  zones:
    - "europe-west4-a"
  privateCluster: true,
  secondaryRangeNamePods: "pods",
  secondaryRangeNameServices: "services",
  masterIpv4CidrBlock: "172.17.0.32/28"
```

| Parameter                                                                  | Description                                                                                                                                                                                                                               |
|----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| project                                                                    | Same as the `-p` CLI argument. If you specify it here you don't need to put on the command line                                                                                                                                           |
| region                                                                     | Same as the `-r` CLI argument. If you specify it here you don't need to put on the command line                                                                                                                                           |
| outputBucket / Override the output bucket location with an existing bucket |
| network                                                                    | A identifier to the VPC network to be used for all compute resources. If the network is in a different project from the run, you the "projects/network-project/global/networks/network-name" format                                       |
| subnet                                                                     | A identifier to the VPC network to be used for all compute resources. If the network is in a different project or region from the run, you the "projects/subnet-project/regions/subnet-region/subnetworks/subnet-name" format             |
| networkTags                                                                | Network tags to apply to all compute resources                                                                                                                                                                                            |
| zones                                                                      | A list of zones to use for kubernetes nodes to avoid capacity issues. The pipeline may run outside these zones, but will automatically select a zone with capacity.                                                                       |
| privateCluster                                                             | Makes the kubernetes cluster private, ie no nodes or master have public IP. Note that if this option is used, you will not be able to run platinum from a computer outside the VPC. You should create a VM within the VPC to run platinum |
| secondaryRangeNamePods                                                     | A secondary IP range for pods in the cluster. This setting is only required if you use a shared VPC network.                                                                                                                              |
| secondaryRangeNamePods                                                     | A secondary IP range for services in the cluster. This setting is only required if you use a shared VPC network.                                                                                                                          |
| masterIpv4CidrBlock                                                        | Passed to the master when private cluster is enabled. Will default to "172.16.0.32/28" so only required if you have multiple private clusters in the same VPC                                                                             |
| preemptibleCluster                                                         | Use pre-emptible nodes in the cluster to save cost. Default is true                                                                                                                                                                       |

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
./platinum status
NAME                                READY   STATUS    RESTARTS   AGE
cpct12345678-5qb2s   1/1     Running   0          172m
```

If your cluster is a shared one there may be pods from other jobs listed, in that case use `kubectl get pods | grep ...`.

To check the logs of an individual pipeline use the `platinum logs` command.

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

Using GCP infrastructure, Platinum can run all your pipelines in parallel, giving you the same total runtime with 1000 samples as a single
sample.
That said, to take advantage your GCP project must have been granted enough quota to support your workload. Here we review the quota limits
frequently reached by Platinum and appropriate values to request from google.

First, please review GCP's documentation on [Raising Quotas](https://cloud.google.com/compute/quotas) and the request process.

An overview of the key quota limits are defined below. All the peaks occur during the alignment process, which uses many VMs with large core
counts.
In our 4 ref + 4 tumor lane benchmark, the peak lasts approx. 45 minutes.

| Quota                          | Peak                   | Description                                                                                                                                                                                                 |
|--------------------------------|------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| CPU                            | 96 x # of lanes        | Each lane is aligned individually on a 96 core VM. While we use preemptible VMs, CPU count in the selected region is still contrained by this quota.                                                        |
| CPU_ALL_REGIONS                | 96 x # of lanes        | This quota is another limit on CPUs, but also includes any CPUs used in other regions                                                                                                                       |
| PREEMPTIBLE_LOCAL_SSD_TOTAL_GB | 1.125 TB  x # of lanes | Local SSDs can be attaches to a VM in 375Gb increments. Attaching 3 local SSDs to each VM ensures we have enough space for the input, output and temporary files involved in alignment and somatic calling. |
| PERSISTENT_DISK_SSD_GB         | 200GB  x # of lanes    | Used for the O/S of each VM, along with HMF resources and tools                                                                                                                                             |

Getting large quota increases can be difficult if you have a new GCP account without a billing track record. Also, quotas are
generally allocated for sustained use, and not the bursty requirements of running a large pipeline. You may need to contact Google
in order to explain your requirements. If you are having trouble getting the quotas you need for a large experiment, please reach
out to us and we can help put you in touch with the right people.

### Batching

When running large workloads you may find that Kubernetes cluster has a hard time keeping up, and you end up overwhelming it at
startup.  This would manifest itself in many "Pending" or "Evicted" jobs. Batching is available to alleviate this issue and there
are two forms:

* Time-based, which is configured with a size and a delay. It submits "size" number of jobs every "delay" minutes. It is still
  supported but has been deprecated.
* Constant-size, which only takes a size. It tries to keep the number of running jobs at the "size" number, but also has logic to
  rate-limit job submission to avoid overwhelming the cluster.

The constant-size scheduler is preferred. It works very will with modern autoscale-enabled clusters and has the pleasant advantage
of allowing runtime throughput management but tuning autoscale parameters while the job is already running. For large workloads it
is recommended to set a large batch size in Platinum and adjust actual cluster size after Platinum is running to achieve the desired
throughput.

```yaml
batch:
  size: 50
  delay: 10
```

The delay is in minutes, and if it is provided the time-based scheduler will be used, otherwise the constant-size scheduler is configured.
Time-based scheduling has been deprecated to be removed in a later release.

### Re-running Platinum

Platinum can also re-use the output of a complete run to facilitate running new version on old data. When re-run platinum will leave input
data in place, but replace existing data when new output is available. You may want to make a backup of your initial data before running
again.

To configure this add the following to your JSON and be sure to use the same project and experiment name when running platinum.

```yaml
argumentOverrides:
  starting_point: "calling_complete"
```

See [HMF cancer analysis pipeline](https://github.com/hartwigmedical/pipeline5) for a current list of available starting points.
                  |
