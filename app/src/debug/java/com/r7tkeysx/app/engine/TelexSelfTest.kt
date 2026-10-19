package com.r7tkeysx.app.engine

// Chạy thử bộ gõ Telex trên máy tính (không cần điện thoại): in PASS / FAIL từng ca
fun main() {
    val cases = listOf(
        "vieetj" to "việt", "tieengs" to "tiếng", "dduwowcj" to "được", "duwowcj" to "dược",
        "hoaf" to "hoà", "tooi" to "tôi", "nguwowif" to "người", "toans" to "toán",
        "quas" to "quá", "gias" to "giá", "giaf" to "già", "cass" to "cas", "aaa" to "aa",
        "aww" to "aw", "w" to "ư", "s" to "s", "Vieetj" to "Việt", "DDa" to "Đa",
        "muoons" to "muốn", "chuyeenr" to "chuyển", "khoer" to "khoẻ", "hojc" to "học",
        "dd" to "đ", "ddd" to "dd", "uow" to "ươ", "uwow" to "ươ", "xin" to "xin",
        "toanf" to "toàn", "traanf" to "trần", "Nguyeenx" to "Nguyễn", "thuwf" to "thừ",
        "hoaan" to "hoân", "sawcs" to "sắc", "moiwf" to "mời", "chaof" to "chào",
        "vieetn" to "viêtn", "buwowir" to "bưởi", "buwoir" to "bưởi", "buoiwr" to "bưởi", "buwoi" to "bươi",
        "duwoc" to "dươc", "nuwowcs" to "nước", "Buwowir" to "Bưởi", "tuwr" to "tử", "zoo" to "zô", "tuyeetj" to "tuyệt", "thaif" to "thài",
        "coss" to "cos", "cosss" to "coss", "cossss" to "cosss", "aaaa" to "aaa", "awww" to "aww", "dddd" to "ddd",
        "caasss" to "câss", "tieengss" to "tiêngs", "sao" to "sao", "trong" to "trong", "xooong" to "xoong",
    )
    val oldStyle = listOf("hoaf" to "hòa", "khoer" to "khỏe", "thuyr" to "thủy", "toans" to "toán")
    var fail = 0
    for ((keys, want) in oldStyle) {
        var word = ""
        for (c in keys) word = TelexEngine.type(word, c, oldToneStyle = true)
        val ok = word == want
        if (!ok) fail++
        println((if (ok) "PASS " else "FAIL ") + "[kiểu cũ] $keys -> $word" + if (ok) "" else "   (mong đợi: $want)")
    }
    for ((keys, want) in cases) {
        var word = ""
        for (c in keys) word = TelexEngine.type(word, c)
        val ok = word == want
        if (!ok) fail++
        println((if (ok) "PASS " else "FAIL ") + "$keys -> $word" + if (ok) "" else "   (mong đợi: $want)")
    }
    println(if (fail == 0) "TẤT CẢ ĐẠT (${cases.size})" else "CÓ $fail CA LỖI")
}
