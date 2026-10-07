import java.util.Scanner;

public class Composicion {

    // Paso 7: x1 = a + (b-a)*sqrt(R2)  si R1 < A1
    //         x2 = c - (c-b)*sqrt(1-R2) si R1 >= A1
    public static double generarX(double a, double b, double c) {
        double R1 = Math.random();
        double R2 = Math.random();
        double A1 = (b - a) / (c - a);
        if (R1 < A1) {
            return a + (b - a) * Math.sqrt(R2);
        } else {
            return c - (c - b) * Math.sqrt(1 - R2);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Ingrese a (minimo): ");
        double a = sc.nextDouble();
        System.out.print("Ingrese b (moda):   ");
        double b = sc.nextDouble();
        System.out.print("Ingrese c (maximo): ");
        double c = sc.nextDouble();
        System.out.print("Ingrese el numero de corridas n: ");
        int n = sc.nextInt();

        double A1 = (b - a) / (c - a);
        System.out.printf("%na=%.2f  b=%.2f  c=%.2f  A1=%.4f%n%n", a, b, c, A1);
        System.out.println("Corrida\tR1\t\tR2\t\tSubfuncion\tx");
        System.out.println("-------\t--------\t--------\t----------\t--------");
        for (int i = 1; i <= n; i++) {
            double R1 = Math.random();
            double R2 = Math.random();
            double x;
            String sub;
            if (R1 < A1) {
                x = a + (b - a) * Math.sqrt(R2);
                sub = "f1";
            } else {
                x = c - (c - b) * Math.sqrt(1 - R2);
                sub = "f2";
            }
            System.out.printf("%d\t%.4f\t\t%.4f\t\t%s\t\t%.4f%n", i, R1, R2, sub, x);
        }
        sc.close();
    }
}
