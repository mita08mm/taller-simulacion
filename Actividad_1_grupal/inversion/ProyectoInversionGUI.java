import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ProyectoInversionGUI extends JFrame {

    private JTextField simulacionesField = new JTextField("500");
    private JTextField inversionField = new JTextField("1000");
    private JTextField anosField = new JTextField("5");
    private JTextField flujoMinField = new JTextField("100");
    private JTextField flujoMaxField = new JTextField("500");
    private JTextField tasaField = new JTextField("15");

    private JTextArea resultadoArea = new JTextArea();
    private JProgressBar barra = new JProgressBar();

    private GraficoLinea graficoLinea = new GraficoLinea();
    private GraficoHistograma graficoHist = new GraficoHistograma();
    private GraficoCDF graficoCDF = new GraficoCDF();

    public ProyectoInversionGUI() {

        setTitle("Simulación de Proyecto de Inversión - Monte Carlo");
        setSize(1000,650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panelEntrada = new JPanel(new GridLayout(7,2,8,8));

        panelEntrada.add(new JLabel("Número de simulaciones:"));
        panelEntrada.add(simulacionesField);

        panelEntrada.add(new JLabel("Inversión inicial:"));
        panelEntrada.add(inversionField);

        panelEntrada.add(new JLabel("Número de años:"));
        panelEntrada.add(anosField);

        panelEntrada.add(new JLabel("Flujo mínimo anual:"));
        panelEntrada.add(flujoMinField);

        panelEntrada.add(new JLabel("Flujo máximo anual:"));
        panelEntrada.add(flujoMaxField);

        panelEntrada.add(new JLabel("Tasa de descuento (%):"));
        panelEntrada.add(tasaField);

        JButton ejecutarBtn = new JButton("Ejecutar Simulación");
        panelEntrada.add(ejecutarBtn);

        add(panelEntrada, BorderLayout.NORTH);

        resultadoArea.setEditable(false);
        resultadoArea.setFont(new Font("Monospaced",Font.PLAIN,13));

        JScrollPane scroll = new JScrollPane(resultadoArea);

        JTabbedPane graficos = new JTabbedPane();
        graficos.add("VAN por simulación", graficoLinea);
        graficos.add("Histograma VAN", graficoHist);
        graficos.add("Probabilidad acumulada", graficoCDF);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, graficos);
        split.setDividerLocation(380);

        add(split, BorderLayout.CENTER);

        barra.setStringPainted(true);
        add(barra, BorderLayout.SOUTH);

        ejecutarBtn.addActionListener(e -> ejecutarSimulacion());
    }


    private void ejecutarSimulacion() {

        try{

            Random rand = new Random();

            int simulaciones = Integer.parseInt(simulacionesField.getText());
            double inversion = Double.parseDouble(inversionField.getText());
            int anos = Integer.parseInt(anosField.getText());
            double flujoMin = Double.parseDouble(flujoMinField.getText());
            double flujoMax = Double.parseDouble(flujoMaxField.getText());
            double tasa = Double.parseDouble(tasaField.getText())/100;

            int exitos = 0;
            double VANpromedio = 0;

            java.util.List<Double> listaVAN = new ArrayList<>();

            resultadoArea.setText("--- INICIO DE SIMULACIÓN ---\n\n");

            barra.setMaximum(simulaciones);
            barra.setValue(0);

            for(int i=0;i<simulaciones;i++){

                double VAN = -inversion;

                for(int a=1;a<=anos;a++){

                    double flujo = flujoMin + (flujoMax-flujoMin)*rand.nextDouble();
                    double flujoDesc = flujo/Math.pow((1+tasa),a);

                    VAN += flujoDesc;
                }

                listaVAN.add(VAN);
                VANpromedio += VAN;

                if(VAN>0){
                    exitos++;
                }

                barra.setValue(i+1);
            }

            VANpromedio = VANpromedio/simulaciones;

            double prob = (double)exitos/simulaciones*100;

            double desviacion = calcularDesviacion(listaVAN, VANpromedio);

            resultadoArea.append("Simulaciones realizadas: "+simulaciones+"\n");
            resultadoArea.append("Proyectos rentables: "+exitos+"\n");
            resultadoArea.append("Probabilidad de éxito: "+String.format("%.2f",prob)+"%\n");
            resultadoArea.append("VAN promedio: "+String.format("%.2f",VANpromedio)+"\n");
            resultadoArea.append("Desviación estándar (riesgo): "+String.format("%.2f",desviacion)+"\n\n");

            if(prob>70){
                resultadoArea.append("Interpretación: Alta probabilidad de éxito.\n");
            }
            else if(prob>40){
                resultadoArea.append("Interpretación: Riesgo medio.\n");
            }
            else{
                resultadoArea.append("Interpretación: Proyecto riesgoso.\n");
            }

            graficoLinea.setDatos(listaVAN);
            graficoHist.setDatos(listaVAN);
            graficoCDF.setDatos(listaVAN);

            graficoLinea.repaint();
            graficoHist.repaint();
            graficoCDF.repaint();

        }
        catch(Exception ex){

            JOptionPane.showMessageDialog(this,
                    "Ingrese valores numéricos válidos",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    private double calcularDesviacion(List<Double> datos, double promedio){

        double suma = 0;

        for(double v: datos){
            suma += Math.pow(v-promedio,2);
        }

        return Math.sqrt(suma/datos.size());
    }


    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            new ProyectoInversionGUI().setVisible(true);
        });

    }
}


