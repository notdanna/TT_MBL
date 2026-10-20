package com.descortezadores.ecoverde.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.descortezadores.ecoverde.domain.model.InfestationFocus

/**
 * Entidad Room para almacenar localmente los Focos de Infestación Activos calculados según
 * la NOM-019-SEMARNAT-2017 (`RF-14`, `RN-25`..`RN-29`).
 *
 * Permite disparar alertas geoespaciales offline al acercarse a `< 50 m` (`RN-28`).
 */
@Entity(tableName = "foco_infestacion")
data class FocoInfestacionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_foco")
    val idFoco: String,

    @ColumnInfo(name = "latitud_centro")
    val latitudCentro: Double,

    @ColumnInfo(name = "longitud_centro")
    val longitudCentro: Double,

    @ColumnInfo(name = "radio_metros")
    val radioMetros: Double = 17.84,

    @ColumnInfo(name = "cantidad_arboles_infectados")
    val cantidadArbolesInfectados: Int,

    @ColumnInfo(name = "ids_arboles_csv")
    val idsArbolesCsv: String,

    @ColumnInfo(name = "nombres_arboles_csv")
    val nombresArbolesCsv: String,

    @ColumnInfo(name = "fecha_actualizacion_millis")
    val fechaActualizacionMillis: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): InfestationFocus = InfestationFocus(
        idFoco = idFoco,
        latitudCentro = latitudCentro,
        longitudCentro = longitudCentro,
        radioMetros = radioMetros,
        cantidadArbolesInfectados = cantidadArbolesInfectados.coerceAtLeast(3),
        idsArboles = idsArbolesCsv.split("|").map { it.trim() }.filter { it.isNotEmpty() },
        nombresArboles = nombresArbolesCsv.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    )

    companion object {
        fun fromDomainModel(focus: InfestationFocus): FocoInfestacionEntity = FocoInfestacionEntity(
            idFoco = focus.idFoco,
            latitudCentro = focus.latitudCentro,
            longitudCentro = focus.longitudCentro,
            radioMetros = focus.radioMetros,
            cantidadArbolesInfectados = focus.cantidadArbolesInfectados,
            idsArbolesCsv = focus.idsArboles.joinToString("|"),
            nombresArbolesCsv = focus.nombresArboles.joinToString("|"),
            fechaActualizacionMillis = System.currentTimeMillis()
        )
    }
}
