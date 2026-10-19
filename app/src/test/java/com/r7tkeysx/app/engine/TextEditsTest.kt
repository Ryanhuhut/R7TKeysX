package com.r7tkeysx.app.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class TextEditsTest {
    private fun afterDelete(s: String) = s.dropLast(TextEdits.wordDeleteCount(s))

    @Test fun xoaDungMotTu() {
        assertEquals("xin chào ", afterDelete("xin chào bạn"))
        assertEquals("xin ", afterDelete("xin chào"))
        assertEquals("", afterDelete("xin"))
    }

    @Test fun xoaKemDauCachSauTuNhungKhongLanSangTuTruoc() {
        assertEquals("xin ", afterDelete("xin chào "))
        assertEquals("xin ", afterDelete("xin chào   "))
    }

    @Test fun chuoiRongHoacToanDauCach() {
        assertEquals("", afterDelete(""))
        assertEquals("", afterDelete("   "))
    }
}
