package com.r7tkeysx.app.engine

import java.text.Normalizer

// Gợi ý emoji theo từ khóa tiếng Việt: gõ "hoa héo" -> 🥀, "cười" -> 😂...
// Xét 3, rồi 2, rồi 1 từ cuối cùng trước con trỏ; cụm dài khớp trước (vd. "hoa héo" thắng "hoa").
// Chỉ gồm emoji hay dùng; emoji ít ai dùng (tranh, đồ vật hiếm...) thì bỏ, không cố gán chữ.
object EmojiSuggester {
    // Mỗi dòng: emoji, dấu ":", rồi các từ khóa cách nhau bởi dấu phẩy (có thể nhiều chữ)
    private const val DATA = """
😂: cười, haha, hihi, hehe, buồn cười, cười chảy nước mắt, mắc cười, hài
🤣: cười lăn, lol, cười bò, cười sặc
😀: vui, vui vẻ, hớn hở
😊: hạnh phúc, mỉm cười, cười mỉm, dễ thương
😍: mê, thích quá, mê quá, đẹp quá, si mê
🥰: yêu thương, đáng yêu, thương quá
😘: hôn, hun, moa, chụt
😉: nháy mắt
😎: ngầu, chất, cool, kính râm
🤔: suy nghĩ, nghĩ, hmm, không biết nữa, thắc mắc
😐: bình thường, vô cảm
😑: cạn lời
🙄: chán, chán đời, liếc
😏: nhếch mép, đểu
😴: ngủ, buồn ngủ, đi ngủ, ngủ ngon
🥱: ngáp, mệt mỏi, uể oải
😪: mệt
😢: buồn, khóc, tủi thân
😭: khóc to, khóc lóc, huhu, hu hu, buồn quá
😡: giận, tức, tức giận, cáu, điên
🤬: chửi, chửi thề
😱: sợ, hoảng, hoảng hốt, kinh khủng
😨: lo sợ
😰: lo lắng, lo
😳: ngại, xấu hổ, ngượng
🥺: năn nỉ, xin xỏ, tội nghiệp, tội
😅: hì hì, toát mồ hôi, ngại ngùng
😇: ngoan, thiên thần
🤗: ôm, ôm ấp, cái ôm
🤫: bí mật, suỵt
🤐: im lặng, im miệng
🤢: buồn nôn, ghê
🤮: nôn, ói, kinh tởm
🤒: ốm, sốt, bệnh
🤧: cảm, cảm cúm, hắt hơi, sổ mũi
😷: khẩu trang, dịch bệnh
🥵: nóng, nóng quá, nóng bức
🥶: lạnh, rét, lạnh quá
🤯: sốc, choáng, nổ não
🥳: tiệc, sinh nhật, chúc mừng, ăn mừng
🤩: tuyệt vời, wow, choáng ngợp
😋: ngon, ngon quá, thèm
🤤: chảy nước miếng, thèm quá
🤑: tiền, giàu, giàu có
🤡: hề, chú hề
👻: ma, con ma
💀: chết, chết cười, xỉu
💩: phân, cứt
🤖: robot, người máy
👽: người ngoài hành tinh
🙈: không dám nhìn, che mắt
😺: mèo cười
❤️: yêu, tim, trái tim, thương, love, yêu em, yêu anh
💔: tan vỡ, thất tình, đau lòng, chia tay
💕: yêu nhau
💖: lấp lánh yêu
🔥: lửa, cháy, nóng bỏng, đỉnh, hot
💯: điểm mười, chuẩn, hoàn hảo, trăm điểm
✨: lấp lánh, long lanh
💪: mạnh mẽ, cố lên, sức mạnh, cơ bắp, chiến
👍: tốt, ok, oke, đồng ý, like, thích
👎: tệ, dở, không thích
👏: vỗ tay, hoan hô, giỏi quá
🙏: cảm ơn, cám ơn, cầu nguyện, làm ơn
👋: chào, xin chào, tạm biệt, bye, hello
🤝: bắt tay, hợp tác, thỏa thuận, thoả thuận
✌️: hòa bình, hoà bình, chiến thắng, yeah
🤞: may mắn, cầu may
👌: ổn, tuyệt
👀: nhìn, hóng, hóng hớt
🧠: não, thông minh, bộ não
🎉: mừng, chúc mừng năm mới, tiệc tùng, yay
🎂: bánh sinh nhật, sinh nhật vui vẻ
🎁: quà, món quà, tặng quà
🎈: bóng bay
🎄: giáng sinh, noel
🎆: pháo hoa
🧧: lì xì, tết, năm mới
🏮: đèn lồng, trung thu
🌹: hoa hồng
🥀: hoa héo, héo, tàn, hoa tàn, héo úa
🌸: hoa anh đào, anh đào, hoa
🌻: hoa hướng dương, hướng dương
🌷: hoa tulip, tulip
🌼: hoa cúc, cúc
💐: bó hoa
🍀: cỏ bốn lá
🌳: cây, cây xanh
🍂: lá rụng, mùa thu, thu
🌵: xương rồng
🌞: mặt trời, nắng, buổi sáng
🌙: trăng, mặt trăng, buổi tối, đêm
⭐: ngôi sao
🌈: cầu vồng
☀️: trời nắng
☁️: mây, trời mây
🌧️: mưa, trời mưa
⛈️: bão, sấm sét, giông
❄️: tuyết, băng giá
🌊: sóng, biển, đại dương
💧: giọt nước
⚡: sét, điện, nhanh
🌍: trái đất, thế giới
🐶: chó, cún, con chó, cún con
🐱: mèo, con mèo, mèo con
🐭: chuột, con chuột
🐰: thỏ, con thỏ
🐻: gấu, con gấu
🐼: gấu trúc
🐯: hổ, cọp, con hổ
🦁: sư tử
🐮: bò, con bò
🐷: lợn, heo, con heo, con lợn
🐸: ếch, con ếch
🐵: khỉ, con khỉ
🐔: gà, con gà
🐧: chim cánh cụt
🐦: chim, con chim
🦆: vịt, con vịt
🦋: bướm, con bướm
🐝: ong, con ong
🐍: rắn, con rắn
🐢: rùa, con rùa
🐟: cá, con cá
🐬: cá heo
🐳: cá voi
🦈: cá mập
🐘: voi, con voi
🐴: ngựa, con ngựa
🦄: kỳ lân
🐉: rồng, con rồng
🦀: cua, con cua
🦐: tôm, con tôm
🐙: bạch tuộc
🍎: táo, quả táo
🍌: chuối, quả chuối
🍊: cam, quả cam
🍉: dưa hấu
🍇: nho, quả nho
🍓: dâu, dâu tây
🍍: dứa, thơm
🥭: xoài, quả xoài
🥥: dừa, quả dừa
🍒: anh đào đỏ
🍑: đào, quả đào
🍋: chanh, quả chanh
🥑: bơ, quả bơ
🍅: cà chua
🌶️: ớt, cay
🥕: cà rốt
🌽: ngô, bắp
🥔: khoai tây
🍞: bánh mì
🥖: bánh mì que
🍚: cơm, ăn cơm
🍜: phở, mì, bún, mì tôm
🍲: lẩu, canh
🍣: sushi
🍕: pizza
🍔: hamburger, bánh kẹp
🍟: khoai tây chiên
🍗: gà rán, đùi gà
🥩: thịt, bít tết
🥚: trứng, quả trứng
🍳: ốp la, trứng chiên
🥗: rau, salad
🍦: kem, ăn kem
🍰: bánh ngọt, bánh kem
🍫: sô cô la, socola
🍬: kẹo
🍿: bỏng ngô, bắp rang
☕: cà phê, cafe, cafe sáng
🍵: trà, chè
🧋: trà sữa
🥛: sữa
🍺: bia, nhậu
🍻: cụng ly, dô, zô
🍷: rượu vang, vang
🥂: nâng ly
🍴: ăn, đói, ăn uống
⚽: bóng đá, đá bóng, đá banh
🏀: bóng rổ
🏸: cầu lông
🏓: bóng bàn
🏊: bơi, bơi lội
🏃: chạy, chạy bộ
🚴: đạp xe
🏋️: tập gym, gym, tập tạ
🧘: yoga, thiền
🏆: cúp, vô địch, chiến thắng
🥇: huy chương, hạng nhất
🎮: chơi game, game, trò chơi
🎵: nhạc, âm nhạc, bài hát
🎤: hát, karaoke
🎸: đàn, ghi ta, guitar
🎬: phim, xem phim
📷: chụp ảnh, ảnh, máy ảnh
📚: sách, đọc sách, học
✏️: viết, bút chì
📝: ghi chú, ghi chép
💻: máy tính, laptop, làm việc
📱: điện thoại
💡: ý tưởng
🔑: chìa khóa, chìa khoá
💰: tiền bạc, túi tiền
💵: đô la, đô
💸: tiêu tiền, hết tiền
🏠: nhà, về nhà, ở nhà
🏫: trường, trường học, đi học
🏥: bệnh viện
🏢: công ty, văn phòng, đi làm
⛪: nhà thờ
🛕: chùa, đền
🏖️: bãi biển, đi biển
⛰️: núi, leo núi
🏕️: cắm trại
🚗: xe, ô tô, xe hơi
🏍️: xe máy
🚲: xe đạp
🚌: xe buýt
🚕: taxi, grab
✈️: máy bay, bay, du lịch
🚢: tàu, tàu thủy, tàu thuỷ
🚆: tàu hỏa, tàu hoả
🚀: tên lửa, phóng
⏰: báo thức, dậy, thức dậy
⌛: chờ, đợi, thời gian
📅: lịch
🎓: tốt nghiệp
💍: nhẫn, cầu hôn, kết hôn
💒: đám cưới, cưới
👶: em bé, trẻ con, bé
👨‍👩‍👧: gia đình
👫: cặp đôi, người yêu
👴: ông, ông nội, ông ngoại
👵: bà, bà nội, bà ngoại
👨‍⚕️: bác sĩ
👮: công an, cảnh sát
👨‍🍳: đầu bếp, nấu ăn
👑: vua, vương miện, nữ hoàng
💋: môi, son
💤: buồn ngủ quá, zzz
💬: nói chuyện, tin nhắn, nhắn tin
📞: gọi, gọi điện
📢: thông báo
✅: xong, hoàn thành
❌: sai, hủy, huỷ
⚠️: cảnh báo, chú ý, cẩn thận
❓: hỏi, câu hỏi
❗: quan trọng
🆗: okela
🇻🇳: việt nam
"""

    private val map: Map<String, String> by lazy {
        val m = HashMap<String, String>()
        for (line in DATA.lines()) {
            val i = line.indexOf(':')
            if (i <= 0) continue
            val emoji = line.substring(0, i).trim()
            for (kw in line.substring(i + 1).split(',')) {
                val key = norm(kw)
                if (key.isNotEmpty() && key !in m) m[key] = emoji
            }
        }
        m
    }

    private fun norm(s: String) = Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFC).replace(Regex("\\s+"), " ")

    // Emoji hợp với mấy chữ cuối của `textBeforeCursor` (bỏ dấu câu / khoảng trắng ở cuối); null nếu không có
    fun suggest(textBeforeCursor: String): String? {
        val words = textBeforeCursor.takeLast(60).lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        for (n in 3 downTo 1) {
            if (words.size < n) continue
            map[norm(words.takeLast(n).joinToString(" "))]?.let { return it }
        }
        return null
    }
}
