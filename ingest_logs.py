#!/usr/bin/env python3
"""Send log lines from multiple files to the Log Monitoring ingestion API in parallel.

Examples:
  python3 ingest_logs.py
  python3 ingest_logs.py --repeat 10
  python3 ingest_logs.py --forever
"""

import argparse
import json
import sys
import time
import random
import urllib.error
import urllib.request
import uuid
import re
import datetime
import threading
from pathlib import Path

# ==============================================================================
# CONFIGURATION
# ==============================================================================
# Thêm API key thật của bạn tương ứng với các app vào đây
API_KEYS = {
    "payment-service": "lms_live_42aors41c3.ZbIsJtFQj-3uwKj-blS-iLlRraQtP5VbwwZG2QN7_eU",
    "order-service": "lms_live_4w535ep9w0.oLNMpHN4oXFYHiOyrHYfXAxLHiaNMZ3vvOIDTEL6fZw"
}

DEFAULT_BASE_URL = "http://localhost:8080/api/v1"

# Regex nhận diện Timestamp ISO (VD: 2026-06-15T09:05:31.014Z) ở đầu câu
TIMESTAMP_REGEX = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z")

def parse_args():
    parser = argparse.ArgumentParser(description="Parallel log ingestion script.")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL)
    parser.add_argument("--mode", choices=("batch", "single"), default="batch")
    parser.add_argument("--repeat", default=1, type=int)
    parser.add_argument("--forever", action="store_true")
    return parser.parse_args()

def read_log_lines(path):
    if not path.exists():
        print(f"Log file does not exist: {path}", file=sys.stderr)
        return []
    lines = [line.rstrip("\n") for line in path.read_text(encoding="utf-8").splitlines()]
    return [line for line in lines if line.strip()]

def chunks(values, size):
    for index in range(0, len(values), size):
        yield values[index : index + size]

def make_request(url, api_key, payload, idempotency_key):
    body = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=body,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json",
            "X-API-Key": api_key,
        },
    )
    if idempotency_key:
        request.add_header("Idempotency-Key", idempotency_key)
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            response_body = response.read().decode("utf-8")
            return json.loads(response_body) if response_body else {}
    except urllib.error.HTTPError as error:
        response_body = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Request failed with HTTP {error.code}: {response_body}") from error
    except urllib.error.URLError as error:
        raise RuntimeError(f"Unable to reach backend: {error}") from error

def update_timestamp(line):
    # Thay thế timestamp tĩnh trong log mẫu bằng timestamp hiện tại để giống real-time
    current_time = datetime.datetime.utcnow().isoformat(timespec='milliseconds') + "Z"
    return TIMESTAMP_REGEX.sub(current_time, line)

def next_idempotency_key(prefix, round_number, request_number):
    return f"{prefix}-r{round_number}-q{request_number}-{uuid.uuid4()}"

def run_for_app(app_name, api_key, args):
    log_file = Path(f"{app_name}.log")
    lines = read_log_lines(log_file)
    if not lines:
        print(f"[{app_name}] No logs found in {log_file.name}. Exiting thread.")
        return

    url_batch = f"{args.base_url.rstrip('/')}/logs/batch"
    url_single = f"{args.base_url.rstrip('/')}/logs"
    idempotency_prefix = f"ingest-{app_name}-{uuid.uuid4()}"

    total = 0
    round_number = 1
    try:
        while args.forever or round_number <= args.repeat:
            # Randomize số lượng log mỗi batch (1-100) và delay (200-2000ms) để giống môi trường thực tế
            batch_size = random.randint(1, 100) if args.mode == "batch" else 1
            delay_ms = random.uniform(200, 2000)

            # Cập nhật thời gian động cho tất cả các dòng log trong vòng lặp này
            dynamic_lines = [update_timestamp(line) for line in lines]

            if args.mode == "batch":
                for index, batch in enumerate(chunks(dynamic_lines, batch_size), start=1):
                    make_request(
                        url=url_batch,
                        api_key=api_key,
                        payload={"applicationName": app_name, "rawLogs": batch},
                        idempotency_key=next_idempotency_key(idempotency_prefix, round_number, index),
                    )
                    total += len(batch)
                    print(f"[{app_name}] round {round_number} batch {index}: sent {len(batch)} logs (delay {int(delay_ms)}ms)")
                    time.sleep(delay_ms / 1000)
            else:
                for index, line in enumerate(dynamic_lines, start=1):
                    make_request(
                        url=url_single,
                        api_key=api_key,
                        payload={"applicationName": app_name, "rawLog": line},
                        idempotency_key=next_idempotency_key(idempotency_prefix, round_number, index),
                    )
                    total += 1
                    print(f"[{app_name}] round {round_number} single {index}: sent (delay {int(delay_ms)}ms)")
                    time.sleep(delay_ms / 1000)

            round_number += 1
    except Exception as e:
        print(f"[{app_name}] Error: {e}", file=sys.stderr)

    print(f"[{app_name}] Done. Total sent: {total}")

def main():
    args = parse_args()
    threads = []
    
    print("Starting parallel log ingestion. Press Ctrl+C to stop.\n")
    for app_name, api_key in API_KEYS.items():
        if api_key.startswith("your_api_key"):
            print(f"⚠️ [WARNING] You are using dummy API key for {app_name}. Please update API_KEYS in the script.")
            
        t = threading.Thread(target=run_for_app, args=(app_name, api_key, args))
        t.daemon = True
        t.start()
        threads.append(t)
        
    try:
        while any(t.is_alive() for t in threads):
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nStopped by user.")
        return 0

    print("\nAll threads finished.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
