package com.security.rakshakx.email.ui

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.security.rakshakx.email.database.ThreatDatabase
import com.security.rakshakx.email.database.ThreatEntity

import android.content.Context

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ThreatHistoryScreen(

    context: Context

) {

    var threats by remember {

        mutableStateOf<List<ThreatEntity>>(
            emptyList()
        )
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {

        scope.launch(Dispatchers.IO) {

            val database =
                ThreatDatabase.getDatabase(context)

            threats =
                database
                    .threatDao()
                    .getAllThreats()
        }
    }

    LazyColumn(

        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),

        verticalArrangement =
            Arrangement.spacedBy(12.dp)

    ) {

        items(threats) { threat ->

            Card(

                modifier = Modifier.fillMaxWidth()

            ) {

                Column(

                    modifier = Modifier
                        .padding(16.dp)

                ) {

                    Text(
                        text = threat.riskLevel,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Score: ${threat.riskScore}"
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = threat.message
                    )
                }
            }
        }
    }
}