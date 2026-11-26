# Độc giả hỏi: luồng sao kê cuối ngày quản lý trạng thái và retry rối, thiết kế lại thế nào?

*Câu hỏi của bạn đọc, 2026-11-26. Bối cảnh: luồng sao kê cuối ngày (EOD) viết bằng Spring Boot, PostgreSQL, Kafka và JasperReports. Bản này là sơ đồ dựng lại từ mô tả của bạn đọc.*

Ký hiệu dùng trong bài:

- **`⚠ DWn`:** Điểm ghi kép (dual write), một bước ghi vào từ 2 hệ thống trở lên mà không có transaction chung.
- **"(chưa rõ)":** Chỗ mô tả của bạn đọc chưa nói tới, cần hỏi lại.

---

## 1. Bài toán

- **Nghiệp vụ:** Cuối ngày có một trigger chạy sao kê theo ngày giao dịch. Quan hệ 3 tầng: 1 Batch Job có nhiều khách hàng, mỗi khách hàng có nhiều tiểu khoản. Mỗi tiểu khoản render ra 1 file PDF.
- **Gửi email:** Khách hàng nào tạo xong 100% PDF thì gửi 1 email đính kèm tất cả file cho khách đó.
- **Khi lỗi:** Một tiểu khoản lỗi thì khách đó FAILED, không gửi email cho khách này. Các khách khác vẫn gửi bình thường.
- **Bảng `batch_job`:** Status PENDING → CALCULATED → PROCESSING_PDF → PROCESSING_EMAIL hoặc FAILED, kèm `total_customers`, `completed_customers`, `failed_customers`.
- **Bảng `customer_task`:** Status PENDING_PDF → READY_FOR_EMAIL hoặc FAILED, kèm `total_sub_accounts`, `completed_sub_accounts`, `failed_sub_accounts`.
- **Bảng `sub_account_task`:** Status PENDING → PROCESSING → COMPLETED hoặc FAILED.
- **Worker:** Kafka consumer 3 thread, `MANUAL_IMMEDIATE` ack, `max.poll.records=1`.

Ba vấn đề bạn đọc đang gặp:

- **Counter lệch:** Worker cộng trừ counter 2 chiều qua 3 bảng dưới đa luồng, dễ lệch khi retry hoặc khi Kafka giao trùng message.
- **Treo ở PROCESSING_PDF:** App crash giữa lúc dispatch để lại task PENDING, worker chết hoặc mất message để lại task PROCESSING, DB chập chờn đúng lúc ACK.
- **Thiếu cơ chế bảo vệ:** Chưa có watchdog quét task kẹt quá hạn, chưa giới hạn số lần retry, chưa có DLT.

---

## 2. Kiến trúc tổng thể

```mermaid
flowchart LR
    TR{{"topic trigger"}} --> DP

    subgraph APP["Spring Boot"]
        DP["Dispatcher<br/>consumer"]
        WK["Worker<br/>3 thread, max.poll.records=1<br/>MANUAL_IMMEDIATE"]
        EM["Email<br/>consumer"]
    end

    KW{{"topic worker<br/>1 msg = 1 tiểu khoản"}}
    KE{{"topic email<br/>1 msg = 1 khách"}}
    DB[("PostgreSQL<br/>SP + batch_job<br/>customer_task<br/>sub_account_task")]
    FS[("Kho file PDF")]
    MAIL(["Mail server"])

    DP -->|"gọi SP, insert task"| DB
    DP -->|"send"| KW
    KW --> WK
    WK -->|"ghi file"| FS
    WK -->|"status + counter 3 tầng"| DB
    WK -->|"send khi đủ tiểu khoản"| KE
    KE --> EM
    EM -->|"đọc file"| FS
    EM -->|"gửi"| MAIL
    EM -.->|"cập nhật trạng thái? (chưa rõ)"| DB
```

Số hệ thống mỗi thành phần phải ghi trong một lần xử lý:

