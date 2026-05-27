# Security and Architecture Review

Ngay sau khi review, hai rui ro can xu ly truoc khi demo hoặc public repo la:

1. Credentials that trong `database.properties` da bi commit vao repo.
2. Password dang duoc hash bang SHA-256 thuan, khong salt.

Tai lieu nay chi tong hop van de va huong xu ly de can nhac. Khong co thay doi code nao duoc thuc hien trong file nay.

## 1. Cac van de dua tren prompt

### 1.1. Railway database password bi commit vao repo

**Muc do:** Critical

`database.properties` dang chua password Railway that va da duoc commit vao repository. Vi credential da xuat hien trong lich su Git, chi xoa khoi file hien tai la chua du.

**Tac dong:**

- Bat ky ai co quyen doc repo hoac lich su commit co the lay credential.
- Neu repo tung public, credential co kha nang da bi crawler hoac bot thu thap.
- Database co nguy co bi truy cap trai phep, dump du lieu, sua du lieu, hoac xoa du lieu.

**Khuyen nghi xu ly:**

1. Rotate password Railway ngay tren Railway dashboard.
2. Cap nhat app dung bien moi truong, vi du:
   - `DB_HOST`
   - `DB_PORT`
   - `DB_NAME`
   - `DB_USER`
   - `DB_PASSWORD`
3. Khong commit file chua secret that.
4. Dung GitHub Secrets cho CI/CD.
5. Them file cau hinh mau, vi du `database.properties.example`, chi chua placeholder.
6. Can nhac rewrite Git history neu repo da public hoac share ra ngoai.

**Uu tien truoc demo:** Bat buoc lam truoc.

### 1.2. PasswordUtil dung SHA-256 thuan khong salt

**Muc do:** Critical

`PasswordUtil` dang hash password bang SHA-256 thuan. Day khong phai cach luu password an toan vi SHA-256 qua nhanh va khong co salt rieng cho tung user.

**Tac dong:**

- De bi tan cong rainbow table.
- Hai user co cung password se co cung hash.
- Neu database bi lo, attacker co the brute force nhanh bang GPU.

**Khuyen nghi xu ly:**

1. Chuyen sang BCrypt hoac Argon2.
2. Moi password phai co salt rieng, duoc quan ly boi thu vien hash password.
3. Them migration strategy cho user cu:
   - Khi login thanh cong bang hash cu, rehash sang BCrypt/Argon2.
   - Hoac yeu cau reset password neu muon don gian va an toan hon.
4. Them test cho verify password dung/sai va luong migrate hash cu neu co.

**Uu tien truoc demo:** Bat buoc lam truoc.

### 1.3. DashboardController snapshot con tham chieu ThemeManager va btnTheme

**Muc do:** High neu con ton tai trong source build, Medium neu chi nam trong snapshot/tai lieu.

Prompt ghi nhan snapshot cua `DashboardController` con tham chieu `ThemeManager` va `btnTheme`, trong khi ban thuc te da bo. Neu con sot trong source, project co the compile fail.

**Tac dong:**

- Compile error neu class/field khong con ton tai.
- De gay nham lan khi merge code giua cac thanh vien.

**Khuyen nghi xu ly:**

1. Search toan repo cac tu khoa:
   - `ThemeManager`
   - `btnTheme`
2. Neu chi con trong snapshot/tai lieu thi ghi ro do la ban cu.
3. Neu con trong source build thi xoa reference hoac khoi phuc dependency tuong ung.

**Uu tien truoc demo:** Kiem tra nhanh truoc khi build demo.

### 1.4. WebSocket server hardcode `ws://localhost:8081`

**Muc do:** High cho production, Medium cho demo mot may.

WebSocket server dang chay tren cung may voi UI client. Mo hinh nay co the demo tren local, nhung khong dung cho nhieu user tren nhieu may khac nhau.

**Tac dong:**

- May A ket noi `localhost` thi se tim WebSocket server tren chinh may A, khong phai server chung.
- Hai user o hai may khac nhau se khong chia se cung mot real-time channel.
- Khong phu hop neu deploy UI va backend rieng.

