# Deploy to AWS

Deploy the observability stack to AWS using ECS with Fargate or EC2.

## Option 1: EC2 with Docker

Simple deployment on a single EC2 instance.

### 1. Launch EC2 Instance

```bash
# Recommended: t3.medium or larger
# AMI: Amazon Linux 2023 or Ubuntu 22.04
# Storage: 50GB minimum
# Security Group: Open ports 3030 (Grafana), 4317 (OTLP)
```

### 2. Install Docker

```bash
# Amazon Linux 2023
sudo yum update -y
sudo yum install docker -y
sudo service docker start
sudo usermod -a -G docker ec2-user

# Install docker-compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 3. Deploy Stack

```bash
# Clone repository
git clone https://github.com/your-org/observability-stack.git
cd observability-stack

# Configure for production
cp .env.example .env
nano .env  # Set passwords and S3 buckets

# Start services
docker-compose up -d
```

### 4. Configure S3 Storage

```bash
# .env configuration
STORAGE_TYPE=s3
AWS_REGION=us-west-2
S3_BUCKET_LOGS=my-project-logs
S3_BUCKET_TRACES=my-project-traces
S3_BUCKET_METRICS=my-project-metrics
```

## Option 2: ECS with Fargate

Serverless container deployment.

### 1. Create Task Definition

Save as `observability-task.json`:

```json
{
  "family": "observability",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "containerDefinitions": [
    {
      "name": "observability",
      "image": "YOUR_ECR_URL/observability:latest",
      "portMappings": [
        {"containerPort": 3030, "protocol": "tcp"},
        {"containerPort": 4317, "protocol": "tcp"},
        {"containerPort": 4318, "protocol": "tcp"}
      ],
      "environment": [
        {"name": "STORAGE_TYPE", "value": "s3"},
        {"name": "AWS_REGION", "value": "us-west-2"}
      ],
      "secrets": [
        {"name": "GRAFANA_ADMIN_PASSWORD", "valueFrom": "arn:aws:secretsmanager:region:account:secret:grafana-password"}
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/observability",
          "awslogs-region": "us-west-2",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

### 2. Build and Push Image

```bash
# Build Docker image
docker build -t observability .

# Tag for ECR
aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin YOUR_ECR_URL
docker tag observability:latest YOUR_ECR_URL/observability:latest
docker push YOUR_ECR_URL/observability:latest
```

### 3. Create Service

```bash
# Register task definition
aws ecs register-task-definition --cli-input-json file://observability-task.json

# Create service
aws ecs create-service \
  --cluster your-cluster \
  --service-name observability \
  --task-definition observability:1 \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx],assignPublicIp=ENABLED}"
```

## IAM Permissions

Create IAM role with these permissions for S3 access:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::my-project-logs/*",
        "arn:aws:s3:::my-project-traces/*",
        "arn:aws:s3:::my-project-metrics/*",
        "arn:aws:s3:::my-project-logs",
        "arn:aws:s3:::my-project-traces",
        "arn:aws:s3:::my-project-metrics"
      ]
    }
  ]
}
```

## Load Balancer Setup

For production, use an Application Load Balancer:

```bash
# Create target groups for:
# - Port 3030 → Grafana (HTTP)
# - Port 4317 → OTLP gRPC
# - Port 4318 → OTLP HTTP

# Add HTTPS listener with ACM certificate
# Route / → Grafana target group
```

## Monitoring & Alerts

Set up CloudWatch alarms for:

- High CPU/Memory usage
- S3 bucket size
- ECS task failures
- Application errors in logs

## Cost Optimization

- Use Spot instances for EC2
- Consider Fargate Spot for non-critical workloads
- Set S3 lifecycle policies for old data
- Use S3 Intelligent-Tiering

## Next Steps

1. Configure your applications to send telemetry to the load balancer endpoint
2. Set up Grafana dashboards
3. Configure retention policies
4. Enable TLS/SSL
5. Set up backups for Grafana database