# ✅ CHECKLIST COPY-PASTE - HỆ THỐNG DUYỆT BIỂN SỐ

## 🚀 CÁC BƯỚC NHANH NHẤT

### **Bước 1: Copy-Paste 4 File Java** (3 phút)

Mở file này → Copy Phần Code → Paste vào IDE:

| STT | File Java | Đường dẫn | Phần Code |
|-----|-----------|----------|----------|
| 1️⃣ | ItemDAO.java | `src/main/java/com/auction/database/` | **File 1 trong IMPLEMENTATION_GUIDE.md** |
| 2️⃣ | ProductService.java | `src/main/java/com/auction/service/` | **File 2 trong IMPLEMENTATION_GUIDE.md** |
| 3️⃣ | AdminController.java | `src/main/java/com/auction/controller/` | **File 3 trong IMPLEMENTATION_GUIDE.md** (thêm 2 phần) |
| 4️⃣ | DatabaseInitializer.java | `src/main/java/com/auction/database/` | **File 4 cập nhật phần tạo items** |

---

### **Bước 2: Cập Nhật Database** (2 phút)

**Nếu là lần đầu:**
- ✅ Bỏ qua! App tự tạo bảng

**Nếu database đã có:**
1. Mở MySQL Workbench
2. Mở file `DATABASE_MIGRATION.sql` 
3. Chạy Query

---

### **Bước 3: Restart & Test** (1 phút)

```
Restart IDE / Reload Maven Project
↓
Chạy App
↓
Người dùng: Đăng ký bán → Nhập biển số → Submit
↓
Admin: Xem "Chờ Duyệt" → Bấm ✔ để duyệt
↓
User: Vào Dashboard → Thấy biển số ✅
```

---

## 📋 CHECKOUT LIST

- [ ] Copy ItemDAO.java
- [ ] Copy ProductService.java
- [ ] Copy thêm 2 phần AdminController.java
- [ ] Copy DatabaseInitializer.java
- [ ] Restart IDE
- [ ] Chạy DATABASE_MIGRATION.sql (nếu DB cũ)
- [ ] Chạy App
- [ ] Test: Người dùng tạo biển số
- [ ] Test: Admin duyệt
- [ ] Test: User xem Dashboard
- [ ] ✅ XONG!

---

## 🎯 TEST NHANH

### **Test Case 1: Tạo biển số mới**
```
1. Người dùng: Đăng nhập (BUY / SELL)
2. Bấm: "Đăng Ký Bán"
3. Nhập biển số: 30K-666.77
4. Nhập giá: 100000000
5. Submit
→ Thấy popup: "Biển số đã được gửi đi. Chờ duyệt!" ✅
```

### **Test Case 2: Admin duyệt**
```
1. Admin: Đăng nhập
2. Xem trang Admin
3. Chỉ tất định "Chờ Duyệt"
4. Bấm ✔ ở biển số 30K-666.77
→ Biển số biến mất khỏi danh sách (đã duyệt) ✅
```

### **Test Case 3: User thấy biển số**
```
1. User: Đăng xuất → Đăng nhập lại
2. Vào Dashboard
3. Chọn "Ô tô" / "Xe máy"
→ Thấy biển số 30K-666.77 ✅
```

### **Test Case 4: Kiểm tra Persistence**
```
1. Tắt App hoàn toàn
2. Mở lại App
3. User: Đăng nhập
4. Vào Dashboard
→ Biển số 30K-666.77 vẫn hiển thị ✅
```

---

## 🐛 LỖI CỤ THỂ & CỐ VẤN ĐỀ

### Lỗi 1: "Column 'approval_status' doesn't exist"
```
Cách sửa:
→ Chạy DATABASE_MIGRATION.sql
```

### Lỗi 2: Không thấy biển số mới ở Admin
```
Cách sửa:
→ Đảm bảo app đã save dữ liệu (xem console)
→ Reload Admin Dashboard
```

### Lỗi 3: Admin duyệt rồi nhưng user không thấy
```
Cách sửa:
→ User phải reload Dashboard (F5)
→ Hoặc đăng xuất rồi đăng nhập lại
```

---

## 📂 CẤU TRÚC FILE

```
Auction-System/
├── DATABASE_MIGRATION.sql          ← Chạy nếu DB cũ
├── IMPLEMENTATION_GUIDE.md         ← Hướng dẫn chi tiết
├── QUICK_CHECKLIST.md             ← File này
└── src/main/java/com/auction/
    ├── database/
    │   ├── ItemDAO.java           ← CẬP NHẬT
    │   ├── DatabaseInitializer.java ← CẬP NHẬT
    │   └── ...
    ├── service/
    │   ├── ProductService.java     ← CẬP NHẬT
    │   └── ...
    ├── controller/
    │   ├── AdminController.java    ← CẬP NHẬT
    │   └── ...
    └── ...
```

---

## 💡 MẸO

- **Chỉ sửa các dòng được báo:** Không copy toàn bộ file nếu chỉ có 1-2 phần thay đổi
- **Restart IDE:** Sau khi copy-paste, IDE cần refresh
- **Check Console:** Xem console để kiểm tra lỗi SQL

---

## ✅ HOÀN THÀNH

Sau khi tất cả xong:

✅ Người dùng tạo biển số → lưu DATABASE (PENDING)  
✅ Admin duyệt → cập nhật DATABASE (APPROVED)  
✅ User xem Dashboard → chỉ thấy APPROVED (từ DATABASE)  
✅ Đăng nhập lại → biển số vẫn hiển thị  
✅ **HỆ THỐNG HOẠT ĐỘNG ĐẦY ĐỦ!** 🎉

---

**Bắt đầu ngay:** → Mở IMPLEMENTATION_GUIDE.md để copy code