- **Dispatcher:** PostgreSQL và Kafka.
- **Worker:** Kho file, PostgreSQL (3 bảng), Kafka topic email và Kafka ACK.
- **Email consumer:** Kho file, mail server và Kafka ACK, cộng PostgreSQL nếu có cập nhật trạng thái.

---

## 3. Luồng xử lý theo từng bước

### 3.1 Bước 1 (Dispatch) và Bước 4 (Retry)

```mermaid
flowchart TD
    A1(["Nhận trigger"]) --> A0{"Batch đang ở<br/>trạng thái nào?"}
    A0 -->|"chưa có"| A2["Gọi Stored Procedure<br/>batch = CALCULATED"]
    A0 -->|"FAILED hoặc PROCESSING_PDF"| R1
    A2 --> A3["batch = PROCESSING_PDF"]
    A3 --> A4["Query 1 trang khách + tiểu khoản"]
    A4 --> A5["Insert customer_task<br/>+ sub_account_task"]
    A5 --> A6["⚠ DW1: send 1 msg / tiểu khoản<br/>vào topic worker"]
    A6 --> A7{"Còn trang?"}
    A7 -->|"còn"| A4
    A7 -->|"hết"| A8["⚠ DW1: update batch.total_customers<br/>(chạy SAU cả vòng lặp)"]

    R1["Tìm sub_account_task FAILED<br/>(không tìm PENDING, PROCESSING)"] --> R2["1 transaction DB:<br/>sub = PENDING<br/>customer.failed_sub_accounts -= n<br/>batch.failed_customers -= 1<br/>status customer, batch có reset? (chưa rõ)"]
    R2 --> R3["⚠ DW4: send lại topic worker"]

    classDef warn fill:#fde2e2,stroke:#c0392b,color:#000
    class A6,A8,R3 warn
```

### 3.2 Bước 2 (Worker) và Bước 3 (Fan-in 2 tầng)

```mermaid
flowchart TD
    B1(["Poll 1 msg tiểu khoản"]) --> B2["sub = PROCESSING"]
    B2 --> B3["⚠ DW2: render Jasper<br/>lưu file PDF"]
    B3 --> B4{"Render OK?"}
    B4 -->|"OK"| B5["sub = COMPLETED<br/>customer.completed_sub_accounts += 1"]
    B4 -->|"lỗi"| B6["sub = FAILED<br/>customer.failed_sub_accounts += 1"]
    B5 --> C1
    B6 --> C1
    C1{"customer:<br/>completed + failed >= total?"}
    C1 -->|"chưa"| K
    C1 -->|"đủ"| C2{"failed = 0?"}
    C2 -->|"có"| C3["customer = READY_FOR_EMAIL<br/>⚠ DW3: send topic email<br/>batch.completed_customers += 1"]
    C2 -->|"không"| C4["customer = FAILED<br/>batch.failed_customers += 1"]
    C3 --> D1
    C4 --> D1
    D1{"batch:<br/>completed + failed >= total_customers?"}
    D1 -->|"chưa"| K
    D1 -->|"đủ"| D2{"failed_customers = 0?"}
    D2 -->|"có"| D3["batch = PROCESSING_EMAIL"]
    D2 -->|"không"| D4["batch = FAILED"]
    D3 --> K
    D4 --> K
    K["⚠ DW2: ACK Kafka<br/>(B2 tới D4 chung 1 transaction hay nhiều? chưa rõ)"]

    classDef warn fill:#fde2e2,stroke:#c0392b,color:#000
    class B3,C3,K warn
```

---

## 4. Máy trạng thái của 3 bảng

Batch job:

```mermaid
stateDiagram-v2
    direction LR
    state "Kết thúc? (chưa có)" as BDONE
    [*] --> PENDING
    PENDING --> CALCULATED: SP xong
    CALCULATED --> PROCESSING_PDF: bắt đầu dispatch
    PROCESSING_PDF --> PROCESSING_EMAIL: đủ khách, 0 lỗi
    PROCESSING_PDF --> FAILED: đủ khách, có lỗi
    FAILED --> PROCESSING_PDF: retry (chưa rõ)
    PROCESSING_PDF --> PROCESSING_PDF: retry khi đang chạy
    PROCESSING_EMAIL --> BDONE: ai chuyển? (chưa rõ)
```

