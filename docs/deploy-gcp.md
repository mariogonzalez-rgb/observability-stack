# Deploy to Google Cloud Platform

Deploy the observability stack to GCP using Cloud Run or Compute Engine.

## Option 1: Compute Engine with Docker

Simple deployment on a VM instance.

### 1. Create VM Instance

```bash
gcloud compute instances create observability-stack \
  --machine-type=e2-medium \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=50GB \
  --tags=observability \
  --zone=us-central1-a
```

### 2. Configure Firewall

```bash
# Allow Grafana and OTLP traffic
gcloud compute firewall-rules create observability-ports \
  --allow tcp:3030,tcp:4317,tcp:4318 \
  --source-ranges 0.0.0.0/0 \
  --target-tags observability
```

### 3. Install Docker

```bash
# SSH into instance
gcloud compute ssh observability-stack

# Install Docker
sudo apt-get update
sudo apt-get install -y docker.io docker-compose
sudo usermod -aG docker $USER
# Logout and login again for group changes
```

### 4. Deploy Stack

```bash
# Clone repository
git clone https://github.com/your-org/observability-stack.git
cd observability-stack

# Configure for GCS
cp .env.example .env
nano .env  # Set passwords and GCS buckets

# Start services
docker-compose up -d
```

### 5. Configure Cloud Storage

```bash
# .env configuration
STORAGE_TYPE=gcs
GCS_BUCKET_LOGS=my-project-logs
GCS_BUCKET_TRACES=my-project-traces
GCS_BUCKET_METRICS=my-project-metrics
```

## Option 2: Cloud Run (Serverless)

Deploy as a managed container service.

### 1. Prepare Container

Create `Dockerfile`:

```dockerfile
FROM docker/compose:latest
WORKDIR /app
COPY . .
RUN apk add --no-cache bash
CMD ["docker-compose", "up"]
```

### 2. Build and Deploy

```bash
# Enable required APIs
gcloud services enable cloudbuild.googleapis.com run.googleapis.com

# Build and push to Container Registry
gcloud builds submit --tag gcr.io/YOUR_PROJECT/observability

# Deploy to Cloud Run
gcloud run deploy observability \
  --image gcr.io/YOUR_PROJECT/observability \
  --platform managed \
  --region us-central1 \
  --memory 2Gi \
  --cpu 2 \
  --port 3030 \
  --allow-unauthenticated \
  --set-env-vars "STORAGE_TYPE=gcs" \
  --set-env-vars "GCS_BUCKET_LOGS=my-logs" \
  --set-env-vars "GCS_BUCKET_TRACES=my-traces" \
  --set-env-vars "GCS_BUCKET_METRICS=my-metrics"
```

## Option 3: Google Kubernetes Engine (GKE)

For more control and scalability.

### 1. Create Cluster

```bash
gcloud container clusters create observability-cluster \
  --machine-type=e2-standard-2 \
  --num-nodes=2 \
  --zone=us-central1-a
```

### 2. Deploy with Kubernetes

```yaml
# observability-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: observability
spec:
  replicas: 1
  selector:
    matchLabels:
      app: observability
  template:
    metadata:
      labels:
        app: observability
    spec:
      containers:
      - name: observability
        image: gcr.io/YOUR_PROJECT/observability:latest
        ports:
        - containerPort: 3030
        - containerPort: 4317
        - containerPort: 4318
        env:
        - name: STORAGE_TYPE
          value: "gcs"
        - name: GCS_BUCKET_LOGS
          value: "my-logs"
---
apiVersion: v1
kind: Service
metadata:
  name: observability
spec:
  type: LoadBalancer
  ports:
  - name: grafana
    port: 3030
    targetPort: 3030
  - name: otlp-grpc
    port: 4317
    targetPort: 4317
  - name: otlp-http
    port: 4318
    targetPort: 4318
  selector:
    app: observability
```

```bash
kubectl apply -f observability-deployment.yaml
```

## Service Account Setup

Create service account for GCS access:

```bash
# Create service account
gcloud iam service-accounts create observability-sa \
  --display-name="Observability Stack"

# Grant storage permissions
gcloud projects add-iam-policy-binding YOUR_PROJECT \
  --member="serviceAccount:observability-sa@YOUR_PROJECT.iam.gserviceaccount.com" \
  --role="roles/storage.objectAdmin"

# Create and download key
gcloud iam service-accounts keys create key.json \
  --iam-account=observability-sa@YOUR_PROJECT.iam.gserviceaccount.com

# Set in environment
export GOOGLE_APPLICATION_CREDENTIALS=key.json
```

## Load Balancer with SSL

Set up HTTPS with managed certificates:

```bash
# Reserve static IP
gcloud compute addresses create observability-ip --global

# Create managed certificate
gcloud compute ssl-certificates create observability-cert \
  --domains=observability.yourdomain.com \
  --global

# Configure load balancer in Cloud Console or via API
```

## Monitoring & Logging

Enable Cloud Monitoring and Logging:

```bash
# View logs
gcloud logging read "resource.type=gce_instance AND resource.labels.instance_id=observability-stack"

# Set up alerts
gcloud alpha monitoring policies create \
  --notification-channels=YOUR_CHANNEL \
  --display-name="High Memory Usage" \
  --condition="resource.type=\"gce_instance\" AND metric.type=\"compute.googleapis.com/instance/memory/utilization\" > 0.9"
```

## Cost Optimization

- Use Preemptible VMs for non-production
- Enable autoscaling for GKE
- Set up lifecycle rules for GCS buckets
- Use committed use discounts for production

## Backup Strategy

```bash
# Backup Grafana dashboards
gsutil -m cp -r /var/lib/grafana/dashboards gs://backup-bucket/grafana/

# Schedule regular backups
gcloud scheduler jobs create app-engine backup-grafana \
  --schedule="0 2 * * *" \
  --uri="https://YOUR_CLOUD_FUNCTION_URL"
```

## Next Steps

1. Point your applications to the load balancer endpoint
2. Import Grafana dashboards
3. Configure data retention policies
4. Set up authentication
5. Enable Cloud Armor for DDoS protection