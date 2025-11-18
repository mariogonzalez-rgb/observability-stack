# RAVN Observability Stack

A standardized, self-hosted observability stack template for modern applications. Deploy your own cost-effective alternative to expensive SaaS solutions like Datadog or New Relic.

## Why This Exists

- **💰 Cost Effective**: ~90% cheaper than SaaS alternatives at scale
- **🎯 Standardized**: Same observability setup across all RAVN projects  
- **🔒 Data Ownership**: Your metrics stay in your infrastructure
- **🚀 Full Featured**: Logs, metrics, traces, and dashboards out of the box
- **☁️ Cloud Agnostic**: Deploy anywhere Docker runs

## What's Included

- **📊 Grafana** - Dashboards and visualization
- **📝 Loki** - Log aggregation and search
- **📈 Mimir** - Metrics storage (Prometheus-compatible)
- **🔍 Tempo** - Distributed tracing
- **🚛 Alloy** - Telemetry collection (OpenTelemetry-compatible)

## Quick Start

### One-Line Install

```bash
curl -sSL https://raw.githubusercontent.com/ravn/observability-stack/main/install.sh | bash
```

This will:
- Check for Docker and docker-compose
- Download the stack to `./observability`
- Generate secure passwords
- Start all services
- Show you the Grafana login credentials

### Manual Install

```bash
# 1. Clone this template for your project
git clone https://github.com/ravn/observability-stack my-project-observability
cd my-project-observability

# 2. Configure environment
cp .env.example .env
# Edit .env - set GRAFANA_ADMIN_PASSWORD

# 3. Start the stack
docker-compose up -d

# 4. Access Grafana
open http://localhost:3030
# Login with admin / your-password
```

## Sending Telemetry

### From Your Application

Your apps send telemetry to the Alloy collector on port 4317 (gRPC) or 4318 (HTTP):

```javascript
// Node.js Example
const { NodeSDK } = require('@opentelemetry/sdk-node');

const sdk = new NodeSDK({
  serviceName: 'my-app',
  // Point to your deployed observability stack
  traceExporterUrl: 'http://your-stack-host:4317',
});
```

See [examples/](./examples/) for language-specific integration:
- [Node.js (Express)](./examples/nodejs/express/)
- [Node.js (Next.js)](./examples/nodejs/nextjs/)
- [Node.js (NestJS)](./examples/nodejs/nestjs/)
- [Java Spring Boot](./examples/java/)
- [Ruby on Rails](./examples/rails/)

### System Metrics

The stack automatically collects system metrics when deployed. For additional monitoring:

```yaml
# docker-compose.yml - Add node-exporter for system metrics
node-exporter:
  image: prom/node-exporter:latest
  ports:
    - "9100:9100"
```

### Database Metrics

PostgreSQL monitoring is built-in. Configure with:

```bash
# .env
DB_DSN=postgresql://user:pass@your-db:5432/dbname?sslmode=require
```

## Deployment

Each project should deploy its own instance. Choose your platform:

### AWS (ECS with Fargate)

```bash
# Build and push to ECR
aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_URL
docker build -t observability .
docker tag observability:latest $ECR_URL/observability:latest
docker push $ECR_URL/observability:latest

# Deploy with provided task definition
aws ecs create-service \
  --cluster your-cluster \
  --service-name observability \
  --task-definition observability-task
```

[Full AWS Guide →](./docs/deploy-aws.md)

### GCP (Cloud Run)

```bash
# Build and deploy
gcloud builds submit --tag gcr.io/PROJECT/observability
gcloud run deploy observability \
  --image gcr.io/PROJECT/observability \
  --platform managed \
  --allow-unauthenticated \
  --set-env-vars "STORAGE_TYPE=gcs,GCS_BUCKET_LOGS=my-logs"
```

[Full GCP Guide →](./docs/deploy-gcp.md)

### Railway

1. Fork this repository
2. Connect Railway to your GitHub
3. Create new project from repository
4. Add environment variables from `.env.example`
5. Deploy

[Full Railway Guide →](./docs/deploy-railway.md)

### Generic VPS (Docker)

```bash
# On your server with Docker installed
git clone <your-fork> observability
cd observability
cp .env.example .env
# Edit .env with production values
docker-compose up -d
```

## Configuration

### Storage Backends

By default, uses local disk. For production, configure cloud storage:

```bash
# AWS S3
STORAGE_TYPE=s3
AWS_REGION=us-west-2
S3_BUCKET_LOGS=my-app-logs
S3_BUCKET_TRACES=my-app-traces  
S3_BUCKET_METRICS=my-app-metrics

# Google Cloud Storage
STORAGE_TYPE=gcs
GCS_BUCKET_LOGS=my-app-logs
GCS_BUCKET_TRACES=my-app-traces
GCS_BUCKET_METRICS=my-app-metrics
```

### Data Retention

Configure how long to keep data:

```bash
LOKI_RETENTION_HOURS=168     # 7 days of logs
TEMPO_RETENTION_HOURS=336    # 14 days of traces
MIMIR_RETENTION_HOURS=8760   # 1 year of metrics
```

### Resource Limits

Adjust based on your load:

```bash
# For small projects (< 100 req/s)
LOKI_INGESTION_RATE_MB=4
TEMPO_INGESTION_RATE_MB=100
MIMIR_INGESTION_RATE=50000

# For larger projects
LOKI_INGESTION_RATE_MB=50
TEMPO_INGESTION_RATE_MB=500
MIMIR_INGESTION_RATE=500000
```

## Cost Comparison

Estimated monthly costs for a typical application:

| Solution | Logs (GB) | Metrics | Traces | Total/Month |
|----------|-----------|---------|--------|-------------|
| Datadog | 100 | 1M series | 1M spans | ~$800 |
| New Relic | 100 | 1M series | 1M spans | ~$650 |
| **This Stack (AWS)** | 100 | 1M series | 1M spans | **~$50** |
| **This Stack (GCP)** | 100 | 1M series | 1M spans | **~$45** |

*Costs include compute (1 vCPU, 2GB RAM) and storage. Actual costs vary by region and usage.*

## What Gets Collected

When properly integrated, you'll have:

- **📝 Logs**: Application logs, errors, structured events
- **📊 Metrics**: Request rates, latency, error rates, custom business metrics
- **🔍 Traces**: Request flow across services, SQL queries, external API calls
- **💻 System**: CPU, memory, disk, network metrics
- **🗄️ Database**: Query performance, connection pools, slow queries

## Security

- Change default passwords
- Use TLS in production (`TLS_ENABLED=true`)
- Restrict network access (firewall/security groups)
- Regular backups of Grafana dashboards
- See [Security Guide](./docs/security.md)

## Contributing

We need help with:

### Priority Tasks

1. **Node.js Examples**: Implement working examples for Express, Next.js 15, and NestJS
2. **Deployment Guides**: Document and test AWS ECS, GCP Cloud Run, Railway deployments
3. **Dashboards**: Create default Grafana dashboards for common scenarios
4. **Alerts**: Add example alert rules for common issues

### How to Contribute

1. Fork this repository
2. Pick a task from [issues](../../issues) or create one
3. Make your changes
4. Test locally with `docker-compose up`
5. Submit a PR

## Support

- **Issues**: [GitHub Issues](../../issues)
- **Discussions**: [GitHub Discussions](../../discussions)
- **Internal**: #observability Slack channel

## License

MIT - Use this template freely for your projects.

---

Built with ❤️ by RAVN