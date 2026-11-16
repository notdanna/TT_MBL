package com.descortezadores.ecoverde.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.descortezadores.ecoverde.domain.model.ClassificationOutcome
import com.descortezadores.ecoverde.domain.model.TreeRecord

/**
 * Entidad Room para la tabla `arbol` (`CU-M09`, `RF-19`, `RF-23`, `RN-35`..`RN-42`).
 */
@Entity(
    tableName = "arbol",
    indices = [
        Index(value = ["nombre_especifico"]),
        Index(value = ["especie"]),
        Index(value = ["ultimo_estado_fitosanitario"])
    ]
)
data class ArbolEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_arbol")
    val idArbol: String,

    @ColumnInfo(name = "nombre_especifico")
    val nombreEspecifico: String,

    @ColumnInfo(name = "especie")
    val especie: String,

    @ColumnInfo(name = "latitud")
    val latitud: Double,

    @ColumnInfo(name = "longitud")
    val longitud: Double,

    @ColumnInfo(name = "descripcion")
    val descripcion: String? = null,

    @ColumnInfo(name = "fecha_registro_iso")
    val fechaRegistroIso: String,

    @ColumnInfo(name = "ultimo_estado_fitosanitario")
    val ultimoEstadoFitosanitario: String = ClassificationOutcome.SANO.apiValue,

    @ColumnInfo(name = "id_cuenta")
    val idCuenta: String? = null,

    @ColumnInfo(name = "id_dispositivo")
    val idDispositivo: String? = null,

    @ColumnInfo(name = "pendiente_sincronizar")
    val pendienteSincronizar: Boolean = false
) {
    fun toDomainModel(): TreeRecord = TreeRecord(
        idArbol = idArbol,
        nombreEspecifico = nombreEspecifico,
        especie = especie,
        latitud = latitud,
        longitud = longitud,
        fechaRegistroIso = fechaRegistroIso,
        descripcion = descripcion,
        idCuenta = idCuenta,
        idDispositivo = idDispositivo,
        ultimoEstadoSanitario = ClassificationOutcome.fromApiValue(ultimoEstadoFitosanitario)
    )

    companion object {
        fun fromDomainModel(
            tree: TreeRecord,
            pendienteSincronizar: Boolean = false
        ): ArbolEntity = ArbolEntity(
            idArbol = tree.idArbol,
            nombreEspecifico = tree.nombreEspecifico,
            especie = tree.especie,
            latitud = tree.latitud ?: 19.4204,
            longitud = tree.longitud ?: -99.1819,
            descripcion = tree.descripcion,
            fechaRegistroIso = tree.fechaRegistroIso,
            ultimoEstadoFitosanitario = (tree.ultimoEstadoSanitario ?: ClassificationOutcome.SANO).apiValue,
            idCuenta = tree.idCuenta,
            idDispositivo = tree.idDispositivo,
            pendienteSincronizar = pendienteSincronizar
        )
    }
}
