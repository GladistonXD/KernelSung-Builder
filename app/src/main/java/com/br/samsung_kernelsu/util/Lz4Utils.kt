package com.br.samsung_kernelsu.util

import android.util.Log
import net.jpountz.lz4.LZ4FrameInputStream
import java.io.File

object Lz4Utils {

    private const val TAG = "Lz4Utils"

    /**
     * Magic number do formato LZ4 Frame: 04 22 4D 18
     * Todo arquivo LZ4 válido começa com esses 4 bytes.
     */
    private val LZ4_MAGIC = byteArrayOf(0x04, 0x22, 0x4D, 0x18)

    /**
     * Verifica se um arquivo é LZ4 lendo o magic number (não o nome).
     * Retorna true só se os 4 primeiros bytes forem 04 22 4D 18.
     */
    fun isLz4File(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return try {
            file.inputStream().use { input ->
                val header = ByteArray(4)
                val read = input.read(header)
                if (read != 4) return false
                header.contentEquals(LZ4_MAGIC)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro lendo magic number: ${e.message}")
            false
        }
    }

    /**
     * Descomprime um arquivo LZ4 para [output].
     * Retorna true em caso de sucesso.
     */
    fun decompress(input: File, output: File): Boolean {
        return try {
            LZ4FrameInputStream(input.inputStream()).use { lz4In ->
                output.outputStream().use { out ->
                    lz4In.copyTo(out)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao descomprimir LZ4: ${e.message}", e)
            false
        }
    }

    /**
     * Copia um arquivo qualquer (não-LZ4) para o destino.
     */
    fun copy(input: File, output: File): Boolean {
        return try {
            input.inputStream().use { inStream ->
                output.outputStream().use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao copiar arquivo: ${e.message}", e)
            false
        }
    }
}