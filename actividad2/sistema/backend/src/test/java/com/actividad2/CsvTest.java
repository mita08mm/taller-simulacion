package com.actividad2;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de caja blanca de Csv.java: separador, comillas y filas vacias. */
class CsvTest {

    @Test
    void leeUnCsvSimple() {
        List<Map<String, String>> filas = Csv.leer("nombre,edad\nAna,20\nLuis,25\n");
        assertEquals(2, filas.size());
        assertEquals("Ana", filas.get(0).get("nombre"));
        assertEquals("25", filas.get(1).get("edad"));
    }

    @Test
    void campoEntreComillasConComaAdentroNoSeParteEnDosColumnas() {
        List<Map<String, String>> filas = Csv.leer("nombre,comentario\nAna,\"Hola, mundo\"\n");
        assertEquals(1, filas.size());
        assertEquals("Hola, mundo", filas.get(0).get("comentario"));
    }

    @Test
    void comillasEscapadasDentroDeUnCampo() {
        // En CSV, "" dentro de un campo entre comillas representa una comilla literal
        List<Map<String, String>> filas = Csv.leer("texto\n\"dijo \"\"hola\"\"\"\n");
        assertEquals("dijo \"hola\"", filas.get(0).get("texto"));
    }

    @Test
    void detectaPuntoYComaCuandoHayMasQueComasEnElEncabezado() {
        // Excel en espanol exporta con ';'; se detecta comparando el encabezado
        List<Map<String, String>> filas = Csv.leer("a;b;c\n1;2;3\n");
        assertEquals("1", filas.get(0).get("a"));
        assertEquals("3", filas.get(0).get("c"));
    }

    @Test
    void filasCompletamenteVaciasSeDescartan() {
        List<Map<String, String>> filas = Csv.leer("a,b\n1,2\n,\n3,4\n");
        assertEquals(2, filas.size());
    }

    @Test
    void valoresFaltantesAlFinalDeLaFilaQuedanVacios() {
        List<Map<String, String>> filas = Csv.leer("a,b,c\n1\n");
        assertEquals("1", filas.get(0).get("a"));
        assertEquals("", filas.get(0).get("b"));
        assertEquals("", filas.get(0).get("c"));
    }

    @Test
    void escribirYLeerSonInversosParaValoresConSeparadorYComillas() {
        List<String> columnas = List.of("a", "b");
        List<List<String>> filas = List.of(List.of("normal", "con, coma"), List.of("con \"comillas\"", ""));
        String csv = Csv.escribir(columnas, filas);

        List<Map<String, String>> leidas = Csv.leer(csv);
        assertEquals(2, leidas.size());
        assertEquals("con, coma", leidas.get(0).get("b"));
        assertEquals("con \"comillas\"", leidas.get(1).get("a"));
        assertEquals("", leidas.get(1).get("b"));
    }

    @Test
    void bomInicialSeDescarta() {
        // Se arma en tiempo de ejecucion (no como escape literal) para que el archivo
        // fuente no quede con el byte BOM incrustado.
        String bom = String.valueOf((char) 0xFEFF);
        List<Map<String, String>> filas = Csv.leer(bom + "a,b\n1,2\n");
        assertEquals("1", filas.get(0).get("a"));
    }
}
