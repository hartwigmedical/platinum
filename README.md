# Platinum

This project is to enable running Pipeline5 at scale, using Kubernetes for orchestration, ie
_P_ipe_L_ine _AT_ any _num_ber! It works by creating a Job in your Kubernetes cluster for each pair of samples you provide. 
To use it:

1. Run the `make_cluster.sh` script if you don't already have a Kubernetes cluster in mind
2. Make a JSON file describing your inputs and arguments to the pipeline
3. Execute the application passing your JSON file

## Making the Cluster

This is scripted and should work if you have the required tools installed and there are not any peculiarities about your GKE setup
(custom networking, etc). The script should tell you if you are missing any dependencies or if anything fails.

When this succeeds the machine you're executing on should be configured to connect to the new cluster via the `gcloud kubectl`
component. 

## Making the JSON file

Platinum uses a JSON batch descriptor file to specify:

* Where your samples are and what their names are;
* Where the output of the pipeline will go;
* Arguments to pass to the invoked pipeline processes.

## Executing the Pipelines

With the setup complete the Platinum application is run with the JSON file that was prepared above. The cluster setup script will
have configured the `gcloud kubectl` component to connect to the new cluster it created. Platinum depends on the environment to
have been setup so that getting a default client will connect to the correct cluster.

