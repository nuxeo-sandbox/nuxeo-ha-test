# k8s-chaos-monkey

Once launched in a Kubernetes namespace, acts as a [Chaos Monkey](https://en.wikipedia.org/wiki/Chaos_Monkey) and deletes a randomly selected pod based on the label that is specified as `K8SMONKEY_LABEL_SELECTOR` environment variable.

The pod can be deployed with the `k8s-chaos.yaml` descriptor found in the parent directory.

*Important:* You also have to check that the `serviceAccount: chaos-monkey`  has the right to delete Pods within the current namespace.