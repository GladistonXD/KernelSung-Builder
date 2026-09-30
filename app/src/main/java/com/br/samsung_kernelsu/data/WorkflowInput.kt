package com.br.samsung_kernelsu.data

data class WorkflowInput(
    val name: String,
    val type: String,
    val description: String? = null,
    val required: Boolean = false,
    val default: String? = null,
    val options: List<String> = emptyList()
)