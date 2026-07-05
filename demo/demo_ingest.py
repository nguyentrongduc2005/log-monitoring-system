#!/usr/bin/env python3
"""Send timestamp-free demo logs with generated chronological timestamps."""

import argparse
import datetime as dt
import json
import os
from pathlib import Path
import threading
import sys
import time
import urllib.error
import urllib.request
import uuid


ROOT = Path(__file__).resolve().parent
LOG_ROOT = ROOT / "logs"

SCENARIO_FILES = {
    "normal": {
        "payment-service": LOG_ROOT / "normal" / "payment-service.log",
        "order-service": LOG_ROOT / "normal" / "order-service.log",
    },
    "timeout": {
        "payment-service": LOG_ROOT / "incidents" / "payment-timeout.log",
    },
    "security": {
        "payment-service": LOG_ROOT / "incidents" / "payment-security.log",
    },
    "error-flood": {
        "order-service": LOG_ROOT / "incidents" / "order-error-flood.log",
    },
    "resource": {
        "payment-service": LOG_ROOT / "incidents" / "payment-resource.log",
    },
    "access-denied": {
        "order-service": LOG_ROOT / "incidents" / "order-access-denied.log",
    },
    "suspicious": {
        "payment-service": LOG_ROOT / "incidents" / "payment-suspicious.log",
    },
    "invoice": {
        "order-service": LOG_ROOT / "business" / "order-invoice-generation.log",
    },
    "inventory-mismatch": {
        "order-service": LOG_ROOT / "business" / "order-inventory-reservation.log",
    },
}

API_KEY_ENV = {
    "payment-service": "PAYMENT_SERVICE_API_KEY",
    "order-service": "ORDER_SERVICE_API_KEY",
}

DEFAULT_API_KEYS = {
    "payment-service": "lms_live_5pco54uva7.z_reT4WBUmZLWkJlHDytnh08AOCbwOuINPR7fbVOVA8",
    "order-service": "lms_live_nt8bjnd035.Wsf_0Bv-mz6l5eQ8wy78VxeNKQX9sq3NjrVUF3-3xxk",
}


def load_dotenv():
    env_path = ROOT.parent / ".env"
    if not env_path.exists():
        return
    for line in env_path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


def parse_args():
    parser = argparse.ArgumentParser(description="Send demo logs with generated timestamps.")
    parser.add_argument("--base-url", default="http://localhost:8080/api/v1")
    parser.add_argument("--scenario", choices=sorted(SCENARIO_FILES), default="normal")
    parser.add_argument("--service", default="all")
    parser.add_argument("--batch-size", type=int, default=10)
    parser.add_argument("--repeat", type=int, default=1)
    parser.add_argument("--forever", action="store_true")
    parser.add_argument("--line-step-ms", type=int, default=1000)
    parser.add_argument("--loop-delay-ms", type=int, default=3000)
    return parser.parse_args()


def read_lines(path):
    if not path.exists():
        raise RuntimeError(f"Log file not found: {path}")
    return [
        line.strip()
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.strip().startswith("#")
    ]


def chunks(values, size):
    if size <= 0:
        raise RuntimeError("--batch-size must be greater than 0")
    for index in range(0, len(values), size):
        yield values[index:index + size]


def iso_timestamp(value):
    return value.isoformat(timespec="milliseconds").replace("+00:00", "Z")


def with_timestamps(lines, start_at, step_ms):
    step = dt.timedelta(milliseconds=step_ms)
    return [
        f"{iso_timestamp(start_at + (step * index))} {line}"
        for index, line in enumerate(lines)
    ]


def api_key_for(service):
    env_name = API_KEY_ENV[service]
    api_key = os.environ.get(env_name) or DEFAULT_API_KEYS.get(service)
    if not api_key:
        raise RuntimeError(f"Missing {env_name} for {service}")
    return api_key


def post_batch(base_url, service, api_key, raw_logs, idempotency_key):
    payload = json.dumps({
        "applicationName": service,
        "rawLogs": raw_logs,
    }).encode("utf-8")
    request = urllib.request.Request(
        f"{base_url.rstrip('/')}/logs/batch",
        data=payload,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json",
            "X-API-Key": api_key,
            "Idempotency-Key": idempotency_key,
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            body = response.read().decode("utf-8")
            return json.loads(body) if body else {}
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {error.code}: {body}") from error
    except urllib.error.URLError as error:
        raise RuntimeError(f"Unable to reach backend: {error}") from error


def selected_files(args):
    scenario = SCENARIO_FILES[args.scenario]
    if args.service == "all":
        return scenario
    if args.service not in scenario:
        available = ", ".join(sorted(scenario))
        raise RuntimeError(
            f"Scenario '{args.scenario}' does not support service '{args.service}'. "
            f"Available: {available}"
        )
    return {args.service: scenario[args.service]}


def send_service(args, service, path, round_number, start_at):
    total = 0
    lines = read_lines(path)
    
    if len(lines) > 0 and len(lines) < args.batch_size:
        multiplier = (args.batch_size // len(lines)) + 1
        lines = (lines * multiplier)[:args.batch_size]

    stamped = with_timestamps(lines, start_at, args.line_step_ms)
    api_key = api_key_for(service)
    prefix = f"demo-{args.scenario}-{service}-{round_number}-{uuid.uuid4()}"
    for batch_index, batch in enumerate(chunks(stamped, args.batch_size), start=1):
        key = f"{prefix}-b{batch_index}"
        post_batch(args.base_url, service, api_key, batch, key)
        total += len(batch)
        print(
            f"[{args.scenario}] {service} round={round_number} "
            f"batch={batch_index} sent={len(batch)}"
        )
    return total


def send_once(args, files, round_number):
    if len(files) == 1:
        service, path = next(iter(files.items()))
        return send_service(args, service, path, round_number, dt.datetime.now(dt.timezone.utc))

    results = {}
    errors = {}
    threads = []
    start_at = dt.datetime.now(dt.timezone.utc)

    def run(service, path):
        try:
            results[service] = send_service(args, service, path, round_number, start_at)
        except RuntimeError as error:
            errors[service] = str(error)

    for service, path in files.items():
        thread = threading.Thread(target=run, args=(service, path), name=f"demo-{service}")
        thread.start()
        threads.append(thread)

    for thread in threads:
        thread.join()

    if errors:
        details = "; ".join(f"{service}: {message}" for service, message in sorted(errors.items()))
        raise RuntimeError(details)
    return sum(results.values())


def main():
    load_dotenv()
    args = parse_args()
    files = selected_files(args)
    round_number = 1
    total = 0
    try:
        while args.forever or round_number <= args.repeat:
            total += send_once(args, files, round_number)
            round_number += 1
            if args.forever or round_number <= args.repeat:
                time.sleep(args.loop_delay_ms / 1000)
    except KeyboardInterrupt:
        print("\nStopped by user.")
        return 0
    except RuntimeError as error:
        print(f"Error: {error}", file=sys.stderr)
        return 1
    print(f"Done. Total sent: {total}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
