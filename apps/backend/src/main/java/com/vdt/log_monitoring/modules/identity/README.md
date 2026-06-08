# Identity Module Summary

## Mục tiêu module

Module `identity` chịu trách nhiệm:

- Quản lý tài khoản người dùng và xác thực.
- Phân biệt vai trò `ADMIN` và `ENGINEER`.
- Quản lý trạng thái tài khoản (`ACTIVE`, `DISABLED`, `LOCKED`).
- Hỗ trợ cơ chế xác thực JWT Stateless và quay vòng token (Refresh Token Rotation) bằng Redis.
- Phân tách và đảm bảo ranh giới giao tiếp giữa các module thông qua `IdentityFacade`.

---

## Cấu trúc thư mục & Package hiện tại

Cấu trúc mã nguồn hiện tại của module Identity và các thành phần liên quan:

```text
com/vdt/log_monitoring/
├── api/identity/                                # Tầng API (Controller & Exception Handler)
│   ├── dto/                                     # Data Transfer Objects (DTOs)
│   │   ├── ChangePasswordRequest.java
│   │   ├── CreateUserRequest.java
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── RefreshTokenRequest.java
│   │   ├── UpdateUserRequest.java
│   │   └── UserResponse.java
│   ├── AuthenticationController.java
│   ├── UserController.java
│   └── IdentityExceptionHandler.java            # Bắt lỗi cục bộ cho module Identity
│
├── modules/identity/                            # Logic nội bộ của Module Identity
│   ├── api/                                     # Public Facade Interfaces (Hợp đồng Module)
│   │   └── IdentityFacade.java
│   ├── application/                             # Tầng Nghiệp vụ (Services & Facade Impl)
│   │   ├── AuthService.java
│   │   ├── IdentityException.java               # Danh sách mã lỗi của Module
│   │   ├── IdentityFacadeImpl.java
│   │   ├── TokenPair.java
│   │   └── UserService.java
│   ├── infrastructure/persistence/              # Tầng dữ liệu (Repository & Entity)
│   │   ├── UserEntity.java
│   │   └── UserRepository.java
│   ├── model/                                   # Domain Model & Enums
│   │   ├── User.java
│   │   ├── UserRole.java
│   │   └── UserStatus.java
│   └── README.md                                # Tài liệu này
│
└── shared/                                      # Thành phần chia sẻ dùng chung kỹ thuật
    ├── dto/
    │   └── ApiResponse.java                     # Bọc API chuẩn { success, message, data, timestamp }
    ├── exceptions/
    │   └── GlobalExceptionHandler.java          # Bắt lỗi toàn cục (Validation, General Exception)
    └── security/                                # Cấu hình bảo mật hệ thống
        ├── JwtAuthenticationFilter.java
        ├── JwtTokenProvider.java
        └── SecurityConfig.java
```

---

## Các tính năng ĐÃ hoàn thành

### 1. Cơ sở dữ liệu & Entity Mapping
- **PostgreSQL Flyway Migration**: Bảng `users` được tách biệt chạy trên schema `identity` thông qua script `V1__create_users_table.sql`. Thiết lập unique index trên cột email không phân biệt hoa thường và check constraint nghiêm ngặt.
- **JPA Entity Mapping**: Ánh xạ `UserEntity` khớp chính xác với schema `identity.users`.
- **Domain Model**: Tách biệt `User` model nghiệp vụ để xử lý các logic mã hóa mật khẩu, đổi thông tin cá nhân và kiểm tra trạng thái tài khoản độc lập với JPA Entity.

### 2. Tầng Nghiệp vụ (Application Service & Facade)
- **UserService**: Quản lý đầy đủ logic CRUD người dùng, kiểm tra email trùng lặp, chuẩn hóa email và chuyển đổi quyền.
- **AuthService**: Thực hiện xác thực thông tin tài khoản, mã hóa mật khẩu sử dụng BCrypt, sinh cặp Token và xử lý vòng đời Refresh Token.
- **Refresh Token Rotation (Redis)**: Sử dụng Redis để quản lý và quay vòng Refresh Token có thời hạn (TTL 7 ngày) giúp chống các cuộc tấn công chiếm quyền và kiểm soát phiên làm việc.
- **IdentityFacade**: Cung cấp giao diện trao đổi dữ liệu an toàn cho các module khác. Các phương thức tìm kiếm (`findUserById`, `findUserByEmail`) trả về `Optional<UserDto>` nhằm cô lập hoàn toàn `IdentityException` nội bộ, ngăn ngừa vi phạm ranh giới module.

