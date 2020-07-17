package sushi.hardcore.droidfs.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class ColoredImageView : AppCompatImageView {
    constructor(context: Context) : super(context) {
        applyColor()
    }
    constructor(context: Context, attrs: AttributeSet): super(context, attrs){
        applyColor()
    }
    private fun applyColor(){
        super.setColorFilter(ThemeColor.getThemeColor(context))
    }
}