**Khuyen nghi xu ly:**

1. Tach WebSocket server thanh process/service doc lap.
2. Deploy WebSocket server len Railway hoac mot host rieng.
3. Dua WebSocket URL vao config/env, vi du `WS_SERVER_URL`.
4. Dung `wss://` khi deploy production.
5. Neu co CI/CD, cau hinh URL qua GitHub Secrets hoac environment cua platform deploy.

**Uu tien truoc demo:** Neu demo tren mot may thi co the chap nhan tam. Neu demo nhieu may thi can sua truoc.

### 1.5. `synchronized` trong `BidDAO.saveBid` chi bao ve trong cung JVM

**Muc do:** High cho multi-instance, Medium cho single-instance demo.

`synchronized` chi ngan race condition giua cac thread trong cung mot JVM. Neu app chay nhieu instance, moi instance co lock rieng, nen khong bao ve duoc tranh chap bid toan he thong.

**Diem dung hien co:**

- Neu DAO da dung transaction va `SELECT ... FOR UPDATE`, database-level locking moi la co che dung de dam bao tinh nhat quan.

**Khuyen nghi xu ly:**

1. Giu logic tranh chap bid trong transaction.
2. Dam bao row lien quan den item/auction duoc lock bang `FOR UPDATE`.
3. Dat isolation level phu hop.
4. Xu ly deadlock/retry neu database tra loi loi tranh chap.
5. Khong dua vao `synchronized` nhu co che an toan chinh cho production.

**Uu tien truoc demo:** Kiem tra transaction va test case bid dong thoi neu co thoi gian.

### 1.6. `province` trong `ItemDTO` bi set thanh chuoi rong

**Muc do:** Low/Medium

Prompt ghi nhan `province` trong `ItemDTO` luon duoc set thanh `""` du DAO co tra ve du lieu. Ket qua la cot province tren Dashboard hien dau `•` hoac trong rong.

**Tac dong:**

- UI hien thi thieu thong tin.
- Du lieu DAO co nhung bi mat khi map sang DTO.
- Nguoi dung co the nghi item khong co dia diem/tinh thanh.

**Khuyen nghi xu ly:**

1. Kiem tra mapping tu DAO/entity sang `ItemDTO`.
2. Dam bao `province` duoc gan tu du lieu thuc te.
3. Them test mapping DTO cho item co province va khong co province.

**Uu tien truoc demo:** Nen sua neu dashboard co hien cot nay.

### 1.7. Test coverage con thap

**Muc do:** Medium

Prompt ghi nhan hien chi co 3 file test cho logic UI/util, chua co test DAO hoac service layer.

**Tac dong:**

- De regression khi sua bidding, authentication, WebSocket.
- Kho tu tin khi refactor security va config.
- Khong bat duoc loi race condition, transaction, mapping DTO.

**Khuyen nghi bo sung test:**

1. `PasswordUtil`:
   - Hash khong trung nhau cho cung mot password neu dung BCrypt/Argon2.
   - Verify dung/sai.
   - Migration hash cu neu co.
2. `BidDAO` hoac service bid:
   - Bid hop le.
   - Bid thap hon gia hien tai bi reject.
   - Hai bid dong thoi chi chap nhan bid dung.
3. DTO mapping:
   - `province` duoc map dung.
4. WebSocket:
   - Client doc URL tu config.
   - Reconnect behavior neu server mat ket noi.

## 2. Nhung diem bo sung de can nhac

### 2.1. Sau khi secret bi lo, can coi secret do da mat an toan vinh vien

Ngay ca khi commit sau da xoa password, credential cu van nam trong Git history. Xu ly dung la rotate secret, sau do moi don repo.

Checklist nen lam:

1. Tao password/database credential moi tren Railway.
2. Revoke credential cu.
3. Cap nhat local env va GitHub Secrets.
4. Kiem tra log CI/CD de chac khong in secret ra console.
5. Chay secret scanner, vi du GitHub secret scanning hoac cong cu tuong duong.

