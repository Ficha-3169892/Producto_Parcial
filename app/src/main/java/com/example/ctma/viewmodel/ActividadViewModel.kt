package com.example.ctma.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ctma.repository.ActividadRepository
import com.example.ctma.repository.ListadoUiState
import com.example.ctma.repository.OperacionUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActividadViewModel(
    private val repository: ActividadRepository
) : ViewModel() {

    private val _listadoUiState = MutableStateFlow<ListadoUiState>(ListadoUiState.Cargando)
    val listadoUiState: StateFlow<ListadoUiState> = _listadoUiState.asStateFlow()

    private val _operacionUiState = MutableStateFlow<OperacionUiState>(OperacionUiState.Inactiva)
    val operacionUiState: StateFlow<OperacionUiState> = _operacionUiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        observarDatosLocales()
        refresh()
    }

    private fun observarDatosLocales() {
        viewModelScope.launch {
            repository.observarActividades().collect { actividades ->
                if (actividades.isEmpty()) {
                    _listadoUiState.value = ListadoUiState.Vacio
                } else {
                    _listadoUiState.value = ListadoUiState.Contenido(actividades)
                }
            }
        }
    }

    fun refresh() {
        // Prevenir ejecuciones concurrentes / duplicadas
        if (_operacionUiState.value is OperacionUiState.EnCurso) return

        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _operacionUiState.value = OperacionUiState.EnCurso
            val resultado = repository.refresh()
            _operacionUiState.value = resultado
        }
    }

    fun reintentar() {
        refresh()
    }
}
