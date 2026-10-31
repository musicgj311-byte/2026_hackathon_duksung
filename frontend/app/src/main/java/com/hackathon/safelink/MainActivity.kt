package com.hackathon.safelink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.hackathon.safelink.ui.theme.SafeLinkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SafeLinkTheme {
                SafeLinkApp()
            }
        }
    }
}

@Composable
fun SafeLinkApp() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "안심문자",
            fontSize = 28.sp
        )

        Text(
            text = "AI 기반 사기 문자 감지 서비스",
            fontSize = 16.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SafeLinkPreview() {
    SafeLinkTheme {
        SafeLinkApp()
    }
}
