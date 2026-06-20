#!/usr/bin/env python3
"""Send log lines from a file to the Log Monitoring ingestion API.

Examples:
  python3 ingest_logs.py --file ./app.log --application-name checkout-api --api-key "$LMS_API_KEY"
  python3 ingest_logs.py --file ./app.log --application-name checkout-api --api-key "$LMS_API_KEY" --mode single
  python3 ingest_logs.py --file ./app.log --application-name checkout-api --api-key "$LMS_API_KEY" --repeat 10
  python3 ingest_logs.py --file ./app.log --application-name checkout-api --api-key "$LMS_API_KEY" --forever
"""

from __future__ import annotations

import argparse
import json
import sys
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path
from typing import Iterable


DEFAULT_BASE_URL = "http://localhost:8080/api/v1"
MAX_BATCH_SIZE = 500


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Read a log file and ingest it through the log monitoring API."
    )
    parser.add_argument(
        "--file",
        required=True,
        type=Path,
        help="Path to the log file. Each non-empty line is sent as one raw log.",
    )
    parser.add_argument(
        "--application-name",
        required=True,
        help="Application name registered in the backend, e.g. checkout-api.",
    )
    parser.add_argument(
        "--api-key",
        required=True,
        help="Raw application API key. Sent as X-API-Key.",
    )
    parser.add_argument(
        "--base-url",
        default=DEFAULT_BASE_URL,
        help=f"Backend API base URL. Default: {DEFAULT_BASE_URL}",
    )
    parser.add_argument(
        "--mode",
        choices=("batch", "single"),
        default="batch",
        help="Use /logs/batch or /logs. Default: batch.",
    )
    parser.add_argument(
        "--batch-size",
        default=100,
        type=int,
        help=f"Number of log lines per batch. Max: {MAX_BATCH_SIZE}. Default: 100.",
    )
    parser.add_argument(
        "--idempotency-prefix",
        default=None,
        help="Optional Idempotency-Key prefix. Default: generated per run.",
    )
    parser.add_argument(
        "--repeat",
        default=1,
        type=int,
        help="Number of times to resend the whole file. Default: 1.",
    )
    parser.add_argument(
        "--forever",
        action="store_true",
        help="Keep resending the file until interrupted.",
    )
    parser.add_argument(
        "--sleep-ms",
        default=0,
        type=int,
        help="Optional delay between requests, useful for demo streaming.",
    )
    return parser.parse_args()


def read_log_lines(path: Path) -> list[str]:
    if not path.exists():
        raise SystemExit(f"Log file does not exist: {path}")

    lines = [line.rstrip("\n") for line in path.read_text(encoding="utf-8").splitlines()]
    return [line for line in lines if line.strip()]


def chunks(values: list[str], size: int) -> Iterable[list[str]]:
    for index in range(0, len(values), size):
        yield values[index : index + size]


def make_request(
    *,
    url: str,
    api_key: str,
    payload: dict[str, object],
    idempotency_key: str | None,
) -> dict[str, object]:
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
        raise RuntimeError(
            f"Request failed with HTTP {error.code}: {response_body}"
        ) from error
    except urllib.error.URLError as error:
        raise RuntimeError(f"Unable to reach backend: {error}") from error


def next_idempotency_key(prefix: str, round_number: int, request_number: int) -> str:
    return f"{prefix}-r{round_number}-q{request_number}-{uuid.uuid4()}"


def send_batch(
    args: argparse.Namespace,
    lines: list[str],
    idempotency_prefix: str,
    round_number: int,
) -> int:
    url = f"{args.base_url.rstrip('/')}/logs/batch"
    total = 0

    for index, batch in enumerate(chunks(lines, args.batch_size), start=1):
        payload = {
            "applicationName": args.application_name,
            "rawLogs": batch,
        }
        response = make_request(
            url=url,
            api_key=args.api_key,
            payload=payload,
            idempotency_key=next_idempotency_key(
                idempotency_prefix,
                round_number,
                index,
            ),
        )
        ingested = response.get("data", {}).get("ingestedCount", len(batch))
        total += int(ingested)
        print(f"round {round_number} batch {index}: accepted {ingested} logs")
        sleep(args.sleep_ms)

    return total


def send_single(
    args: argparse.Namespace,
    lines: list[str],
    idempotency_prefix: str,
    round_number: int,
) -> int:
    url = f"{args.base_url.rstrip('/')}/logs"

    for index, line in enumerate(lines, start=1):
        payload = {
            "applicationName": args.application_name,
            "rawLog": line,
        }
        make_request(
            url=url,
            api_key=args.api_key,
            payload=payload,
            idempotency_key=next_idempotency_key(
                idempotency_prefix,
                round_number,
                index,
            ),
        )
        print(f"round {round_number} log {index}: accepted")
        sleep(args.sleep_ms)

    return len(lines)


def sleep(milliseconds: int) -> None:
    if milliseconds > 0:
        time.sleep(milliseconds / 1000)


def main() -> int:
    args = parse_args()
    if args.batch_size < 1 or args.batch_size > MAX_BATCH_SIZE:
        raise SystemExit(f"--batch-size must be between 1 and {MAX_BATCH_SIZE}")
    if args.repeat < 1:
        raise SystemExit("--repeat must be at least 1")

    lines = read_log_lines(args.file)
    if not lines:
        print("No non-empty log lines found.")
        return 0

    idempotency_prefix = args.idempotency_prefix or f"ingest-file-{uuid.uuid4()}"
    try:
        total = 0
        round_number = 1
        while args.forever or round_number <= args.repeat:
            total += (
                send_batch(args, lines, idempotency_prefix, round_number)
                if args.mode == "batch"
                else send_single(args, lines, idempotency_prefix, round_number)
            )
            round_number += 1
    except RuntimeError as error:
        print(error, file=sys.stderr)
        return 1
    except KeyboardInterrupt:
        print("\nstopped by user")
        return 0

    print(f"done: accepted {total} logs from {args.file}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
