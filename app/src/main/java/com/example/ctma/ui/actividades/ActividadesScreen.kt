package com.example.ctma.ui.actividades

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ctma.domain.ActividadFormativa
import com.example.ctma.repository.ListadoUiState
import com.example.ctma.repository.OperacionUiState
import com.example.ctma.viewmodel.ActividadViewModel

@Composable
fun ActividadesScreen(
    viewModel: ActividadViewModel,
    onVolver: () -> Unit
) {
    val listadoState by viewModel.listadoUiState.collectAsState()
    val operacionState by viewModel.operacionUiState.collectAsState()

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Actividades Formativas") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Text("🔄")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Estado de operación / Banner de Red y Caché
            BannerEstadoOperacion(
                state = operacionState,
                onReintentar = { viewModel.reintentar() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de Contenido desde Room
            when (val state = listadoState) {
                is ListadoUiState.Cargando -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ListadoUiState.Vacio -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No hay actividades almacenadas en el dispositivo.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is ListadoUiState.Contenido -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.actividades) { actividad ->
                            ItemActividad(actividad = actividad)
                        }
                    }
                }
                is ListadoUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.mensaje, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Button(
                onClick = onVolver,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Volver al Catálogo")
            }
        }
    }
}

@Composable
fun BannerEstadoOperacion(
    state: OperacionUiState,
    onReintentar: () -> Unit
) {
    when (state) {
        is OperacionUiState.EnCurso -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sincronizando con el servidor...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        is OperacionUiState.Fallida -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.mensaje,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = onReintentar) {
                        Text("Reintentar")
                    }
                }
            }
        }
        is OperacionUiState.Exitosa -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "✓ Actualizado correctamente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        is OperacionUiState.Inactiva -> {}
    }
}

@Composable
fun ItemActividad(actividad: ActividadFormativa) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = actividad.titulo, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = actividad.descripcion, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AssistChip(onClick = {}, label = { Text(actividad.estado) })
                Text(
                    text = "Límite: ${actividad.fechaLimite}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}
