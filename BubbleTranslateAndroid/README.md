# Bubble Translate — Ứng dụng Dịch Màn Hình Trực Tiếp Cho Android

Ứng dụng dịch màn hình trực tiếp không cần chụp rồi dán. Dịch đè ngay lên vị trí chữ gốc trên màn hình điện thoại khi đang chơi game, đọc truyện tranh, xem video hay lướt mạng xã hội.

---

## ⚡ Cơ chế hoạt động (Dịch trực tiếp trong 1 chạm)

1. **Bong bóng nổi (Floating Bubble)**: Lơ lửng trên mọi ứng dụng khác nhờ quyền `SYSTEM_ALERT_WINDOW`. Bạn có thể kéo thả di chuyển tự do và tự động hít vào mép màn hình.
2. **Chụp màn hình ngầm tức thì (MediaProjection API)**: Khi bạn chạm vào bong bóng nổi, ứng dụng chụp frame màn hình hiện tại trong vòng ~50ms mà không làm gián đoạn màn hình của bạn.
3. **Nhận diện chữ siêu tốc (Google ML Kit Text Recognition)**: Phân tích tọa độ chính xác của từng dòng chữ trên màn hình (hỗ trợ Tiếng Anh, Trung, Nhật, Hàn, Latin).
4. **Dịch tự động sang Tiếng Việt**: Dịch thuật tức thì qua Google Translate Engine.
5. **Vẽ bản dịch đè trực tiếp (In-place Overlay)**: Các hộp chữ dịch tiếng Việt được vẽ đè ngay ngắn đúng vào vị trí chữ gốc trên màn hình. Chạm vào chữ để nghe phát âm giọng nói (Text-to-Speech).

---

## 📦 Cách lấy file cài đặt APK về điện thoại

### Cách 1: Tự động Build APK trên GitHub (Khuyên dùng - Không cần cài Android Studio)
Dự án đã được cấu hình sẵn file GitHub Actions `.github/workflows/build-apk.yml`. Bạn chỉ cần:
1. Tạo một Repository mới trên [GitHub.com](https://github.com) (chế độ Public hoặc Private đều được).
2. Tải toàn bộ thư mục `BubbleTranslateAndroid` này lên repository đó:
   ```bash
   git init
   git add .
   git commit -m "Initial Bubble Translate App"
   git branch -M main
   git remote add origin https://github.com/TÊN_BẠN/TÊN_REPO.git
   git push -u origin main
   ```
3. Chuyển sang tab **Actions** trên GitHub: bạn sẽ thấy quy trình **Build Bubble Translate APK** tự động chạy trong khoảng 2 phút.
4. Sau khi hoàn thành, nhấn vào lần build đó và tải file **`BubbleTranslate-APK`** (bên trong có `app-debug.apk`) về điện thoại Android để cài đặt ngay!

---

### Cách 2: Mở bằng Android Studio (Nếu bạn có máy tính cài Android Studio)
1. Mở **Android Studio** $\rightarrow$ Chọn **Open** $\rightarrow$ Trỏ tới thư mục `BubbleTranslateAndroid`.
2. Chờ Gradle đồng bộ xong các thư viện.
3. Cắm cáp điện thoại Android hoặc mở máy ảo $\rightarrow$ Nhấn nút **Run ▶** (hoặc vào menu `Build > Build Bundle(s) / APK(s) > Build APK(s)`).

---

## 📱 Hướng dẫn sử dụng trên điện thoại

1. **Mở ứng dụng lần đầu**:
   - Nhấn **Bắt đầu dịch màn hình**.
   - Cấp quyền **"Xuất hiện trên cùng"** (Display over other apps) theo hướng dẫn trên màn hình.
   - Nhấn **"Bắt đầu ngay"** khi hệ thống hỏi quyền Chụp màn hình.
2. **Sử dụng**:
   - Bong bóng nổi tròn sẽ xuất hiện trên màn hình.
   - Bạn mở bất kỳ game, truyện tranh, TikTok hay bài báo tiếng nước ngoài.
   - **Chạm 1 cái vào bong bóng nổi** $\rightarrow$ Bản dịch tiếng Việt sẽ lập tức phủ đè lên màn hình ngay tại vị trí chữ gốc!
   - Chạm vào màn hình hoặc nút ✕ để tắt lớp phủ và tiếp tục sử dụng bình thường.
