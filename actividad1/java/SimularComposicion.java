public class SimularComposicion {

    static double a = 1.5;
    static double b = 4.0;
    static double c = 6.5;
    static double A1 = (b - a) / (c - a); // 0.5

    public static void main(String[] args) {
        int[] corridas = {10, 25, 50, 100, 250, 500, 1000};
        for (int n : corridas) {
            simulacion(n);
        }
    }

    static void simulacion(int n) {
        System.out.println("\n=== n=" + n + " ===");
        System.out.println("Corrida\tR1\t\tR2\t\tSub\tx");
        double sumX = 0;
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
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
            sumX += x;
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (n <= 25) {
                System.out.printf("%d\t%.4f\t\t%.4f\t\t%s\t%.4f%n", i, R1, R2, sub, x);
            }
        }
        System.out.printf("Promedio=%.4f  Min=%.4f  Max=%.4f%n", sumX/n, minX, maxX);
    }
}