class GraficoLinea extends JPanel{

    private java.util.List<Double> datos = new ArrayList<>();

    public GraficoLinea(){
        setBackground(Color.white);
    }

    public void setDatos(java.util.List<Double> datos){
        this.datos = datos;
    }

    protected void paintComponent(Graphics g){

        super.paintComponent(g);

        if(datos==null || datos.isEmpty()){
            g.drawString("Ejecute la simulación",50,50);
            return;
        }

        int width = getWidth();
        int height = getHeight();

        double max = Collections.max(datos);
        double min = Collections.min(datos);

        if(max==min) max = min+1;

        int size = datos.size();

        int prevX = 0;
        int prevY = height/2;

        g.setColor(Color.blue);

        for(int i=0;i<size;i++){

            double v = datos.get(i);

            int x = (int)((double)i/size*width);
            int y = (int)(height - ((v-min)/(max-min))*height);

            g.drawLine(prevX, prevY, x, y);

            prevX = x;
            prevY = y;
        }

        g.setColor(Color.black);
        g.drawString("VAN por simulación",10,20);
    }
}


class GraficoHistograma extends JPanel{

    private java.util.List<Double> datos = new ArrayList<>();

    public GraficoHistograma(){
        setBackground(Color.white);
    }

    public void setDatos(java.util.List<Double> datos){
        this.datos = datos;
    }

    protected void paintComponent(Graphics g){

        super.paintComponent(g);

        if(datos==null || datos.isEmpty()){
            g.drawString("Ejecute la simulación",50,50);
            return;
        }

        int bins = 20;

        double max = Collections.max(datos);
        double min = Collections.min(datos);

        double rango = max-min;

        int[] conteo = new int[bins];

        for(double v: datos){

            int indice = (int)((v-min)/rango * (bins-1));
            conteo[indice]++;
        }

        int width = getWidth();
        int height = getHeight();

        int maxCount = Arrays.stream(conteo).max().getAsInt();

        int barWidth = width/bins;

        g.setColor(new Color(100,150,255));

        for(int i=0;i<bins;i++){

            int barHeight = (int)((double)conteo[i]/maxCount * (height-40));

            int x = i*barWidth;
            int y = height-barHeight-20;

            g.fillRect(x,y,barWidth-2,barHeight);
        }

        g.setColor(Color.black);
        g.drawString("Histograma del VAN",10,20);
    }
}


class GraficoCDF extends JPanel{

    private java.util.List<Double> datos = new ArrayList<>();

    public GraficoCDF(){
        setBackground(Color.white);
    }

    public void setDatos(java.util.List<Double> datos){

        this.datos = new ArrayList<>(datos);
        Collections.sort(this.datos);
    }

    protected void paintComponent(Graphics g){

        super.paintComponent(g);

        if(datos==null || datos.isEmpty()){
            g.drawString("Ejecute la simulación",50,50);
            return;
        }

        int width = getWidth();
        int height = getHeight();

        int n = datos.size();

        double max = Collections.max(datos);
        double min = Collections.min(datos);

        int prevX = 0;
        int prevY = height;

        g.setColor(Color.red);

        for(int i=0;i<n;i++){

            double v = datos.get(i);

            double prob = (double)i/n;

            int x = (int)((v-min)/(max-min) * width);
            int y = (int)(height - prob*height);

            g.drawLine(prevX,prevY,x,y);

            prevX = x;
            prevY = y;
        }

        g.setColor(Color.black);
        g.drawString("Probabilidad acumulada del VAN",10,20);
    }
}
