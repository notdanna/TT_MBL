package com.descortezadores.ecoverde.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.descortezadores.ecoverde.domain.model.FoliageZoneRecord

/**
 * Entidad Room para la tabla `zona_follaje` (`RN-47`, `RN-49`).
 */
@Entity(
    tableName = "zona_follaje",
    indices = [
        Index(value = ["nombre_zona"])
    ]
)
data class ZonaFollajeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_zona")
    val idZona: String,

    @ColumnInfo(name = "nombre_zona")
    val nombreZona: String,

    @ColumnInfo(name = "latitud_centro")
    val latitudCentro: Double,

    @ColumnInfo(name = "longitud_centro")
    val longitudCentro: Double,

    @ColumnInfo(name = "radio_metros")
    val radioMetros: Double = 50.0,

    @ColumnInfo(name = "descripcion")
    val descripcion: String? = null,

    @ColumnInfo(name = "fecha_registro_iso")
    val fechaRegistroIso: String,

    @ColumnInfo(name = "id_cuenta")
    val idCuenta: String? = null,

    @ColumnInfo(name = "id_dispositivo")
    val idDispositivo: String? = null,

    @ColumnInfo(name = "pendiente_sincronizar")
    val pendienteSincronizar: Boolean = false
) {
    fun toDomainModel(): FoliageZoneRecord = FoliageZoneRecord(
        idZona = idZona,
        nombreZona = nombreZona,
        latitudCentro = latitudCentro,
        longitudCentro = longitudCentro,
        radioMetros = radioMetros,
        fechaRegistroIso = fechaRegistroIso,
        descripcion = descripcion,
        idCuenta = idCuenta,
        idDispositivo = idDispositivo
    )

    companion object {
        fun fromDomainModel(
            zone: FoliageZoneRecord,
            pendienteSincronizar: Boolean = false
        ): ZonaFollajeEntity = ZonaFollajeEntity(
            idZona = zone.idZona,
            nombreZona = zone.nombreZona,
            latitudCentro = zone.latitudCentro,
            longitudCentro = zone.longitudCentro,
            radioMetros = zone.radioMetros,
            descripcion = zone.descripcion,
            fechaRegistroIso = zone.fechaRegistroIso,
            idCuenta = zone.idCuenta,
            idDispositivo = zone.idDispositivo,
            pendienteSincronizar = pendienteSincronizar
        )
    }
}
