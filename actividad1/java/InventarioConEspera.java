import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class InventarioConEspera {

    static Random rnd = new Random();

    static final int[] VALORES_DEMANDA = {25, 26, 27, 28, 29, 30, 31, 32, 33, 34};
    static final double[] ACUM_DEMANDA = {0.02, 0.06, 0.12, 0.24, 0.44, 0.68, 0.83, 0.93, 0.98, 1.00};

    static final int[] VALORES_ENTREGA = {1, 2, 3, 4};
    static final double[] ACUM_ENTREGA = {0.20, 0.50, 0.75, 1.00};

    static final int[] VALORES_ESPERA = {0, 1, 2, 3, 4};
    static final double[] ACUM_ESPERA = {0.40, 0.60, 0.75, 0.90, 1.00};

    public static int generico(int[] valores, double[] acumulada) {
        double R = rnd.nextDouble();
        for (int i = 0; i < acumulada.length; i++) {
            if (R < acumulada[i]) {
                return valores[i];
            }
        }
        return valores[valores.length - 1];
    }

    public static int demandaDiaria() { return generico(VALORES_DEMANDA, ACUM_DEMANDA); }
    public static int tiempoEntrega() { return generico(VALORES_ENTREGA, ACUM_ENTREGA); }
    public static int tiempoEspera()  { return generico(VALORES_ESPERA, ACUM_ESPERA); }

    static class Backorder {
        int cantidad;
        int plazoLimite;
        Backorder(int c, int p) { cantidad = c; plazoLimite = p; }
    }

    public static double simularInventario(int q, int R, int dias) {
        int inventario = 100;
        boolean hayPedido = false;
        int diaLlegada = 0;
        int ordenes = 0;
        double sumaProm = 0.0;
        double costoEsperaOk = 0.0;
        double costoPerdida = 0.0;
        List<Backorder> backorders = new ArrayList<>();

        for (int dia = 1; dia <= dias; dia++) {
            // 1. Expirar backorders cuyo plazo ya pasó sin ser atendidos
            List<Backorder> vivos = new ArrayList<>();
            for (Backorder b : backorders) {
                if (dia > b.plazoLimite) {
                    costoPerdida += b.cantidad * 50.0;
                } else {
                    vivos.add(b);
                }
            }
            backorders = vivos;

            // 2. Si llega el pedido, sumarlo y atender backorders pendientes (FIFO)
            if (hayPedido && dia == diaLlegada) {
                inventario += q;
                hayPedido = false;
                List<Backorder> restantes = new ArrayList<>();
                for (Backorder b : backorders) {
                    if (inventario <= 0) { restantes.add(b); continue; }
                    int atendido = Math.min(b.cantidad, inventario);
                    inventario -= atendido;
                    costoEsperaOk += atendido * 20.0;
                    int resto = b.cantidad - atendido;
                    if (resto > 0) restantes.add(new Backorder(resto, b.plazoLimite));
                }
                backorders = restantes;
            }

            int invInicial = inventario;
            int demanda = demandaDiaria();
            double promedioDia;

            if (demanda <= inventario) {
                inventario -= demanda;
                promedioDia = (invInicial + inventario) / 2.0;
            } else {
                int faltante = demanda - inventario;
                // Igual que en p3 (Tabla 5.7 del Ejemplo 5.5): si el inventario se agota
                // antes de terminar el dia, el promedio es (invInicial/2)*(invInicial/demanda).
                promedioDia = (invInicial > 0) ? (invInicial / 2.0) * (invInicial / (double) demanda) : 0.0;
                inventario = 0;
                int te = tiempoEspera();
                if (te == 0) {
                    costoPerdida += faltante * 50.0;
                } else {
                    backorders.add(new Backorder(faltante, dia + te));
                }
            }

            sumaProm += promedioDia;

            if (inventario <= R && !hayPedido) {
                diaLlegada = dia + tiempoEntrega();
                hayPedido = true;
                ordenes++;
            }
        }

        double costoOrdenar = ordenes * 100.0;
        double costoInventario = sumaProm * (52.0 / dias);
        return costoOrdenar + costoInventario + costoEsperaOk + costoPerdida;
    }

    // Corre 20 dias imprimiendo cada paso del modelamiento (Paso 3: comparar R
    // contra F(x); Paso 4: tramo asignado) para las 3 variables (demanda, entrega,
    // espera), para verificar el codigo contra la Propuesta de solucion.
    public static void demostracionModelamiento(int q, int R) {
        int inventario = 100;
        boolean hayPedido = false;
        int diaLlegada = 0;
        List<Backorder> backorders = new ArrayList<>();

        System.out.println("=== Demostracion del modelamiento (Pasos 3-4), q=" + q + " R=" + R + " ===");
        for (int dia = 1; dia <= 20; dia++) {
            List<Backorder> vivos = new ArrayList<>();
            for (Backorder b : backorders) {
                if (dia > b.plazoLimite) {
                    System.out.println("Dia " + dia + ": vence backorder de " + b.cantidad + "u (costo $50 c/u)");
                } else {
                    vivos.add(b);
                }
            }
            backorders = vivos;

            if (hayPedido && dia == diaLlegada) {
                inventario += q;
                hayPedido = false;
                System.out.println("Dia " + dia + ": llega pedido, inventario += " + q);
                List<Backorder> restantes = new ArrayList<>();
                for (Backorder b : backorders) {
                    int atendido = Math.min(b.cantidad, inventario);
                    inventario -= atendido;
                    System.out.println("  cubre backorder " + atendido + "u (costo $20 c/u)");
                    int resto = b.cantidad - atendido;
                    if (resto > 0) restantes.add(new Backorder(resto, b.plazoLimite));
                }
                backorders = restantes;
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
                int faltante = demanda - inventario;
                inventario = 0;
                double Re = rnd.nextDouble();
                int te = 0;
                for (int i = 0; i < ACUM_ESPERA.length; i++) {
                    if (Re < ACUM_ESPERA[i]) { te = VALORES_ESPERA[i]; break; }
                    if (i == ACUM_ESPERA.length - 1) te = VALORES_ESPERA[i];
                }
                if (te == 0) {
                    System.out.printf("  faltante %du, R_espera=%.4f -> espera=0: PIERDE de inmediato ($50 c/u)%n", faltante, Re);
                } else {
                    backorders.add(new Backorder(faltante, dia + te));
                    System.out.printf("  faltante %du, R_espera=%.4f -> espera=%d dias, backorder hasta dia %d%n",
                            faltante, Re, te, dia + te);
                }
            }
            System.out.println("  inventario: " + invInicial + " -> " + inventario);

            if (inventario <= R && !hayPedido) {
                double Rt = rnd.nextDouble();
                int tent = 0;
                for (int i = 0; i < ACUM_ENTREGA.length; i++) {
                    if (Rt < ACUM_ENTREGA[i]) { tent = VALORES_ENTREGA[i]; break; }
                    if (i == ACUM_ENTREGA.length - 1) tent = VALORES_ENTREGA[i];
                }
                diaLlegada = dia + tent;
                hayPedido = true;
                System.out.printf("  se ordena q=%d, R_entrega=%.4f -> tiempo=%d, llega dia %d%n",
                        q, Rt, tent, diaLlegada);
            }
        }
        System.out.println();
    }

    public static void main(String[] args) {
        demostracionModelamiento(90, 40);

        int mejorQ = 0, mejorR = 0;
        double mejorCosto = Double.MAX_VALUE;

        for (int q = 130; q <= 230; q += 10) {
            for (int R = 90; R <= 140; R += 5) {
                double suma = 0;
                for (int anio = 0; anio < 400; anio++) {
                    suma += simularInventario(q, R, 260);
                }
                double promedio = suma / 400;
                if (promedio < mejorCosto) {
                    mejorCosto = promedio;
                    mejorQ = q;
                    mejorR = R;
                }
            }
        }

        System.out.printf("Par optimo: q=%d, R=%d, costo promedio anual = %.2f%n",
                mejorQ, mejorR, mejorCosto);
    }
}
