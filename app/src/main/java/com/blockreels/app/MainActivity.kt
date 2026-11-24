package com.blockreels.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.blockreels.app.ui.BlockReelsApp
import com.blockreels.app.viewmodel.BlockReelsViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: BlockReelsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlockReelsApp(viewModel = viewModel)
        }
    }
}
