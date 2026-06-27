# Luong log, fingerprint, alert va incident hien tai

Tai lieu nay tom tat co che dang chay trong code hien tai cua backend. Muc tieu la lam ro log duoc xu ly va danh fingerprint nhu nao, alert su dung fingerprint ra sao, va incident hien tai bat dau tu dau.

## 1. Tong quan luong vao he thong

Luong chinh cua he thong:

1. Client gui raw log vao ingestion API.
2. Ingestion tao envelope/event cho log tho va dua vao luong xu ly log.
3. Module processing parse, normalize, enrich va gan fingerprint cho log.
4. Log da xu ly duoc luu vao ClickHouse bang `processed_logs`.
5. Neu log co level `ERROR` hoac `CRITICAL`, processing publish `CriticalLogDetectedEvent`.
6. Module alerting nhan event nay, so khop alert rule, tinh threshold/cooldown va tao hoac cap nhat alert.
7. Nguoi dung chu dong bam tao incident tu mot alert.
8. Module incident thu thap evidence lien quan roi goi AI phan tich.

Incident hien tai khong tu dong quet toan he thong de tao incident va khong con luong manual start khong co alert lam diem bat dau.

## 2. Xu ly log va luu `processed_logs`

Module processing xu ly log theo cac buoc chinh:

1. `LogParser` tach raw message thanh `level`, `message`, `traceId`, `logTimestamp`.
2. `LogNormalizer` tao `ProcessedLog` voi thong tin application, ingestion, thoi gian nhan va status ban dau.
3. `LogEnricher` bo sung thong tin can thiet cho log.
4. `LogFingerprinter` tao fingerprint cho log.
5. `LogWriter` ghi log da xu ly vao ClickHouse.
6. Sau khi luu, he thong publish realtime log va, neu can, critical alert event.

Bang ClickHouse `processed_logs` dang luu cac cot chinh:

- `event_id`
- `ingestion_id`
- `application_id`
- `application_name`
- `application_display_name`
- `level`
- `message`
- `trace_id`
- `log_timestamp`
- `received_at`
- `processed_at`
- `fingerprint`
- `status`

Bang nay dung `ReplacingMergeTree(processed_at)`, partition theo thang cua `log_timestamp`, va order theo `(application_id, log_timestamp, event_id)`.

## 3. Co che log fingerprint

`LogFingerprinter` tao fingerprint theo cong thuc:

```text
source = applicationId + "|" + level + "|" + normalizeMessage(message)
fingerprint = SHA-256(source)
```

`normalizeMessage(message)` hien lam cac viec:

- Thay UUID bang `{uuid}`.
- Thay chuoi so bang `{number}`.
- `strip()` va chuyen ve lowercase.

Vi du y nghia:

- `Order 123 failed for user 456`
- `Order 789 failed for user 999`

Sau normalize co the cung tro thanh:

```text
order {number} failed for user {number}
```

Nen hai log cung `applicationId`, cung `level`, cung dang message sau normalize se co cung fingerprint.

Quan trong: fingerprint cua log khong phai ID duy nhat cua mot log. No la khoa gom nhom cac log co hinh dang loi giong nhau trong cung application va cung level.

## 4. Khi nao log tao alert event

Trong `ProcessedLog`, ham `shouldPublishCriticalAlert()` tra ve true khi level la:

- `ERROR`
- `CRITICAL`

Khi do processing publish `CriticalLogDetectedEvent`. Event nay mang theo cac thong tin chinh:

- `eventId`
- `ingestionId`
- `applicationId`
- `applicationName`
- `applicationDisplayName`
- `traceId`
- `level`
- `message`
- `fingerprint`
- `logTimestamp`
- `detectedAt`

Fingerprint trong event la fingerprint da duoc tao o buoc processing. Alerting khong tu tinh lai fingerprint moi tu message.

## 5. Co che alert fingerprint

Module alerting nhan `CriticalLogDetectedEvent` va tao `AlertEvaluationCandidate`. Candidate giu nguyen fingerprint tu log.

Sau do `AlertEvaluationService`:

1. Lay cac rule dang active cua application.
2. Loc rule khop voi severity va message.
3. Goi `AlertThresholdCache.evaluate(rule, eventId, fingerprint, logTimestamp)`.
4. Neu threshold du dieu kien thi tao hoac cap nhat alert.

Redis threshold key duoc scope theo:

```text
ruleId + ":" + SHA-256(fingerprint)
```

Nghia la alert threshold gom theo cung rule va cung log fingerprint.

Khi alert duoc trigger, `AlertService` kiem tra alert active hien co bang:

```text
ruleId + fingerprint + status != RESOLVED
```

Neu da co alert active:

- Khong tao alert moi.
- Tang `occurrenceCount`.
- Cap nhat `firstSeenAt` / `lastSeenAt`.
- Co the `retrigger` alert va dua status ve `OPEN`.

