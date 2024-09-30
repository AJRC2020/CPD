import java.util.Scanner;
import java.lang.Math;

public class multi {

    public static void onMult(int row, int col) {
        double[] pha = new double[row * col];
        double[] phb = new double[row * col];
        double[] phc = new double[row * col];

        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                pha[i * row + j] = 1.0;
                phb[i * row + j] = i + 1;
                phc[i * row + j] = 0.0;
            }
        }

        double startTime = System.currentTimeMillis();

        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                for (int k = 0; k < col; k++) {
                    phc[i * row + j] += pha[i * row + k] * phb[k * row + j];
                }
            }
        }

        double endTime = System.currentTimeMillis();

        double time = (endTime - startTime) / 1000;
        double gflops = 2 * Math.pow(row, 3) / time / Math.pow(10, 9);

        System.out.println("Time: " + time + " seconds\n");
        System.out.println("Gflops: " + gflops + "\n");

        for (int j = 0; j < 10; j++){
            System.out.println(phc[j] + " ");
        }
        System.out.println("\n");

    }

    public static void onMultLine(int row, int col) {
        double[] pha = new double[row * col];
        double[] phb = new double[row * col];
        double[] phc = new double[row * col];

        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                pha[i * row + j] = 1.0;
                phb[i * row + j] = i + 1;
                phc[i * row + j] = 0.0;
            }
        }

        double startTime = System.currentTimeMillis();

        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                for (int k = 0; k < col; k++) {
                    phc[i * row + k] += pha[i * row + j] * phb[j * row + k];
                }
            }
        }

        double endTime = System.currentTimeMillis();

        double time = (endTime - startTime) / 1000;
        double gflops = 2 * Math.pow(row, 3) / time / Math.pow(10, 9);

        System.out.println("Time: " + time + " seconds\n");
        System.out.println("Gflops: " + gflops + "\n");

        for (int j = 0; j < 10; j++){
            System.out.println(phc[j] + " ");
        }
        System.out.println("\n");
    }

    public static void onMultBlock(int row, int col, int bkSize) {
        double[] pha = new double[row * col];
        double[] phb = new double[row * col];
        double[] phc = new double[row * col];

        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                pha[i * row + j] = 1.0;
                phb[i * row + j] = i + 1;
                phc[i * row + j] = 0.0;
            }
        }

        double startTime = System.currentTimeMillis();

        for (int i = 0; i < row; i += bkSize) {
            for (int j = 0; j < col; j += bkSize) {
                for (int k = 0; k < col; k += bkSize) {
                    for (int a = i; a < i + bkSize; a++) {
                        for (int b = j; b < j + bkSize; b++) {
                            for (int c = k; c < k + bkSize; c++) {
                                phc[a * row + c] += pha[a * row + b] * phb[b * row + c];
                            }
                        }
                    }
                }
            }
        }

        double endTime = System.currentTimeMillis();

        double time = (endTime - startTime) / 1000;
        double gflops = 2 * Math.pow(row, 3) / time / Math.pow(10, 9);

        System.out.println("Time: " + time + " seconds\n");
        System.out.println("Gflops: " + gflops + "\n");

        for (int j = 0; j < 10; j++){
            System.out.println(phc[j] + " ");
        }
        System.out.println("\n");
    }

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        boolean check = true;

        while (check) {
            System.out.println("1. Multiplication\n2. Line Multiplication\n3. Block Multiplication\n0. Exit\nSelection?:");
            int option = scan.nextInt();
            int col;

            switch(option) {
                case 1:
                    System.out.println("Dimensions?: col=row: ");
                    col = scan.nextInt();
                    onMult(col, col);
                    break;

                case 2:
                    System.out.println("Dimensions?: col=row: ");
                    col = scan.nextInt();
                    onMultLine(col, col);
                    break;

                case 3:
                    System.out.println("Dimensions?: col=row: ");
                    col = scan.nextInt();
                    System.out.println("Block size?: ");
                    int bkSize = scan.nextInt();
                    onMultBlock(col, col, bkSize);
                    break;

                case 0:
                    check = false;
                    break;

                default:
                    check = false;
                    break;
            }
        }

        scan.close();
    }
}