# StockPulse — Week 1 Remaining Tasks

Nguồn: mục **Complete Requirement Traceability** và các story tương ứng trong `_bmad-output/planning-artifacts/epics.md`.

Phạm vi của tài liệu này chỉ gồm các requirement Week 1 (`REQ-STP-*-1XX`) chưa hoàn thành hoặc mới hoàn thành một phần. Không triển khai lại các chức năng baseline đã hoàn thành.

## 1. Phân quyền Product, Category và Warehouse

**Requirement:** `REQ-STP-B-106`  
**Story:** 1.1 — Enforce the catalog and warehouse authorization matrix  
**Trạng thái:** `PARTIALLY_IMPLEMENTED`

- [ ] Thêm `STAFF` và `MANAGER` vào role model dạng string, đồng thời giữ tương thích với `ADMIN` và `USER` hiện có.
- [ ] Bật method-level authorization.
- [ ] Cho phép `STAFF`, `MANAGER`, `ADMIN` xem danh sách và chi tiết Product/Category.
- [ ] Chỉ cho phép `MANAGER`, `ADMIN` tạo và cập nhật Product/Category.
- [ ] Cho phép `STAFF`, `MANAGER`, `ADMIN` xem danh sách và chi tiết Warehouse.
- [ ] Chỉ cho phép `ADMIN` tạo và cập nhật Warehouse.
- [ ] Bảo đảm request chưa đăng nhập trả về HTTP 401 theo error envelope hiện có.
- [ ] Bảo đảm user đã đăng nhập nhưng không đủ quyền trả về HTTP 403 theo error envelope hiện có.
- [ ] Viết authorization integration tests cho `USER`, `STAFF`, `MANAGER`, `ADMIN` và trường hợp chưa đăng nhập.

## 2. Hoàn thiện Flyway migrations V3–V7

**Requirement:** `REQ-STP-B-107`  
**Story:** 2.1 — Add the remaining StockPulse schema migrations  
**Trạng thái:** `IMPLEMENTED`

- [x] Giữ nguyên V1 và V2; không chỉnh sửa migration đã phát hành.
- [x] Tạo V3 cho `stock_levels`.
- [x] Tạo V4 cho `stock_movements` và `stock_movement_items`.
- [x] Tạo V5 cho `stock_alerts` và `reorder_suggestions`.
- [x] Tạo V6 cho indexes, Product search-vector backfill và cơ chế duy trì `search_vector` khi insert/update.
- [x] Tạo V7 cho `mv_stock_summary` và unique index phục vụ concurrent refresh.
- [x] Đồng bộ enum dạng string, foreign keys, unique constraints, timestamps, version và quantity constraints giữa SQL và JPA entities.
- [x] Kiểm tra database PostgreSQL 16 mới có thể migrate sạch từ V1 đến V7.
- [x] Kiểm tra database đang ở V2 có thể migrate tiếp mà không mất dữ liệu.
- [x] Kiểm tra Flyway validation thành công sau khi restart.
- [x] Viết integration test cho migrations, constraints, indexes, search trigger, materialized view và repeatable seed.

## 3. Kích hoạt PostgreSQL full-text search cho Product

**Requirement:** `REQ-STP-B-105`  
**Story:** 1.2 — Activate PostgreSQL product full-text search  
**Trạng thái:** `PARTIALLY_IMPLEMENTED`  
**Phụ thuộc:** Hoàn thành V6 trong Story 2.1.

- [ ] Backfill `products.search_vector` từ SKU và Product name cho dữ liệu hiện có.
- [ ] Tự động cập nhật `search_vector` khi Product được insert hoặc update.
- [ ] Tạo GIN index theo specification.
- [ ] Chuyển search path hiện có sang `ProductRepository.searchByVector` mà không viết lại Product CRUD.
- [ ] Chuẩn hóa và xử lý search input an toàn.
- [ ] Giữ response contract `ProductSummaryResponse` có pagination hiện tại.
- [ ] Trả về HTTP 400 theo error envelope hiện có khi search text rỗng.
- [ ] Viết unit tests cho validation và service delegation đã thay đổi.
- [ ] Viết PostgreSQL integration tests cho backfill, insert/update maintenance, matching, no-match và pagination.

## 4. Bổ sung cột còn thiếu trong Product list

**Requirement:** `REQ-STP-F-101`  
**Story:** 1.3 — Complete required Product list columns  
**Trạng thái:** `PARTIALLY_IMPLEMENTED`

- [ ] Bổ sung `minStock` và `reorderPoint` vào Product list model/response nếu hiện chưa có.
- [ ] Hiển thị hai cột **Min Stock** và **Reorder Point** trong bảng Product hiện tại.
- [ ] Giữ nguyên SKU, Name, Category, Unit, Status, Actions và pagination hiện có.
- [ ] Giữ response envelope `ApiResponse<PageResponse<ProductSummaryResponse>>`.
- [ ] Bổ sung mapper/service test nếu backend response thay đổi.
- [ ] Viết Angular component test cho hai cột mới, giá trị, pagination và error state.