### 3. Bảo mật & Xác thực (Security Infrastructure)
- **Spring Security Configuration**: Chuyển đổi toàn bộ API sang cơ chế Stateless REST APIs sử dụng JWT.
- **JWT Provider**: Khởi tạo, parse và validate các mã token HMAC-SHA256 an toàn. Các tham số JWT Secret và Expiration được cấu hình động qua `application.yaml`.
- **JWT Filter**: Interceptor kiểm tra HTTP Authorization Header, giải mã token và đưa đối tượng principal vào Context.

### 4. Giao tiếp API & Bắt lỗi cục bộ
- **API Response Wrapper**: Tất cả controller (`AuthenticationController` và `UserController`) trả về các kiểu dữ liệu được bọc bởi `ApiResponse<T>`, đồng bộ hóa cấu trúc JSON cho frontend.
- **IdentityExceptionHandler**: Cấu hình `@Order(Ordered.HIGHEST_PRECEDENCE)` để bắt riêng biệt `IdentityException` và trả về mã lỗi cụ thể (như `USER_NOT_FOUND`, `INVALID_CREDENTIALS`) nằm trong trường `data` của API Response kèm theo HTTP status code tương ứng (400, 401, 403, 404).

---

## Các tính năng CHƯA hoàn thành (Roadmap Gaps & Future Backlog)

### 1. Quản lý Đăng xuất & Thu hồi Token chủ động (Token Revocation)
- **Hiện tại**: JWT access token là stateless, chỉ tự hết hạn. Refresh token được xoá khỏi Redis khi gọi cấp lại, tuy nhiên chưa có endpoint `/logout` để chủ động hủy (blacklist) Access Token trước thời hạn trên Redis.
- **Kế hoạch**: Bổ sung endpoint `/logout` cấu hình lưu các token bị thu hồi vào Redis Blacklist trong thời gian tồn tại còn lại của nó.

### 2. Chính sách Khóa tài khoản khi nhập sai mật khẩu (Failed-login Policy)
- **Hiện tại**: Nhập sai pass chỉ ném lỗi `INVALID_CREDENTIALS`. Chưa có bộ đếm tự động khóa tài khoản tạm thời.
- **Kế hoạch**: Sử dụng Redis để đếm số lần đăng nhập sai của một email (ví dụ: sai quá 5 lần trong 10 phút thì chuyển status thành `LOCKED` tạm thời).

### 3. Kiểm thử Tích hợp (Integration Tests với Testcontainers)
- **Hiện tại**: Đã chạy thành công unit test kiểm tra logic nghiệp vụ và context load.
- **Kế hoạch**: Viết thêm kiểm thử tích hợp thực tế sử dụng `Testcontainers` giả lập PostgreSQL và Redis độc lập để kiểm tra cơ chế phân quyền, lọc JWT, và tự động thu hồi token.

---

## Quy tắc thiết kế module đã chốt

1. **Ranh giới Module**: Không một module nào được phép import trực tiếp Entity, Repository, Domain model, hay Exception của module `identity`. Tất cả các thao tác đều bắt buộc gọi qua `IdentityFacade`.
2. **Exception Handling**: Các checked/unchecked exceptions phát sinh từ logic nghiệp vụ của module `identity` khi trôi ra ngoài controller sẽ được Handler cục bộ bắt và ánh xạ thành schema `ApiResponse` chứa mã lỗi nghiệp vụ riêng để frontend xử lý.
3. **Response DTO**: Không bao giờ trả về trực tiếp `UserEntity` hoặc `passwordHash` ra ngoài Client. Luôn ánh xạ qua `UserResponse`.
