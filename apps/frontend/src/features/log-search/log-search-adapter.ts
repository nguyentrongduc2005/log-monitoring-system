import type {
  LogSearchApplication,
  LogSearchBucket,
  LogSearchEntry,
  LogSearchFilters,
  LogSearchSnapshot,
  LogSearchSummary,
} from "@/features/log-search/log-search-types";

const applications: LogSearchApplication[] = [
  { id: "checkout-api", name: "Checkout API" },
  { id: "billing-worker", name: "Billing Worker" },
  { id: "identity-service", name: "Identity Service" },
];

const logs: LogSearchEntry[] = [
  {
    id: "log-1008",
    timestamp: "2026-06-23 10:30:22.184",
    applicationId: "checkout-api",
    applicationName: "Checkout API",
    level: "ERROR",
    message:
      "Payment gateway timeout after 3000ms while authorizing order ORD-8842",
    traceId: "trc-pay-8842",
    spanId: "span-authorize",
    eventId: "evt-ck-1008",
    source: "payment.authorize",
    host: "checkout-api-7f9c6d9f4b-h2t7n",
    durationMs: 3104,
    statusCode: 504,
    attributes: {
      orderId: "ORD-8842",
      customerId: "CUS-3911",
      provider: "stripe",
      endpoint: "/api/checkout/pay",
    },
    stack: [
      "PaymentGatewayClient.authorize(PaymentGatewayClient.java:88)",
      "CheckoutService.capturePayment(CheckoutService.java:142)",
      "CheckoutController.pay(CheckoutController.java:57)",
    ],
  },
  {
    id: "log-1007",
    timestamp: "2026-06-23 10:30:21.903",
    applicationId: "checkout-api",
    applicationName: "Checkout API",
    level: "WARN",
    message: "Retrying payment authorization attempt=2 order=ORD-8842",
    traceId: "trc-pay-8842",
    spanId: "span-retry-2",
    eventId: "evt-ck-1007",
    source: "payment.retry",
    host: "checkout-api-7f9c6d9f4b-h2t7n",
    durationMs: 906,
    attributes: {
      orderId: "ORD-8842",
      attempt: "2",
      retryPolicy: "payment-gateway-standard",
    },
  },
  {
    id: "log-1006",
    timestamp: "2026-06-23 10:30:20.441",
    applicationId: "checkout-api",
    applicationName: "Checkout API",
    level: "INFO",
    message: "Started checkout payment request order=ORD-8842",
    traceId: "trc-pay-8842",
    spanId: "span-request",
    eventId: "evt-ck-1006",
    source: "checkout.controller",
    host: "checkout-api-7f9c6d9f4b-h2t7n",
    durationMs: 12,
    attributes: {
      orderId: "ORD-8842",
      method: "POST",
      path: "/api/checkout/pay",
    },
  },
  {
    id: "log-1005",
    timestamp: "2026-06-23 10:29:45.118",
    applicationId: "billing-worker",
    applicationName: "Billing Worker",
    level: "ERROR",
    message: "Invoice retry storm detected for batch INV-BATCH-118",
    traceId: "trc-inv-118",
    spanId: "span-invoice-batch",
    eventId: "evt-bw-1005",
    source: "invoice.scheduler",
    host: "billing-worker-64b8b77d8c-qg92r",
    durationMs: 1840,
    attributes: {
      batchId: "INV-BATCH-118",
      retryCount: "37",
      queue: "invoice-retry",
    },
  },
  {
    id: "log-1004",
    timestamp: "2026-06-23 10:28:16.774",
    applicationId: "identity-service",
    applicationName: "Identity Service",
    level: "WARN",
    message: "JWT refresh rejected because token family was revoked",
    traceId: "trc-auth-204",
    spanId: "span-refresh-token",
    eventId: "evt-id-1004",
    source: "auth.refresh",
    host: "identity-service-68cc9f8bb7-s6mkl",
    durationMs: 52,
    statusCode: 401,
    attributes: {
      userId: "USR-204",
      reason: "token_family_revoked",
      path: "/oauth/token",
    },
  },
  {
    id: "log-1003",
    timestamp: "2026-06-23 10:27:34.211",
    applicationId: "checkout-api",
    applicationName: "Checkout API",
    level: "CRITICAL",
    message: "Circuit breaker opened for payment gateway provider=stripe",
    traceId: "trc-pay-circuit",
    spanId: "span-circuit-breaker",
    eventId: "evt-ck-1003",
    source: "payment.circuit-breaker",
    host: "checkout-api-7f9c6d9f4b-h2t7n",
    durationMs: 4,
    attributes: {
      provider: "stripe",
      failureWindow: "5m",
      failureRate: "62%",
    },
  },
  {
    id: "log-1002",
    timestamp: "2026-06-23 10:24:02.490",
    applicationId: "billing-worker",
    applicationName: "Billing Worker",
    level: "WARN",
    message: "Invoice job latency above threshold duration=1410ms",
    traceId: "trc-inv-latency",
    spanId: "span-invoice-job",
    eventId: "evt-bw-1002",
    source: "invoice.job",
    host: "billing-worker-64b8b77d8c-qg92r",
    durationMs: 1410,
    attributes: {
      thresholdMs: "1000",
      queueDepth: "824",
    },
  },
  {
    id: "log-1001",
    timestamp: "2026-06-23 10:21:51.067",
    applicationId: "checkout-api",
    applicationName: "Checkout API",
    level: "INFO",
    message: "Checkout cart validated successfully order=ORD-8839",
    traceId: "trc-cart-8839",
    spanId: "span-cart-validation",
    eventId: "evt-ck-1001",
    source: "cart.validation",
    host: "checkout-api-6d775b4f89-5r8xr",
    durationMs: 38,
    attributes: {
      orderId: "ORD-8839",
      itemCount: "3",
    },
  },
];

