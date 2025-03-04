import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RoundedButton(
    name: String,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
){
    Box(
        modifier = Modifier.padding(
            horizontal = 12.dp
        )
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier
                .background(color = Color.LightGray, shape = CircleShape)
                .border(
                    width = 3.dp,
                    color = Color.LightGray,
                    shape = CircleShape
                )
                .fillMaxWidth() then modifier,

            content = { Text(text = name) }
        )
    }
}