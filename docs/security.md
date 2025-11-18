# Security Guide

Basic security setup for the RAVN Observability Platform.

## 🔒 Quick Security Setup

```bash
# Copy environment template
cp .env.example .env

# Generate a secure password (optional)
openssl rand -base64 32

# Edit .env and change the default password
# GRAFANA_ADMIN_PASSWORD=your_secure_password_here
```

## 🛡️ Security Checklist

### Authentication
- [ ] Change default admin credentials
- [ ] Disable anonymous access in production
- [ ] Enable TLS/SSL encryption

### Data Protection
- [ ] Set up data retention policies
- [ ] Use cloud storage encryption
- [ ] Enable backup

## 🔐 Basic Authentication

### Local Development
```bash
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=secure_password
TLS_ENABLED=false
```

### Production
```bash
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=strong_random_password
TLS_ENABLED=true
TLS_CERT_PATH=/etc/ssl/certs/observability.crt
TLS_KEY_PATH=/etc/ssl/private/observability.key
```

## 🔒 TLS Configuration

### Custom Certificates
```bash
TLS_ENABLED=true
TLS_CERT_PATH=/etc/ssl/certs/observability.crt
TLS_KEY_PATH=/etc/ssl/private/observability.key
```

## 🚨 Emergency Recovery

If locked out of admin account:

```bash
# Reset Grafana admin password (Docker)
docker exec -it grafana grafana-cli admin reset-admin-password newpassword
```

## 📞 Support

For security issues: security@ravn.co