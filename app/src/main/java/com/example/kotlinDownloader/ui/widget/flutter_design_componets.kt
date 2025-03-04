import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp



class FlutterDesignWidget{
    companion object{

        @Composable
        @OptIn(ExperimentalMaterial3Api::class)
        fun AppBar(
            modifier: Modifier = Modifier,
            onClick: () -> Unit = {},
            title: @Composable () -> Unit = {},
            actions: @Composable (RowScope.() -> Unit) = {},


        ){
            TopAppBar(
                title = title,
                actions = actions,
                navigationIcon = {
                    IconButton( onClick = onClick ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back to Previous Page"
                        )
                    }
                },
                modifier = modifier

            )
        }


        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        fun ListTile(
            title: String,
            onClick: () -> Unit = {},
            onLongClick: () -> Unit = {},
        ){
            Box(
                modifier = Modifier
                    .combinedClickable(
                        enabled = true,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                    .padding(vertical = 12.dp)
                    .fillMaxWidth()
            ){
                Text(
                    text = title,
                    fontSize = 16.sp
                )
            }


        }

    }
}