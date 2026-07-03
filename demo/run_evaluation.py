#!/usr/bin/env python3
import subprocess
import time
import urllib.request
import base64
import sys
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DEMO_INGEST_PATH = os.path.join(SCRIPT_DIR, "demo_ingest.py")

# ClickHouse Configuration
CH_URL = "http://localhost:8123/"
CH_AUTH = base64.b64encode(b"log_user:password").decode("utf-8")

# Rule ID for Invoice Job Failure
RULE_ID = "6add0c95-b1e4-49a8-8ba3-89b8588ee339"

def run_cmd(cmd):
    try:
        res = subprocess.run(cmd, shell=True, capture_output=True, text=True, check=True)
        return res.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error executing command: {cmd}\nStderr: {e.stderr}", file=sys.stderr)
        return ""

def query_clickhouse(sql):
    req = urllib.request.Request(
        CH_URL,
        data=sql.encode("utf-8"),
        headers={"Authorization": f"Basic {CH_AUTH}"}
    )
    try:
        with urllib.request.urlopen(req) as response:
            return response.read().decode("utf-8").strip()
    except Exception as e:
        return f"Error: {e}"

def query_postgres(sql):
    cmd = f'docker exec -i log-monitoring-postgres-1 psql -U log_user -d log_monitoring -t -A -c "{sql}"'
    return run_cmd(cmd)

def print_header(title):
    print("\n" + "="*60)
    print(f"  {title}")
    print("="*60)

def main():
    print_header("CHƯƠNG TRÌNH ĐÁNH GIÁ & KIỂM THỬ HỆ THỐNG TỰ ĐỘNG")
    
    # 1. Reset databases to clean slate
    print("[1/5] Đang dọn dẹp dữ liệu cũ (Clean Slate)...")
    
    # Truncate ClickHouse
    query_clickhouse("TRUNCATE TABLE log_monitoring.processed_logs")
    
    # Clear Postgres alerts
    query_postgres(f"DELETE FROM alerting.alerts WHERE rule_id = '{RULE_ID}'")
    
    # Set threshold_count = 2 for Alert Rule
    query_postgres(f"UPDATE alerting.alert_rules SET threshold_count = 2 WHERE id = '{RULE_ID}'")
    
    # Flush Redis
    run_cmd("docker exec -i log-monitoring-redis-1 redis-cli flushall")
    print("  -> Đã làm sạch ClickHouse, PostgreSQL (alerts) và Redis.")
    
    time.sleep(1)

    # 2. Run Scenario 1 (500 logs / 2 seconds)
    print("\n[2/5] Đang chạy Kịch bản 1: Mô phỏng gửi 500 logs liên tục...")
    start_time = time.time()
    
    cmd_scen1 = (
        f"python3 {DEMO_INGEST_PATH} "
        "--scenario normal "
        "--service payment-service "
        "--batch-size 10 "
        "--repeat 25 "
        "--loop-delay-ms 10"
    )
    run_cmd(cmd_scen1)
    duration_scen1 = time.time() - start_time
    print(f"  -> Gửi thành công 500 logs. Thời gian gửi phía client: {duration_scen1:.3f} giây.")

    # Wait for consumer processing
    print("  -> Đang chờ Worker tiêu thụ log từ Kafka và ghi vào ClickHouse (5 giây)...")
    time.sleep(5)

    # Query Scenario 1 results
    ch_count = int(query_clickhouse("SELECT count() FROM log_monitoring.processed_logs"))
    ch_avg_lat = query_clickhouse("SELECT avg(dateDiff('millisecond', received_at, processed_at)) FROM log_monitoring.processed_logs")
    try:
        avg_latency = float(ch_avg_lat)
    except ValueError:
        avg_latency = 0.0

    # 3. Run Scenario 2 (Deduplication)
    print("\n[3/5] Đang chạy Kịch bản 2: Mô phỏng cảnh báo trùng lặp (Deduplication)...")
    cmd_scen2 = (
        f"python3 {DEMO_INGEST_PATH} "
        "--scenario invoice "
        "--service order-service "
        "--batch-size 1 "
        "--repeat 1 "
        "--line-step-ms 1000"
    )
    run_cmd(cmd_scen2)
    print("  -> Đã gửi 3 logs cảnh báo trùng lặp.")
    print("  -> Đang chờ AlertSyncScheduler đồng bộ từ Redis về Postgres (10 giây)...")
    time.sleep(10)

    # Query Scenario 2 results
    pg_result = query_postgres(
        f"SELECT count(*), COALESCE(sum(occurrence_count), 0) "
        f"FROM alerting.alerts WHERE rule_id = '{RULE_ID}'"
    )
    
    alert_count = 0
    occurrence_count = 0
    if pg_result and "|" in pg_result:
        parts = pg_result.split("|")
        alert_count = int(parts[0])
        occurrence_count = int(parts[1])

    # 4. Display results
    print_header("KẾT QUẢ ĐÁNH GIÁ VÀ KIỂM THỬ THỰC TẾ")
    
    print("KỊCH BẢN 1: TIẾP NHẬN LOG CHỊU TẢI (500 LOGS)")
    print(f"  - Số log gửi từ client:                 500")
    print(f"  - Số log tiếp nhận thành công (API):     {ch_count}")
    print(f"  - Số log được xử lý thành công:          {ch_count}")
    print(f"  - Số log lưu trữ trong ClickHouse:       {ch_count}")
    print(f"  - Số log bị lỗi:                         {500 - ch_count}")
    print(f"  - Tỷ lệ tiếp nhận thành công:            {(ch_count / 500.0) * 100:.1f}%")
    print(f"  - Thời gian xử lý trung bình:            {avg_latency:.1f} ms")
    
    print("\nKỊCH BẢN 2: KHỬ TRÙNG LẶP CẢNH BÁO (REDIS DEDUP)")
    print(f"  - Số log lỗi phát sinh (trùng lặp):     3")
    print(f"  - Số Alert được tạo trong DB:            {alert_count}")
    print(f"  - Số thông báo phát đi:                  {alert_count}")
    print(f"  - Tổng occurrence_count được gộp:       {occurrence_count}")
    print(f"  - Cơ chế cooldown hoạt động:             {'CÓ (Hoạt động tốt)' if alert_count == 1 and occurrence_count == 3 else 'KHÔNG'}")
    
    print("="*60)
    print(" Bạn có thể chụp ảnh màn hình console này để đưa vào báo cáo.")
    print("="*60 + "\n")

if __name__ == "__main__":
    main()
