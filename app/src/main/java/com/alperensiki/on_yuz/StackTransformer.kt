import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class StackTransfomer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        val absPos = abs(position)

        page.apply {
            if (position >= 0) { // Arkadaki kartlar
                // Ölçeklendirme: Arkadakiler %85 boyutuna iner
                val scale = 0.85f + (1 - absPos) * 0.15f
                scaleX = scale
                scaleY = scale

                // Yığın (Stack) görüntüsü için dikey pozisyonu yukarı çek
                translationY = -height * position * 0.8f

                // Arkadakileri hafif karart (Opaklık)
                alpha = 1f - (absPos * 0.3f)
            } else { // Öne doğru kayıp giden kart
                translationY = 0f
                alpha = 1f
                scaleX = 1f
                scaleY = 1f
            }

            // Görünürlük sırasını (Z-index) yönet
            translationZ = -absPos
        }
    }
}