Neu chua co alert active:

- Tao `AlertEntity` moi voi fingerprint cua log.

Vi vay, alert fingerprint hien tai la fingerprint cua log, duoc dung de gom occurrence cho alert theo rule. No khong phai la root cause, va cung khong phai bang chung duy nhat de ket luan incident.

## 6. Co che incident hien tai

Incident hien tai chi duoc bat dau tu mot alert qua API:

```http
POST /api/v1/incidents/from-alert/{alertId}
```

Controller se:

1. Lay alert theo `alertId`.
2. Kiem tra user co quyen xem application cua alert hay khong.
3. Goi `IncidentFacade.startFromAlert(...)` voi thong tin cua alert.

`IncidentService.startFromAlert(...)` xu ly nhu sau:

1. Validate request phai co `alertId`, `applicationId`, `requestedBy`, va timestamp.
2. Chong trung theo alert: neu alert nay da nam trong mot incident open thi tra ve incident do.
3. Chong trung theo cum: neu co incident open cung `applicationId` va cung `fingerprint`, attach alert moi vao incident cu voi quan he `RELATED`, roi tra ve incident cu.
4. Neu chua co incident phu hop, tao incident moi tu alert voi trigger type `ALERT`.
5. Tinh cua so dieu tra:
   - `windowStart = anchor - 30 phut`
   - `windowEnd = max(now, anchor + 5 phut)`
   - `anchor` la `logTimestamp`, neu khong co thi dung `triggeredAt`.
6. Thu thap evidence.
7. Danh dau da collect evidence.
8. Chay AI analysis.
9. Luu incident.

## 7. Evidence incident dang thu thap

Khi tao incident tu alert, `IncidentEvidenceCollector.collectFromTriggerAlert(...)` thu thap:

1. Trigger alert chinh voi `alertName`, severity, status, occurrence count, `firstSeenAt`, `lastSeenAt`.
2. Trigger fingerprint summary trong `processed_logs`: count, first seen, last seen, sample message.
3. Trace context theo `eventId` cua trigger alert neu log co `traceId`. Neu khong co trace thi bo qua.
4. Top error fingerprints trong cung `applicationId` va incident window, sap xep theo occurrence count.
5. Related alerts trong cung `applicationId` va incident window, khong gioi han vao cung fingerprint.

Luồng mới khong con lay related logs/alerts chi bang `applicationId + fingerprint`. Fingerprint van la tin hieu chong trung va trigger summary, nhung context chinh cua incident den tu window quanh alert.

## 8. Data gui sang AI

Incident AI client hien lay toi da 30 evidence item, moi item gom cac field chinh:

- `type`
- `sourceId`
- `applicationId`
- `fingerprint`
- `traceId`
- `severity`
- `summary`
- `sampleMessage`
- `occurredAt`
- `metadataJson`

Prompt yeu cau AI chi phan tich dua tren evidence duoc cung cap va tra JSON gom:

- `summary`
- `likelyCause`
- `severity`
- `severityReason`
- `confidence`
- `suggestedActions`
- `evidenceRefs`

Provider hien tai duoc cau hinh theo `app.incident.ai.provider=gemini`, model mac dinh trong code/config la Gemini Flash Lite theo cau hinh cua project.

## 9. Vai tro dung cua fingerprint trong tung module

Trong processing:

- Fingerprint dung de gom cac log co cung application, cung level, cung mau message sau normalize.
- Fingerprint khong dai dien cho mot event duy nhat.

Trong alerting:

- Fingerprint dung chung voi `ruleId` de tinh threshold, cooldown, duplicate va active alert occurrence.
- Alert fingerprint la fingerprint tu log, khong phai fingerprint rieng cua alert.

Trong incident:

- Fingerprint dung de chong trung khi user tao incident tu alert khac nhung thuoc cung cum loi.
- Fingerprint dung de tim alert/log evidence lien quan trong cua so thoi gian.
- Fingerprint chi la tin hieu correlation, khong du de ket luan nguyen nhan goc.

## 10. Gioi han hien tai can luu y

Hien tai related logs va related alerts cua incident van dua nhieu vao `applicationId + fingerprint + time window`. Dieu nay giup luong don gian va chong trung tot, nhung co the lam evidence lap lai neu fingerprint qua chung chung.

Neu can dieu tra sau hon, cac tin hieu nen duoc bo sung tiep la:

- `traceId`
- `eventId`
- chuoi thoi gian truoc/sau alert
- deploy/version metadata
- service/component metadata
- log context quanh trigger alert thay vi chi cung fingerprint

Tai thoi diem hien tai, implementation uu tien luong don gian: user chon mot alert lam diem bat dau, incident gom evidence gan alert do, roi AI dua ra phan tich dua tren evidence da thu thap.
