import java.util.*;

interface ProbabilityCalculator {
    void calculate(int totalAttempts, double[] results);
}

class CoinProbabilityCalculator implements ProbabilityCalculator {
    private static final int NUM_SIDES = 2; 
    private Random rand = new Random();
    public void calculate(int totalAttempts, double[] results) {
        if (totalAttempts <= 0) {
            throw new IllegalArgumentException("El número debe ser mayor que 0.");
        }

        for (int i = 0; i < totalAttempts; i++) {
            int randomValue = rand.nextInt(NUM_SIDES) + 1;
            results[randomValue - 1]++;
            String resultText = (randomValue == 1) ? "Cara" : "Cruz";
            System.out.print(resultText + (i < totalAttempts - 1 ? ", " : "\n"));
        }

        System.out.printf("%% Cara = %.2f%%\n", (results[0] / totalAttempts) * 100);
        System.out.printf("%% Cruz = %.2f%%\n", (results[1] / totalAttempts) * 100);
    }
}

class DiceProbabilityCalculator implements ProbabilityCalculator {
    private static final int NUM_SIDES = 6; 
    private static final int NUM_DICE = 2;  
    private Random rand = new Random();
    public void calculate(int totalAttempts, double[] results) {
        if (totalAttempts <= 0) {
            throw new IllegalArgumentException("El número de lanzamientos debe ser mayor que 0.");
        }
        double[][] diceResults = new double[NUM_DICE][NUM_SIDES];

        for (int i = 0; i < totalAttempts; i++) {
            System.out.print("Lanzamiento " + (i + 1) + ": ");
            for (int j = 0; j < NUM_DICE; j++) {
                int randomValue = rand.nextInt(NUM_SIDES) + 1;
                diceResults[j][randomValue - 1]++;
                System.out.print("Dado " + (j + 1) + " = " + randomValue + " ");
            }
            System.out.println();
        }

        for (int j = 0; j < NUM_DICE; j++) {
            System.out.println("Probabilidades del Dado " + (j + 1) + ":");
            for (int k = 0; k < NUM_SIDES; k++) {
                System.out.printf("%% Valor %d = %.2f%%\n", (k + 1), (diceResults[j][k] / totalAttempts) * 100);
            }
        }
    }
}

public class ProbabilityGame {
    private static final String MENU = "1. Moneda\n2. Dados";
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        boolean continuePlaying = true;

        while (continuePlaying) {
            ProbabilityCalculator calculator = null;

            int gameMode = -1;
            while (gameMode < 1 || gameMode > 2) {
                System.out.println(MENU);
                System.out.print("Selecciona una opción (1 o 2): ");
                try {
                    gameMode = scanner.nextInt();
                    if (gameMode < 1 || gameMode > 2) {
                        System.out.println("Error: Ingresa un valor válido (1 o 2).");
                    }
                } catch (InputMismatchException e) {
                    System.out.println("Error: Debes ingresar un número entero.");
                    scanner.next(); 
                }
            }

            switch (gameMode) {
                case 1:
                    calculator = new CoinProbabilityCalculator();
                    break;
                case 2:
                    calculator = new DiceProbabilityCalculator();
                    break;
            }
            
            int totalAttempts = -1;
            while (totalAttempts <= 0) {
                System.out.print("Ingresa la cantidad de lanzamientos a realizar: ");
                try {
                    totalAttempts = scanner.nextInt();
                    if (totalAttempts <= 0) {
                        System.out.println("Error: El número de lanzamientos debe ser mayor que 0.");
                    }
                } catch (InputMismatchException e) {
                    System.out.println("Error: Debes ingresar un número entero.");
                    scanner.next();
                }
            }

            double[] results = new double[gameMode == 1 ? 2 : 6];
            calculator.calculate(totalAttempts, results);
            System.out.print("¿Deseas continuar? (s/n): ");
            String response = scanner.next().toLowerCase();
            continuePlaying = response.equals("s");
        }

        scanner.close(); 
        System.out.println("¡Gracias por jugar!");
    }
}