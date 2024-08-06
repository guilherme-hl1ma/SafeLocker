package br.com.puc.pi3_es_2024_t2.models

/**
 * Classe para armazenar dados do Armário.
 * Representa a coleção ARMARIO do Firebase Firestore.
 * @param nomeArmario Nome da localização
 * @param descricao Referência do armário
 * @param latArmario Latitude do armário
 * @param lotArmario Longitude do armário
 * @param precos Lista de preços do armário
 * @author Guilherme
 * */
data class Locker(
    val nomeArmario: String,
    val descricao: String,
    val latArmario: Double,
    val lotArmario: Double,
    val precos: List<Double>
)
