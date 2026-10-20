# R7TKeysX

Bàn phím Android phong cách iOS tối, gõ tiếng Việt (Telex) và tiếng Anh. Viết lại từ đầu bằng **Kotlin + Jetpack Compose**, chạy như bàn phím hệ thống thật (`InputMethodService`).

*An iOS-style dark keyboard for Android with Vietnamese Telex and English typing, built from scratch with Kotlin and Jetpack Compose.*

## Tính năng

- **Gõ Telex tiếng Việt** có gạch chân từ đang soạn, kiểu dấu cũ/mới (`oà` / `òa`), mở lại từ khi xoá dấu cách để gõ tiếp dấu
- **Tiếng Anh** có sửa chính tả và gợi ý từ, gợi ý emoji theo chữ vừa gõ
- Giao diện iOS tối: hiệu ứng nhấn phím, bong bóng chữ, rung nhẹ, âm thanh phím, hiệu ứng sóng
- **Liquid glass** (kính mờ + khúc xạ) bằng thư viện Backdrop
- Shift / Caps Lock 3 trạng thái, tự viết hoa đầu câu, `. Shortcut` (cách hai lần ra dấu chấm)
- Quả địa cầu: chạm để đổi ngôn ngữ, giữ + trượt để chọn
- Giữ phím ra chữ có dấu, giữ phím cách để rê con trỏ, giữ phím xoá để xoá từng từ
- Bàn phím nổi, chỉnh cỡ, tầng số / ký hiệu, bảng emoji, lịch sử clipboard, nhãn dán
- **Nhập giọng nói offline** (tiếng Việt / tiếng Anh) bằng sherpa-onnx; model tải về qua Wi-Fi khi dùng lần đầu
- Không cần Google Play Services

## Yêu cầu

- Android 8.0 trở lên (`minSdk 26`), điện thoại ARM 64-bit
- JDK 17+ và Android SDK (chỉ cần dòng lệnh, không cần Android Studio)

## Build và cài

```bash
./gradlew assembleRelease        # bản dùng hằng ngày
adb install -r app/build/outputs/apk/release/app-release.apk
```

Bản release hiện ký bằng khoá debug có sẵn để cài đè lên bản đang dùng mà không mất cài đặt. Muốn phát hành công khai thì đổi sang khoá riêng của bạn trong `app/build.gradle.kts`.

Sau khi cài: mở app, làm theo thẻ **Status** để bật bàn phím và chọn R7TKeysX làm bàn phím hiện hành.

Chạy kiểm thử trên máy tính (Telex, sửa chính tả, xoá từ, gợi ý chữ):

```bash
./gradlew testDebugUnitTest
```

## Cấu trúc

```
app/src/main/java/com/r7tkeysx/app/
├── R7TInputMethodService.kt   dịch vụ bàn phím hệ thống, nối phím với ô nhập
├── engine/                    Telex, gợi ý chữ, emoji, vuốt gõ (không dính Android)
├── model/                     trạng thái Shift, ngôn ngữ, tầng phím
├── data/                      cài đặt, clipboard, emoji, nhãn dán
├── ui/keyboard/               bàn phím, phím, quả địa cầu, popup
├── ui/settings/               trang cài đặt trong app
├── ui/glass/                  hiệu ứng liquid glass
└── voice/                     nhập giọng nói offline
```

## Giấy phép các thành phần bên thứ ba

- [Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) (Apache-2.0): liquid glass
- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) (Apache-2.0): nhận diện giọng nói
- Từ điển gợi ý chữ dựng từ [FrequencyWords](https://github.com/hermitdave/FrequencyWords) (CC-BY-SA 4.0)

Dự án chỉ phỏng theo *phong cách* iOS, không dùng tên, logo hay hình ảnh của Apple.
