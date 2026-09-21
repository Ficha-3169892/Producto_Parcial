package com.example.ctma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ctma.navigation.AppNavigation
import com.example.ctma.ui.theme.PrestamoLabCTMAATheme
import com.example.ctma.viewmodel.PrestamoViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            PrestamoLabCTMAATheme {

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {

                    val prestamoViewModel: PrestamoViewModel = viewModel(
                        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                    )

                    AppNavigation(
                        viewModel = prestamoViewModel
                    )
                }
            }
        }
    }
}