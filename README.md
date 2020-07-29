# Platinum

This project is to enable running Pipeline5 at scale, using Kubernetes for orchestration, ie
_P_ipe_L_ine _AT_ any _num_ber!

## To Make The Cluster

On my version of `gcloud` (294.0.0.) it asks to enable the required APIs if they are not already active. Older versions might
require you to manually enable them first. Note that this procedure only worked for me in development, I think the other
environments have default networking in place that is too restrictive for the health checks to complete.

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
# gcloud container clusters get-credentials platinum-cluster --zone europe-west4-a --project ...
# kubectl create namespace platinum
# kubectl config set-context --current --namespace=platinum
# kubectl create clusterrolebinding ned-cluster-admin-binding --clusterrole=cluster-admin --user=n.leitch@hartwigmedicalfoundation.nl
# kubectl apply -n platinum -f https://raw.githubusercontent.com/argoproj/argo/stable/manifests/install.yaml
# curl -sSL -o ~/bin/argo https://github.com/argoproj/argo/releases/download/v2.2.1/argo-linux-amd64
# chmod +x ~/bin/argo
```

Simple explanation of how to use it on the CLI: `https://argoproj.github.io/argo/examples/#argo-cli`

## Preapring Data for the Run

Make your JSON files according to the `sample_json` format in the Pipleine5 repo. Put them all in a single directory named
according to their samples (eg `CPCT12345678.json`) and then load them into a config map (here the directory containing the JSON
files is `jsons` and we're in its parent directory):

```
# kubectl create configmap patient-jsons --from-file=jsons
```

Then make the required secret for the pipeline to run:

```
# kubectl create secret generic platinum-bootstrap-key --from-file=platinum-bootstrap-key=./hmf-crunch-425af65e20aa.json
```

## Additional Configuration 

If you get these in the logs for the Argo controller:


```
n_leitch@cloudshell:~/DEV-1389$ kubectl logs workflow-controller-7955968c89-q84r9
time="2020-07-24T23:31:23Z" level=info msg="config map" name=workflow-controller-configmap
time="2020-07-24T23:31:23Z" level=fatal msg="Failed to register watch for controller config map: configmaps \"workflow-controller-configmap\" is forbidden: User \"system:serviceaccount:platinum:argo\" cannot get resource \"configmaps\" in API group \"\" in the namespace \"platinum\""
```
Then you have to do this because for some reason the Argo install doesn't give itself the right permissions:

```
# kubectl create clusterrolebinding argo-cluster-admin-binding --clusterrole=cluster-admin --user=system:serviceaccount:platinum:argo
# kubectl create clusterrolebinding argo-server-cluster-admin-binding --clusterrole=cluster-admin --user=system:serviceaccount:platinum:argo-server
# kubectl create clusterrolebinding argo-cluster-platinum-binding --clusterrole=cluster-admin --user=system:serviceaccount:platinum:default
```

Suspect those permissions are too much and we could/should reduce them to some miniumum necessary privileges.