## 5. Thay category ID input bằng dropdown

**Requirement:** `REQ-STP-F-102`  
**Story:** 1.4 — Replace Product category ID entry with a dropdown  
**Trạng thái:** `COMPLETED`
**Phụ thuộc:** Category list API và Story 1.1.

- [x] Thay numeric category input bằng Angular Material select.
- [x] Tạo hoặc tái sử dụng typed Category model/service để đọc Category API có pagination.
- [x] Vẫn gửi selected category ID theo Product request contract hiện có.
- [x] Giữ nguyên các field, validation và create/edit branching của Product form.
- [x] Giữ SKU ở trạng thái disabled khi edit.
- [x] Xử lý loading, empty và API-error states của Category options.
- [x] Không cho submit khi reference data bắt buộc chưa sẵn sàng.
- [x] Preselect đúng Category khi edit, kể cả Category nằm ở page tiếp theo của API.
- [x] Viết Angular service test cho response-envelope parsing.
- [x] Viết component tests cho loading, preselection, validation, payload ID, empty/error và create/edit behavior.

## 6. Thêm Warehouse list, route và sidebar navigation

**Requirements:** `REQ-STP-F-103`, `REQ-STP-F-104`  
**Story:** 1.5 — Add the Warehouse list route and navigation  
**Trạng thái:** `NOT_IMPLEMENTED` / `PARTIALLY_IMPLEMENTED`  
**Phụ thuộc:** Warehouse API và Story 1.1.

- [ ] Tạo typed Warehouse model và service dưới `features/warehouses`.
- [ ] Tạo standalone `WarehouseListComponent` sử dụng Warehouse API có pagination hiện tại.
- [ ] Hiển thị Name, Code, Address và Status.
- [ ] Hỗ trợ loading, empty, error và server-side pagination.
- [ ] Hỗ trợ page size 10, 20 và 50.
- [ ] Thêm protected lazy route cho Warehouse dưới `MainLayoutComponent`.
- [ ] Thêm Warehouse link vào sidebar cho các StockPulse role đã đăng nhập.
- [ ] Không thêm Warehouse create/edit form ngoài phạm vi requirement.
- [ ] Viết Angular service tests cho pagination và error response.
- [ ] Viết component tests cho loading, empty, content, error và paginator.
- [ ] Viết route smoke test.

## 7. Hoàn thiện và xác minh Swagger/OpenAPI

**Requirement:** `REQ-STP-T-103`  
**Story:** 5.2 — Complete and verify StockPulse OpenAPI documentation  
**Trạng thái:** `PARTIALLY_IMPLEMENTED`  
**Phụ thuộc:** Các endpoint Week 1 liên quan phải hoàn thành trước.

- [ ] Bổ sung OpenAPI annotations còn thiếu cho các endpoint StockPulse Week 1.
- [ ] Mô tả request constraints, roles/security, pagination và filter parameters.
- [ ] Mô tả success envelope và các response 400/401/403/404/409 phù hợp.
- [ ] Kiểm tra Swagger UI hiển thị đúng method, path và schema của các endpoint đã triển khai.
- [ ] Không document endpoint chưa tồn tại như một endpoint khả dụng.
- [ ] Tạo checklist xác minh lặp lại được hoặc automated OpenAPI assertions.
- [ ] Viết OpenAPI JSON assertions cho paths, operations, security và schemas.
- [ ] Ghi lại kết quả Swagger UI smoke verification.

## Requirements đã hoàn thành — không tạo lại

- [x] `REQ-STP-B-101` — Warehouse entity và Flyway V2.
- [x] `REQ-STP-B-102` — Category entity, self-reference và Flyway V2.
- [x] `REQ-STP-B-103` — Product entity và Flyway V2.
- [x] `REQ-STP-B-104` — Warehouse/Category services, CRUD và tests baseline.
- [x] `REQ-STP-T-101` — Product service unit tests.
- [x] `REQ-STP-T-102` — Warehouse service unit tests.

## Definition of Done cho Week 1

- [ ] Tất cả checklist chưa hoàn thành phía trên đã được xử lý hoặc có lý do trì hoãn được ghi rõ.
- [ ] Backend unit/integration tests liên quan đều pass.
- [ ] Angular tests và production build đều pass.
- [ ] Flyway migration được kiểm tra trên PostgreSQL 16 sạch và database nâng cấp từ V2.
- [ ] Swagger/OpenAPI phản ánh đúng các endpoint thực sự đã triển khai.
- [ ] Mỗi thay đổi được liên kết lại với đúng `REQ-STP-*-1XX` và story tương ứng.
