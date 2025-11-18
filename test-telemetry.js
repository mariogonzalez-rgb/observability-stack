const { NodeSDK } = require('@opentelemetry/sdk-node');
const { getNodeAutoInstrumentations } = require('@opentelemetry/auto-instrumentations-node');
const { Resource } = require('@opentelemetry/resources');
const { SemanticResourceAttributes } = require('@opentelemetry/semantic-conventions');
const { OTLPTraceExporter } = require('@opentelemetry/exporter-trace-otlp-grpc');
const { OTLPMetricExporter } = require('@opentelemetry/exporter-metrics-otlp-grpc');
const { PeriodicExportingMetricReader } = require('@opentelemetry/sdk-metrics');
const opentelemetry = require('@opentelemetry/api');

// Create SDK
const sdk = new NodeSDK({
  resource: new Resource({
    [SemanticResourceAttributes.SERVICE_NAME]: 'test-service',
    [SemanticResourceAttributes.SERVICE_VERSION]: '1.0.0',
    environment: 'development',
  }),
  traceExporter: new OTLPTraceExporter({
    url: 'http://localhost:4317',
  }),
  metricReader: new PeriodicExportingMetricReader({
    exporter: new OTLPMetricExporter({
      url: 'http://localhost:4317',
    }),
    exportIntervalMillis: 1000,
  }),
  instrumentations: [getNodeAutoInstrumentations()],
});

// Initialize SDK
sdk.start();

// Get tracer
const tracer = opentelemetry.trace.getTracer('test-tracer');

// Get meter
const meter = opentelemetry.metrics.getMeter('test-meter');

// Create metrics
const requestCounter = meter.createCounter('http_requests_total', {
  description: 'Total HTTP requests',
});

const requestDuration = meter.createHistogram('http_request_duration_ms', {
  description: 'HTTP request duration in milliseconds',
  unit: 'ms',
});

const activeUsers = meter.createUpDownCounter('active_users', {
  description: 'Number of active users',
});

const cpuUsage = meter.createObservableGauge('cpu_usage_percent', {
  description: 'CPU usage percentage',
});

// Set up CPU usage callback
cpuUsage.addCallback((observableResult) => {
  observableResult.observe(Math.random() * 100, { core: 'cpu0' });
  observableResult.observe(Math.random() * 100, { core: 'cpu1' });
});

// Send test data
async function sendTestData() {
  console.log(`[${new Date().toISOString()}] Sending telemetry...`);
  
  // Simulate different operations
  const operations = [
    { name: 'user-login', duration: 150, status: 'success' },
    { name: 'fetch-data', duration: 75, status: 'success' },
    { name: 'process-payment', duration: 500, status: 'success' },
    { name: 'send-email', duration: 200, status: 'failed' },
    { name: 'update-profile', duration: 100, status: 'success' },
  ];
  
  for (const op of operations) {
    // Create a span for each operation
    const span = tracer.startSpan(op.name, {
      attributes: {
        'operation.type': op.name,
        'service.environment': 'development',
      }
    });
    
    // Add span attributes
    span.setAttribute('http.method', 'POST');
    span.setAttribute('http.route', `/api/${op.name}`);
    span.setAttribute('http.status_code', op.status === 'success' ? 200 : 500);
    
    // Record metrics
    requestCounter.add(1, { 
      method: 'POST', 
      route: `/api/${op.name}`,
      status: op.status === 'success' ? '200' : '500'
    });
    
    requestDuration.record(op.duration + Math.random() * 50, { 
      method: 'POST',
      route: `/api/${op.name}`
    });
    
    // Simulate active users
    if (Math.random() > 0.5) {
      activeUsers.add(1);
    } else {
      activeUsers.add(-1);
    }
    
    // Simulate work
    await new Promise(resolve => setTimeout(resolve, op.duration));
    
    // Add events
    if (op.status === 'failed') {
      span.recordException(new Error(`Operation ${op.name} failed`));
      span.setStatus({ code: opentelemetry.SpanStatusCode.ERROR });
    } else {
      span.addEvent('operation-completed', {
        'result.size': Math.floor(Math.random() * 1000),
      });
      span.setStatus({ code: opentelemetry.SpanStatusCode.OK });
    }
    
    // End span
    span.end();
  }
  
  console.log('✓ Telemetry sent');
}

// Main loop
async function main() {
  console.log('Starting telemetry test...');
  console.log('Sending data to: http://localhost:4317');
  console.log('Press Ctrl+C to stop\n');
  
  // Send initial burst
  for (let i = 0; i < 3; i++) {
    await sendTestData();
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  
  // Then send every 5 seconds
  while (true) {
    await sendTestData();
    await new Promise(resolve => setTimeout(resolve, 5000));
  }
}

// Graceful shutdown
process.on('SIGINT', async () => {
  console.log('\nShutting down...');
  await sdk.shutdown();
  process.exit(0);
});

main().catch(console.error);