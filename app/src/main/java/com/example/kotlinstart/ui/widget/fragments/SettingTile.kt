import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun SettingTile(
    settingName: String,
    trailingContent: @Composable (() -> Unit) = {},
    onPressed: () -> Unit = {},
    onPressedText: String = "",

){

    ListItem(

        headlineContent = {


            Box {

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween
                ){

                    Text(
                        text = settingName,
                    )

                    TextButton(onClick = onPressed) {
                        Text(text = onPressedText)
                    }


                }



            }
        },

        trailingContent = trailingContent,
    )
}