### 2.2. Nen chuan hoa cau hinh theo environment

Hien tai cac gia tri nhu database URL va WebSocket URL nen duoc gom ve mot tang config.

Nen co:

- `database.properties.example` cho dev.
- Bien moi truong cho secret va endpoint.
- Gia tri default chi dung cho local, khong chua secret.
- Huong dan setup trong README.

Khong nen co:

- Password that trong source.
- Endpoint production hardcode trong class Java.
- `localhost` hardcode trong code client neu app co muc tieu demo nhieu may.

### 2.3. Can phan biet muc tieu demo va production

Neu day la du an nhom mon hoc, co the chia muc tieu thanh hai muc:

**Bat buoc truoc demo:**

- Rotate Railway credential.
- Dua password DB ra bien moi truong/GitHub Secrets.
- Doi SHA-256 sang BCrypt/Argon2 hoac it nhat co ke hoach migration ro rang.
- Build lai va dam bao khong con `ThemeManager`/`btnTheme` gay loi.

**Nen lam neu huong production:**

- Tach WebSocket server thanh service doc lap.
- Dung `wss://`.
- Them database-level concurrency test cho bid.
- Them audit/logging cho cac hanh dong quan trong: login, bid, admin update.
- Them rate limiting cho login va bid endpoint neu co.

### 2.4. Migration password can duoc thiet ke can than

Neu database hien co da luu SHA-256, chuyen ngay sang BCrypt/Argon2 se lam user cu khong login duoc neu khong co migration.

Hai cach kha thi:

1. **Lazy migration khi login:**
   - Detect hash cu.
   - Verify bang SHA-256 cu.
   - Neu dung, hash lai password bang BCrypt/Argon2 va update database.

2. **Force reset password:**
   - Danh dau tat ca user can reset password.
   - Gui thong bao hoac yeu cau user doi password khi dang nhap.

Voi demo mon hoc, lazy migration thuong de trinh bay hon vi cho thay he thong nang cap ma khong pha user cu.

### 2.5. Bid consistency nen nam o service boundary

Neu bidding la chuc nang loi, nen co mot service ro rang quan ly rule:

- Auction con dang mo.
- User khong bid item cua chinh minh neu co rule nay.
- Gia bid cao hon gia hien tai va buoc gia toi thieu.
- Transaction lock dung row can thiet.
- Sau khi commit moi broadcast WebSocket event.

Dieu quan trong la khong broadcast bid thanh cong truoc khi database commit thanh cong.

### 2.6. Nen kiem tra `.gitignore`

Can dam bao cac file chua cau hinh local khong bi commit tiep:

- `database.properties`
- `.env`
- file credential export tu Railway
- IDE local config co the chua token

Dong thoi nen commit file example:

- `.env.example`
- `database.properties.example`

File example chi nen chua placeholder, khong chua secret that.

## 3. Thu tu uu tien de hanh dong

1. Rotate Railway password va revoke password cu.
2. Dua database credential ra environment variables/GitHub Secrets.
3. Xoa secret khoi file config dang commit va them file example.
4. Chuyen password hashing sang BCrypt/Argon2, kem migration strategy.
5. Build/test toan project de bat loi `ThemeManager`/`btnTheme`.
6. Sua mapping `province` trong `ItemDTO`.
7. Dua WebSocket URL vao config.
8. Neu can demo nhieu may, deploy WebSocket server thanh service doc lap.
9. Them test DAO/service cho bidding va authentication.

## 4. Ket luan ngan

He thong co scope tot cho du an nhom: co real-time, phan quyen, CI/CD va luong nghiep vu kha rong. Tuy nhien, credential bi commit va password hashing bang SHA-256 thuan la hai rui ro nghiem trong nhat. Hai diem nay nen duoc xu ly truoc moi buoi demo, dac biet neu repo duoc share cho giang vien, ban cung nhom, hoac public tren GitHub.
