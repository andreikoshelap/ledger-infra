# ledger-api on Kubernetes — local practice run

Goal: go from "I've read about K8s" to "I've actually deployed my own Spring Boot
service on it" in about an hour, using `kind` (Kubernetes-in-Docker — lightest way
to get a real cluster on your laptop, no cloud account needed).

## 1. Create a local cluster

```bash
kind create cluster --name ledger-demo
kubectl cluster-info --context kind-ledger-demo
```

## 2. Build your ledger-api image and load it into kind

`kind` doesn't pull from a registry by default for local images — you build
locally then explicitly load the image into the cluster's nodes.

```bash
# from your ledger-api project root
docker build -t ledger-api:local .
kind load docker-image ledger-api:local --name ledger-demo
```

## 3. Apply the manifests in order

```bash
cd k8s-ledger-api
kubectl apply -f 00-namespace.yaml
kubectl apply -f 01-configmap.yaml
kubectl apply -f 02-secret.yaml
kubectl apply -f 03-postgres.yaml
kubectl apply -f 04-deployment.yaml
kubectl apply -f 05-service-ingress.yaml
```

(Files are numbered because `kubectl apply -f .` on a whole directory applies
them in an unpredictable order — namespace and config need to exist before
anything references them.)

## 4. Watch it come up

```bash
kubectl get pods -n ledger -w
# once pods are Running:
kubectl get deployments -n ledger
kubectl get svc -n ledger
```

If a pod is stuck in `CrashLoopBackOff` or `Pending`, this is the single most
useful debugging command — memorize it, interviewers love asking "how would
you debug a pod that won't start":

```bash
kubectl describe pod <pod-name> -n ledger   # events at the bottom tell you why
kubectl logs <pod-name> -n ledger           # app-level errors
```

## 5. Reach the service without an Ingress controller (fastest path)

```bash
kubectl port-forward -n ledger svc/ledger-api 8080:80
curl http://localhost:8080/actuator/health
```

## 6. Try the things that actually demonstrate understanding

```bash
# Scale up/down — watch new pods appear, old ones stay untouched
kubectl scale deployment ledger-api -n ledger --replicas=5

# Trigger a rolling update — edit the image tag, then:
kubectl set image deployment/ledger-api ledger-api=ledger-api:local -n ledger
kubectl rollout status deployment/ledger-api -n ledger
kubectl rollout undo deployment/ledger-api -n ledger   # rollback

# Kill a pod manually and watch the Deployment self-heal it
kubectl delete pod <pod-name> -n ledger
kubectl get pods -n ledger -w
```

## 7. Clean up

```bash
kind delete cluster --name ledger-demo
```

---

## Interview talking points this exercise directly supports

- **"Walk me through what happens when you `kubectl apply` a Deployment."**
  API server validates and stores it in etcd → Deployment controller creates a
  ReplicaSet → ReplicaSet controller creates Pods → scheduler assigns Pods to
  nodes → kubelet on each node pulls the image and starts containers.

- **"How do you debug a Pod stuck in Pending/CrashLoopBackOff?"**
  `describe pod` for events (scheduling failures, image pull errors, failed
  probes), `logs` for app-level errors, `logs --previous` if it already
  restarted.

- **"How do you do a zero-downtime deploy?"** — direct link to your CV line
  about zero-downtime deployments at Vuupala: `RollingUpdate` strategy with
  `maxUnavailable`/`maxSurge`, gated by `readinessProbe` so traffic never hits
  a pod before it's actually ready.

- **"Are Kubernetes Secrets actually secure?"** — No by default: base64 is
  encoding, not encryption. Needs etcd encryption-at-rest + RBAC restricting
  `get`/`list` on secrets, and ideally an external secrets manager
  (Vault, GCP Secret Manager) for anything real.
