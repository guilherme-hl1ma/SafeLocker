package br.com.puc.pi3_es_2024_t2.models

/**
* Classe que armazena as coordenadas de um determinado armário.
 * Na LocationActivity, ao usuário selcionar um marcado e clicar
 * em alugar armário, é realizado o acesso das coordenadas desse marcador
 * e é inicializado um objeto dessa classe que será serializado em Json
 * e enviado para a HomeActivity que por sua vez envia para o RentLockerFragment.
 * @param latArmario Latitude do armário do tipo [Double]
 * @param lotArmario Longitude do armário do tipo [Double]
 * @author Guilherme
* */

data class LockerLocation (
    val latArmario: Double,
    val lotArmario: Double,
)