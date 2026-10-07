import java.util.Random;

public class InventarioSimple {

    static Random rnd = new Random();

    // Tablas de Transformada Inversa: valores y su acumulada F(x).
    // El mismo generico() sirve para cualquier tabla, sin importar su tamaño.
    static final int[] VALORES_DEMANDA = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    static final double[] ACUM_DEMANDA = {0.04, 0.10, 0.20, 0.40, 0.70, 0.88, 0.96, 0.99, 1.00};

    static final int[] VALORES_ENTREGA = {1, 2, 3, 4};
    static final double[] ACUM_ENTREGA = {0.25, 0.75, 0.95, 1.00};

    // Transformada Inversa genérica: recorre la acumulada y devuelve
    // el primer valor cuyo F(x) sea mayor que R generado.
    public static int generico(int[] valores, double[] acumulada) {
        double R = rnd.nextDouble();
        for (int i = 0; i < acumulada.length; i++) {
            if (R < acumulada[i]) {
                return valores[i];
            }
        }
        return valores[valores.length - 1];
    }

    public static int demandaDiaria() {
        return generico(VALORES_DEMANDA, ACUM_DEMANDA);
    }

    public static int tiempoEntrega() {
        return generico(VALORES_ENTREGA, ACUM_ENTREGA);
    }

    // Simula un año (260 días) para un par (q, R) y devuelve el costo total anual
    public static double simularInventario(int q, int R, int dias) {
        int inventario = 15;
        int faltanteAcum = 0;
        int ordenes = 0;
        boolean hayPedido = false;
        int diaLlegada = 0;
        double sumaProm = 0.0;

        for (int dia = 1; dia <= dias; dia++) {
            if (hayPedido && dia == diaLlegada) {
                inventario += q;
                hayPedido = false;
            }

            int invInicial = inventario;
            int demanda = demandaDiaria();
            double promedioDia;

            if (demanda <= inventario) {
                inventario -= demanda;
                promedioDia = (invInicial + inventario) / 2.0;
            } else {
                faltanteAcum += (demanda - inventario);
                // El inventario se agota antes de terminar el dia: el promedio no es
                // (invInicial+0)/2, es el promedio de la parte con stock (invInicial/2)
                // multiplicado por la fraccion del dia que duro esa parte (invInicial/demanda),
                // igual que la Tabla 5.7 del Ejemplo 5.5 (ej. 4=(19/2)(19/48)).
                promedioDia = (invInicial > 0) ? (invInicial / 2.0) * (invInicial / (double) demanda) : 0.0;
                inventario = 0;
            }

            sumaProm += promedioDia;

            if (inventario <= R && !hayPedido) {
                diaLlegada = dia + tiempoEntrega();
                hayPedido = true;
                ordenes++;
            }
        }

        double costoOrdenar = ordenes * 50.0;
        double costoInventario = sumaProm * (26.0 / dias);
        double costoFaltante = faltanteAcum * 25.0;

        return costoOrdenar + costoInventario + costoFaltante;
    }

    // Corre 13 días imprimiendo cada paso del modelamiento (Paso 3: comparar R contra
    // F(x); Paso 4: tramo asignado) para que se pueda verificar en la defensa que el
    // código hace exactamente lo mismo que la Propuesta de solución, no otra cosa.
    public static void demostracionModelamiento(int q, int R) {
        int inventario = 15;
        boolean hayPedido = false;
        int diaLlegada = 0;

        System.out.println("=== Demostracion del modelamiento (Pasos 3-4), q=" + q + " R=" + R + " ===");
        for (int dia = 1; dia <= 13; dia++) {
            if (hayPedido && dia == diaLlegada) {
                inventario += q;
                hayPedido = false;
                System.out.println("Dia " + dia + ": llega pedido, inventario += " + q);
            }

            int invInicial = inventario;
            double Rd = rnd.nextDouble();
            int demanda = 0;
            for (int i = 0; i < ACUM_DEMANDA.length; i++) {
                if (Rd < ACUM_DEMANDA[i]) { demanda = VALORES_DEMANDA[i]; break; }
                if (i == ACUM_DEMANDA.length - 1) demanda = VALORES_DEMANDA[i];
            }
            System.out.printf("Dia %d: R=%.4f (Paso 3: comparar contra F(x)) -> x=%d (Paso 4: demanda)%n",
                    dia, Rd, demanda);

            if (demanda <= inventario) {
                inventario -= demanda;
            } else {
                inventario = 0;
                System.out.println("  faltante = " + (demanda - invInicial));
            }
            System.out.println("  inventario: " + invInicial + " -> " + inventario);

            if (inventario <= R && !hayPedido) {
                double Re = rnd.nextDouble();
                int te = 0;
                for (int i = 0; i < ACUM_ENTREGA.length; i++) {
                    if (Re < ACUM_ENTREGA[i]) { te = VALORES_ENTREGA[i]; break; }
                    if (i == ACUM_ENTREGA.length - 1) te = VALORES_ENTREGA[i];
                }
                diaLlegada = dia + te;
                hayPedido = true;
                System.out.printf("  inventario <= R=%d: se ordena q=%d, R_entrega=%.4f -> tiempo=%d, llega dia %d%n",
                        R, q, Re, te, diaLlegada);
            }
        }
        System.out.println();
    }

    public static void main(String[] args) {
        // Primero, demostración trazable del modelamiento (Pasos 3-4) con valores
        // de partida, para poder verificar el código contra la Propuesta de solución.
        demostracionModelamiento(30, 10);

        // Búsqueda del par (q, R) óptimo: se prueban varios pares candidatos,
        // promediando el costo anual sobre muchos años simulados por par.
        int mejorQ = 0, mejorR = 0;
        double mejorCosto = Double.MAX_VALUE;

        for (int q = 40; q <= 90; q += 2) {
            for (int R = 0; R <= 24; R += 2) {
                double suma = 0;
                for (int anio = 0; anio < 300; anio++) {
                    suma += simularInventario(q, R, 260);
                }
                double promedio = suma / 300;
                if (promedio < mejorCosto) {
                    mejorCosto = promedio;
                    mejorQ = q;
                    mejorR = R;
                }
            }
        }

        System.out.printf("Par optimo encontrado: q=%d, R=%d, costo promedio anual = %.2f%n",
                mejorQ, mejorR, mejorCosto);
    }
}
