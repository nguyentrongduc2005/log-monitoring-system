# Log Monitoring Frontend

Frontend React hiện giữ kiến trúc feature-based đang có; việc thay đổi kiến
trúc backend không làm thay đổi cấu trúc frontend.

```text
src/
├── app       # Router, providers và application wiring
├── features  # Feature UI và behavior
├── shared    # Component, utility và primitive dùng chung
└── api       # Transport client và generated API types
```

## Phạm vi bắt buộc

- Live Stream View hiển thị log liên tục không reload.
- Lọc nhanh theo application và log level.
- Hiển thị alert `ERROR`/`CRITICAL` qua WebSocket.
- Engineer chỉ xem application được cấp quyền.
- Admin quản lý application access và ngưỡng alert.
- Search/history dùng HTTP API; live update dùng WebSocket.

Analytics sức khỏe ứng dụng, retention UI và AI insight là phạm vi điểm
cộng/tương lai, không chặn luồng demo bắt buộc.

## Trạng thái hiện tại

Frontend vẫn đang ở giai đoạn scaffold. Các mục trên mô tả target behavior,
không khẳng định màn hình hoặc WebSocket flow đã được triển khai.

## Commands

Chạy từ repository root:

```sh
make frontend
make lint
make api
```

Không chỉnh sửa API types generated bằng tay. Backend vẫn phải enforce
authorization; ẩn UI ở frontend không thay thế kiểm tra quyền server-side.
