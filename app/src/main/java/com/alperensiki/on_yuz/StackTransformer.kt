import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class StackTransfomer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        val absPos = abs(position)

        page.apply {
            if (position >= 0) {
                val scale = 0.85f + (1 - absPos) * 0.15f
                scaleX = scale
                scaleY = scale

                translationY = -height * position * 0.8f

                alpha = 1f - (absPos * 0.3f)
            } else {
                translationY = 0f
                alpha = 1f
                scaleX = 1f
                scaleY = 1f
            }

            translationZ = -absPos
        }
    }
}