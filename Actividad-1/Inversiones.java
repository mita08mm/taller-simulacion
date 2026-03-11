import java.util.Random;
import java.util.Scanner;

public class Inversiones {

    public static void main(String[] args) {

        Random rand = new Random();
        Scanner sc = new Scanner(System.in);

        double proyectosExitosos = 0;

        System.out.println("Ingrese el número de simulaciones: ");
        int nroSimulaciones = sc.nextInt();

        System.out.println("Ingrese la inversión inicial: ");
        double inversionInicial = sc.nextDouble();

        System.out.println("Ingrese el número de años del proyecto: ");
        int años = sc.nextInt();

        System.out.println("Ingrese el flujo mínimo anual esperado: ");
        double flujoMin = sc.nextDouble();

        System.out.println("Ingrese el flujo máximo anual esperado: ");
        double flujoMax = sc.nextDouble();

        System.out.println("Ingrese la tasa de descuento (%): ");
        double tasaDescuento = sc.nextDouble() / 100;

        for(int i = 0; i < nroSimulaciones; i++){

            double VAN = -inversionInicial;

            System.out.println("\nSimulación: " + (i+1));

            for(int año = 1; año <= años; año++){

                double flujo = flujoMin + (flujoMax - flujoMin) * rand.nextDouble();

                double flujoDescontado = flujo / Math.pow((1 + tasaDescuento), año);

                VAN += flujoDescontado;

                System.out.println(" Año: " + año +
                        " | Flujo generado: " + String.format("%.2f", flujo) +
                        " | Flujo descontado: " + String.format("%.2f", flujoDescontado));
            }

            System.out.println(" VAN obtenido: " + String.format("%.2f", VAN));

            if (VAN > 0){
                System.out.println(" ¿El proyecto es rentable? SI");
                proyectosExitosos++;
            } else {
                System.out.println(" ¿El proyecto es rentable? NO");
            }

            System.out.println("-------------------------------------------------------");
        }

        double probabilidad = proyectosExitosos / nroSimulaciones;

        System.out.println("\nRESULTADOS FINALES");
        System.out.println("Proyectos rentables: " + proyectosExitosos +
                " de " + nroSimulaciones);
        System.out.println("Probabilidad de éxito: " +
                String.format("%.2f", probabilidad * 100) + "%");

        sc.close();
    }
}