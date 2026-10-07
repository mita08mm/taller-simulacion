import java.util.Scanner;

public class TransformadaInversa {

    // Paso 4: x = 3 + raiz_cubica(54*R - 27)
    public static double generarX() {
        double R = Math.random();
        double x = 3 + Math.cbrt(54 * R - 27);
        return x;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Ingrese el numero de corridas n: ");
        int n = sc.nextInt();

        System.out.println("\nCorrida\tR\t\tx");
        System.out.println("-------\t--------\t--------");
        for (int i = 1; i <= n; i++) {
            double R = Math.random();
            double x = 3 + Math.cbrt(54 * R - 27);
            System.out.printf("%d\t%.4f\t\t%.4f%n", i, R, x);
        }
        sc.close();
    }
}
