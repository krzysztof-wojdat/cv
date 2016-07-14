package bothack.agents.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by administrator on 12/10/14.
 */
class GUITest extends JFrame implements Runnable{


    private JPanel main;
    private final JPanel main1;

    private Map map1;
    private JScrollPane scroll;


    public GUITest(){
        main1 = new JPanel();
        StatsPane stats = new StatsPane();
        map1 = new Map();
        main1.setSize(615,550);
        main1.setBackground(Color.BLACK);
        main1.setLayout(new FlowLayout());
        JPanel panelA = new JPanel();
        JScrollPane panelB = new JScrollPane(map1);
        panelA.setSize(200, 500);
        panelA.setPreferredSize(new Dimension(300, 500));
        panelB.setSize(300, 500);
        panelB.setPreferredSize(new Dimension(300, 500));
        panelA.setBackground(Color.BLUE);
        panelB.setBackground(Color.DARK_GRAY);
        main1.add(panelA);
        panelA.add(stats);
        main1.add(panelB);
        main1.setAutoscrolls(true);
        panelB.setAutoscrolls(true);

        }


    @Override
    public void run() {
        this.setContentPane(this.main1);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setSize(640,560);
        this.validate();
        this.repaint();
        this.setVisible(true);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        map1 = new Map();
    }
}
