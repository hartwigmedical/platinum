# Platinum

This project is to enable running Pipeline5 at scale, using Kubernetes for orchestration, ie
_P_ipe_L_ine _AT_ any _num_ber!

## To Make The Cluster

On my version of `gcloud` (294.0.0.) it asks to enable the required APIs if they are not already active. Older versions might
require you to manually enable them first.

```
# gcloud config set project ...
# gcloud config set account ...
# gcloud deployment-manager deployments create platinum-cluster --config cluster.yaml
```

To delete it: 

```
# gcloud deployment-manager deployments delete platinum-cluster
```

## Installing Workflow Management on the Cluster

```
# kubectl create namespace platinum
# kubectl apply -n platinum -f https://raw.githubusercontent.com/argoproj/argo/stable/manifests/install.yaml
# curl -sSL -o ~/bin/argo https://github.com/argoproj/argo/releases/download/v2.2.1/argo-linux-amd64
# chmod +x ~/bin/argo
```

Simple explanation of how to use it on the CLI: `https://argoproj.github.io/argo/examples/#argo-cli`


