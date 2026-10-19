package com.r7tkeysx.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordSuggesterTest {
    private val en = WordSuggester(listOf("the", "hello", "world", "test", "keyboard", "there"))
    private val vi = WordSuggester(listOf("tiếng", "tiền", "tiếp", "việt"))

    @Test fun suaLoiSaiMotChu() {
        assertEquals("the", en.correct("teh"))        // đảo 2 chữ liền nhau
        assertEquals("hello", en.correct("helo"))     // thiếu 1 chữ
        assertEquals("world", en.correct("worlld"))   // thừa 1 chữ
        assertEquals("test", en.correct("tast"))      // sai 1 chữ
    }

    @Test fun giuKieuVietHoa() {
        assertEquals("The", en.correct("Teh"))
    }

    @Test fun khongSuaKhiKhongCan() {
        assertNull(en.correct("the"))      // từ đúng
        assertNull(en.correct("TEH"))      // viết tắt toàn chữ hoa
        assertNull(en.correct("te"))       // quá ngắn
        assertNull(en.correct("qwerty"))   // không có từ nào gần
        assertNull(en.correct("hel1o"))    // dính số
    }

    @Test fun goiYTiengVietGoKhongDau() {
        assertEquals(listOf("tiếng", "tiền"), vi.suggest("tie", 2))
    }
}
