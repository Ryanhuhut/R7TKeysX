package com.r7tkeysx.app.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.Normalizer

// Gõ lần lượt từng phím như người dùng rồi so với chữ mong đợi
class TelexEngineTest {
    private fun typed(keys: String, old: Boolean = false): String =
        Normalizer.normalize(keys.fold("") { w, k -> TelexEngine.type(w, k, old) }, Normalizer.Form.NFC)

    private fun nfc(s: String) = Normalizer.normalize(s, Normalizer.Form.NFC)

    @Test fun dauMuVaTrang() {
        assertEquals(nfc("â"), typed("aa"))
        assertEquals(nfc("ê"), typed("ee"))
        assertEquals(nfc("ô"), typed("oo"))
        assertEquals(nfc("ă"), typed("aw"))
        assertEquals(nfc("ơ"), typed("ow"))
        assertEquals(nfc("ư"), typed("uw"))
        assertEquals(nfc("đ"), typed("dd"))
    }

    @Test fun nămDauThanh() {
        assertEquals(nfc("má"), typed("mas"))
        assertEquals(nfc("mà"), typed("maf"))
        assertEquals(nfc("mả"), typed("mar"))
        assertEquals(nfc("mã"), typed("max"))
        assertEquals(nfc("mạ"), typed("maj"))
    }

    @Test fun tuDai() {
        assertEquals(nfc("việt"), typed("vieetj"))
        assertEquals(nfc("tiếng"), typed("tieesng"))
        assertEquals(nfc("người"), typed("nguwowif"))
        assertEquals(nfc("đường"), typed("dduwowngf"))
    }

    @Test fun dauGoSauCungDuoc() {
        // gõ dấu thanh ở cuối từ vẫn đặt đúng chỗ
        assertEquals(nfc("việt"), typed("vietej"))
        assertEquals(nfc("tiếng"), typed("tiengse"))
    }

    @Test fun boDauBangZ() {
        assertEquals("ma", typed("masz"))
    }

    @Test fun kieuDauCuVaMoi() {
        assertEquals(nfc("hoà"), typed("hoaf"))
        assertEquals(nfc("hòa"), typed("hoaf", old = true))
    }

    @Test fun chuHoaGiuNguyen() {
        assertEquals(nfc("Việt"), typed("Vieetj"))
    }

    @Test fun wLeVaBamDup() {
        assertEquals(nfc("ư"), typed("w"))
        assertEquals("w", typed("ww"))
        assertEquals("www", typed("wwww")) // w, ư, w, rồi w thường
        assertEquals("W", typed("WW"))
        assertEquals(nfc("tư"), typed("tw"))
        assertEquals(nfc("ưu"), typed("wu"))
    }
}