const bucketsByRange: Record<LogSearchFilters["range"], LogSearchBucket[]> = {
  "15m": [
    { time: "10:16", total: 180, info: 142, warn: 24, error: 13, critical: 1 },
    { time: "10:19", total: 210, info: 164, warn: 28, error: 17, critical: 1 },
    { time: "10:22", total: 245, info: 188, warn: 35, error: 20, critical: 2 },
    { time: "10:25", total: 231, info: 172, warn: 39, error: 18, critical: 2 },
    { time: "10:28", total: 292, info: 205, warn: 48, error: 34, critical: 5 },
    { time: "10:31", total: 318, info: 218, warn: 57, error: 37, critical: 6 },
  ],
  "1h": [
    { time: "09:35", total: 580, info: 486, warn: 62, error: 29, critical: 3 },
    { time: "09:45", total: 620, info: 498, warn: 74, error: 43, critical: 5 },
    { time: "09:55", total: 710, info: 559, warn: 91, error: 54, critical: 6 },
    { time: "10:05", total: 760, info: 592, warn: 104, error: 56, critical: 8 },
    { time: "10:15", total: 830, info: 641, warn: 118, error: 62, critical: 9 },
    {
      time: "10:25",
      total: 890,
      info: 676,
      warn: 132,
      error: 70,
      critical: 12,
    },
  ],
  "6h": [
    {
      time: "05:00",
      total: 1960,
      info: 1640,
      warn: 218,
      error: 91,
      critical: 11,
    },
    {
      time: "06:00",
      total: 2100,
      info: 1732,
      warn: 240,
      error: 113,
      critical: 15,
    },
    {
      time: "07:00",
      total: 2280,
      info: 1855,
      warn: 276,
      error: 132,
      critical: 17,
    },
    {
      time: "08:00",
      total: 2420,
      info: 1944,
      warn: 318,
      error: 137,
      critical: 21,
    },
    {
      time: "09:00",
      total: 2680,
      info: 2118,
      warn: 370,
      error: 166,
      critical: 26,
    },
    {
      time: "10:00",
      total: 2980,
      info: 2310,
      warn: 428,
      error: 209,
      critical: 33,
    },
  ],
  "24h": [
    {
      time: "Jun 22 12:00",
      total: 7200,
      info: 6120,
      warn: 760,
      error: 292,
      critical: 28,
    },
    {
      time: "Jun 22 16:00",
      total: 7600,
      info: 6340,
      warn: 862,
      error: 356,
      critical: 42,
    },
    {
      time: "Jun 22 20:00",
      total: 6900,
      info: 5850,
      warn: 710,
      error: 306,
      critical: 34,
    },
    {
      time: "Jun 23 00:00",
      total: 5200,
      info: 4550,
      warn: 476,
      error: 158,
      critical: 16,
    },
    {
      time: "Jun 23 04:00",
      total: 6100,
      info: 5160,
      warn: 662,
      error: 248,
      critical: 30,
    },
    {
      time: "Jun 23 08:00",
      total: 8400,
      info: 6810,
      warn: 1040,
      error: 492,
      critical: 58,
    },
  ],
};

export async function searchLogs(
  filters: LogSearchFilters,
  selectedLogId?: string,
): Promise<LogSearchSnapshot> {
  const query = filters.query.trim().toLowerCase();
  const results = logs.filter((log) => {
    const matchesApplication =
      !filters.applicationId || log.applicationId === filters.applicationId;
    const matchesLevel = filters.level === "ALL" || log.level === filters.level;
    const searchableText = [
      log.message,
      log.traceId,
      log.eventId,
      log.source,
      log.host,
      ...Object.values(log.attributes),
    ]
      .join(" ")
      .toLowerCase();
    const matchesQuery = !query || searchableText.includes(query);

    return matchesApplication && matchesLevel && matchesQuery;
  });

  const selectedLog =
    results.find((log) => log.id === selectedLogId) ?? results[0];
  const relatedTrace = selectedLog
    ? logs
        .filter((log) => log.traceId === selectedLog.traceId)
        .sort((left, right) => left.timestamp.localeCompare(right.timestamp))
    : [];

  return {
    applications,
    summary: createSummary(results),
    buckets: bucketsByRange[filters.range],
    results,
    relatedTrace,
  };
}

function createSummary(results: LogSearchEntry[]): LogSearchSummary {
  return {
    totalMatches: results.length,
    errorMatches: results.filter((log) => log.level === "ERROR").length,
    criticalMatches: results.filter((log) => log.level === "CRITICAL").length,
    uniqueTraces: new Set(results.map((log) => log.traceId)).size,
    slowestDurationMs: Math.max(
      0,
      ...results.map((log) => log.durationMs ?? 0),
    ),
  };
}
