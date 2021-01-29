package android.text.style

import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint

/**
 * Created by elephant on 2020/10/15 11:53.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class FakeBoldSpan : CharacterStyle() {

    override fun updateDrawState(tp: TextPaint?) {
        tp?.isFakeBoldText = true                   //一种伪粗体效果，比原字体加粗的效果弱一点

        /*tp?.style = Paint.Style.FILL_AND_STROKE
        tp?.color = Color.RED                       //字体颜色
        tp?.strokeWidth = 10f                       //控制字体加粗的程度*/
    }
}