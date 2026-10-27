# Cài Nhắc nhở kỷ niệm lên điện thoại

Máy cần **Android 12 trở lên**. File cài là bản debug đã ký, cài được trực tiếp. Không dùng bản release: project chưa có chữ ký release, điện thoại sẽ từ chối.

## File APK

```
app\build\outputs\apk\debug\app-debug.apk
```

Tên trên máy tiếng Việt: **Nhắc nhở kỷ niệm**. Gói `com.mine.reminder`, phiên bản 1.0.

Build lại khi sửa code (trong thư mục project):

```
gradlew.bat assembleDebug
```

## Đưa file sang máy

Cách nào cũng được: cáp USB (copy vào thư mục Download), Zalo, Google Drive, hoặc Nearby Share. Giữ nguyên đuôi `.apk`.

## Cài

1. Mở file `app-debug.apk` trên điện thoại (trình quản lý tệp, hoặc thông báo tải về).
2. Nếu được hỏi, bật **Cho phép cài ứng dụng từ nguồn này** cho app đang mở file (Files, Chrome, Zalo…).
3. Bấm **Cài đặt**. Xong thì mở app.

Cài đè bản cũ chỉ được khi APK build trên **cùng máy tính** này. Build từ máy khác sẽ báo không cài được: gỡ app cũ rồi cài lại. Dữ liệu ngày kỷ niệm trên máy sẽ mất.

## Sau khi mở app

Bấm biểu tượng bánh răng (**Cài đặt máy**) và bật hết mục Nhắc nhở, không thì báo không kêu đúng giờ:

1. **Thông báo** — cho phép biểu ngữ và màn hình khóa.
2. **Báo thức và nhắc nhở** — bật quyền báo thức đúng giờ.
3. **Pin** — chọn không hạn chế.
4. **Tự khởi động** — trên Oppo / Realme (ColorOS), bật app trong danh sách tự khởi động. Máy không có màn hình này thì app mở trang thông tin ứng dụng.

Đặt **Giờ nhắc** trong cùng màn hình đó.
