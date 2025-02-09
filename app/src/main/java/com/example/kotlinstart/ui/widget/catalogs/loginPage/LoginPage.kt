package com.example.kotlinstart.ui.widget.catalogs.loginPage

import RoundedButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

//这个东西么。。按道理说应该放在Route页面或者是 对应的Model页面里
val localAccountID = compositionLocalOf<Long?> { error("No userId provided") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPage(
    accountID : Long?
){


        var expandedStatus by remember { mutableStateOf(false) }

        Scaffold (
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "LoginPage")
                    },

                    actions = {
                        //Text(text = Date().toString())

//                        Box {
//
//                            IconButton(
//                                onClick ={
////                                    println("显示更多")
//                                    expandedStatus=true
//                                }
//                            ){
//                                Icon(Icons.Default.MoreVert, contentDescription ="menu" )
//                                expandedStatus=false
//                            }
//
//
//
//                            DropdownMenu(
//                                expanded = expandedStatus,
//                                onDismissRequest = { expandedStatus = false }
//                            ) {
//                                DropdownMenuItem(
//                                    text = { Text("1") },
//                                    onClick = { println("") }
//
//                                )
//                            }
//                        }

                        Box{

                            IconButton(onClick = { expandedStatus = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Localized description")
                            }
                            DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = { /* Handle edit! */ },
                                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = { /* Handle settings! */ },
                                    leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) }
                                )
                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text("Send Feedback") },
                                    onClick = { /* Handle send feedback! */ },
                                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                                    trailingIcon = { Text("F11", textAlign = TextAlign.Center) }
                                )
                            }
                        }

                    }
                )
            },
        ) {



            CompositionLocalProvider(
                localAccountID provides accountID,

            ) {
                Box(modifier = Modifier.padding(it)) {
                    Column {
                        InputForm()
                        SubmitButton()
                    }
                }
            }



        }



}

@Composable
@Preview
fun InputForm(){

//    val paddingW12Prop = Modifier.width(12.dp)
    val paddingH12 = @Composable { Spacer(modifier = Modifier.padding(12.dp)) }

    Column {

        Box(
            modifier = Modifier
                .background(color = Color.Blue, shape = CircleShape)
                .size(100.dp)
                .border(2.dp, Color.Gray, CircleShape)
                .align(Alignment.CenterHorizontally)
        )

        paddingH12()


        LoginField()

        TextField(
            shape = RoundedCornerShape(
                topEnd = 12.dp,
                topStart = 0.dp,
                bottomEnd= 12.dp,
                bottomStart = 0.dp,
            ),
            modifier =
                Modifier
                    .fillMaxWidth(fraction = 1f),

            value = "",
            onValueChange = { value -> println(value) },
            placeholder = {
                Text("密码")
            }

        )

        paddingH12()





    }
}


@Composable
@Preview
fun SubmitButton(){

    RoundedButton(
        onClick = { println("登录")},
        modifier = Modifier
            .background(
                color = Color(0.6f, 0.8f, 0.4f, 1.0f), shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = Color.Yellow,
                shape = CircleShape
            ),
        name = "test"
    )


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun LoginField(){

    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf("账号") }
    val loginOptions = listOf("账号", "邮箱", "电话")

    Row {

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },

        ){


            TextField(
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 0.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 0.dp,
                ),

                modifier = Modifier
//                    menuAnchor was deprecated
                    .menuAnchor(
                        type = MenuAnchorType.PrimaryNotEditable,
                        enabled = true
                    )
                    .width(100.dp),


                value = selectedOptionText,


                trailingIcon = { TrailingIcon(expanded = expanded) } ,

                onValueChange = {},
                readOnly = true,
                singleLine = true,




            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }


            ) {
                loginOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            selectedOptionText = option
                            expanded = false
                        },

                    )
                }

            }

        }


        TextField(
            shape = RoundedCornerShape(
                topEnd = 12.dp,
                topStart = 0.dp,
                bottomEnd= 12.dp,
                bottomStart = 0.dp,
            ),

            modifier =
            Modifier
                .fillMaxWidth(fraction = 1f),

            value = "${localAccountID.current}",
            onValueChange = { println(it) },
            textStyle = TextStyle(
                fontSize = 16.sp, color = Color.Blue
            ),
        )
    }

}