Customer task:

```mermaid
stateDiagram-v2
    direction LR
    state "Đã gửi mail? (chưa có)" as CSENT
    [*] --> PENDING_PDF
    PENDING_PDF --> READY_FOR_EMAIL: đủ tiểu khoản, 0 lỗi
    PENDING_PDF --> FAILED: đủ tiểu khoản, có lỗi
    FAILED --> PENDING_PDF: retry (chưa rõ)
    READY_FOR_EMAIL --> CSENT: email consumer (chưa rõ)
```

Sub-account task:

```mermaid
stateDiagram-v2
    direction LR
    [*] --> PENDING
    PENDING --> PROCESSING: worker nhận msg
    PROCESSING --> COMPLETED: render OK
    PROCESSING --> FAILED: render lỗi
    FAILED --> PENDING: retry
    PENDING --> PENDING: kẹt, dispatch crash trước khi send
    PROCESSING --> PROCESSING: kẹt, worker chết hoặc msg bị bỏ qua
```

---

## 5. Bản đồ ghi kép

Luồng này không dùng distributed transaction thật (2PC). Thay vào đó là 5 chỗ ghi kép, mỗi chỗ ghi nhiều hệ thống nối tiếp nhau, đứt ở giữa là lệch trạng thái.

| Điểm | Ghi vào | Đứt giữa chừng thì |
|---|---|---|
| DW1 Dispatch | DB insert task, Kafka send, DB update `total_customers` | Crash giữa vòng: task PENDING không có msg, retry chỉ tìm FAILED nên không ai nhặt. `total_customers` chưa set: fan-in batch so với 0 (chốt sớm) hoặc NULL (không bao giờ chốt) |
| DW2 Worker | File PDF, DB status và counter 3 tầng, Kafka ACK | Commit DB xong chưa ACK: msg giao lại, counter cộng thêm lần nữa. DB lỗi: error handler retry vài lần rồi bỏ qua msg, sub kẹt PROCESSING |
| DW3 Fan-in khách | DB status và counter batch, Kafka send email | DB commit rồi send lỗi: READY_FOR_EMAIL nhưng không có mail. Send rồi DB rollback: mail đi, status cũ, lần sau gửi nữa |
| DW4 Retry | DB reset và trừ counter, Kafka send | Send lỗi: sub PENDING không có msg. Msg cũ còn trong topic: tiểu khoản chạy 2 lần, counter lệch |
| DW5 Email | Kafka, file, mail server, DB (chưa rõ) | Gửi mail xong crash trước ACK: khách nhận 2 email |

---

## 6. Một ca cụ thể: msg trùng làm email gửi đi thiếu file

```mermaid
sequenceDiagram
    participant K as topic worker
    participant W1 as Worker thread 1
    participant W2 as Worker thread 2
    participant DB as customer_task (total = 3)
    participant E as topic email

    K->>W1: msg tiểu khoản A
    W1->>DB: A = COMPLETED, completed = 1
    Note over W1: rebalance / crash trước khi ACK
    K->>W2: giao lại msg A
    W2->>DB: A = COMPLETED lần 2, completed = 2
    K->>W1: msg tiểu khoản B
    W1->>DB: B = COMPLETED, completed = 3
    Note over DB: completed = total, nhưng C chưa render
    W1->>E: READY_FOR_EMAIL, gửi mail thiếu PDF của C
    Note over DB: C xong sau đó: completed = 4, fan-in chạy lần 2, batch.completed_customers cộng 2 lần
```

Phép `+1` không idempotent nên một message giao lại là đủ làm customer đạt `total` sớm. Điều kiện `>=` còn cho fan-in chạy thêm lần nữa khi C xong, Batch đếm khách này 2 lần và có thể chốt trong lúc khách khác chưa xong.
