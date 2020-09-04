package util;

import app.SharedObject;
import model.BlockType;
import processing.core.PApplet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class Swing {
    private static final JDialog[] dialogList = new JDialog[4];

    public static void createTopMenu(int width, int height, Frame frame, PApplet pApplet) {
        JWindow menuWindow = new JWindow();
        menuWindow.setSize(width, height);
        menuWindow.setLocation(0, 0);

        int buttonWidth = width/7;
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 0));

        //button
        JButton oButton = new JButton("Origin");
        ActionListener oButtonActionListen = new ActionListener() {//监听
            @Override
            public void actionPerformed(ActionEvent ae) {
                SharedObject.getInstance().updateRegionPreList(0);
            }
        };
        oButton.addActionListener(oButtonActionListen);
        oButton.setSize(buttonWidth,height);

        JButton dButton = new JButton("Destination");
        ActionListener dButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharedObject.getInstance().updateRegionPreList(1);
            }
        };
        dButton.addActionListener(dButtonActionListen);
        dButton.setSize(buttonWidth,height);

        JButton wButton = new JButton("WayPoint");
        ActionListener wButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharedObject.getInstance().updateRegionPreList(2);
            }
        };
        wButton.addActionListener(wButtonActionListen);
        wButton.setSize(buttonWidth,height);

        JButton wLayerButton = new JButton("NextWayPointLayer");
        ActionListener wLayerButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharedObject.getInstance().updateWLayer();
            }
        };
        wLayerButton.addActionListener(wLayerButtonActionListen);
        wLayerButton.setSize(buttonWidth,height);

        JButton finishSelectButton = new JButton("FinishSelect");
        ActionListener finishSelectButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharedObject.getInstance().setFinishSelectRegion(true);
            }
        };
        finishSelectButton.addActionListener(finishSelectButtonActionListen);
        finishSelectButton.setSize(buttonWidth,height);

        JButton screenShotButton = new JButton("ScreenShot");
        ActionListener screenShotButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharedObject.getInstance().setScreenShot(true);
            }
        };
        screenShotButton.addActionListener(screenShotButtonActionListen);
        screenShotButton.setSize(buttonWidth,height);

        JButton clearRegionButton = new JButton("ClearAllRegions");
        ActionListener clearRegionActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("clear all!");
                SharedObject.getInstance().cleanRegions();
            }
        };
        clearRegionButton.addActionListener(clearRegionActionListen);
        clearRegionButton.setSize(buttonWidth,height);

        JButton exitButton = new JButton("Exit");
        ActionListener exitButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pApplet.exit();
            }
        };
        exitButton.addActionListener(exitButtonActionListen);
        exitButton.setSize(buttonWidth,height);

        Container panel2 = menuWindow.getContentPane();
        panel2.add(new JLabel("[--]menu function"), BorderLayout.PAGE_START);
        panel2.add(panel,BorderLayout.CENTER);

        panel.add(oButton);
        panel.add(dButton);
        panel.add(wButton);
        panel.add(wLayerButton);
        panel.add(finishSelectButton);
        panel.add(screenShotButton);
        panel.add(clearRegionButton);
        panel.add(exitButton);

        menuWindow.setAlwaysOnTop(true);
        menuWindow.setVisible(true);
    }

    public static JDialog getSwingDialog(Frame frame, int mapIdx) {
        if (dialogList[mapIdx] != null) {
            return dialogList[mapIdx];
        }

        JDialog dialog = new JDialog(frame, "Select Data Set", false);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(frame);

        JPanel mainPanel = new JPanel(new BorderLayout());

        /* main part interface */

        GridBagLayout gbLayout = new GridBagLayout();
        JPanel centerPanel = new JPanel(gbLayout);

        JLabel infoLabel = new JLabel("Choose the base data set for map " + mapIdx);
        centerPanel.add(infoLabel, new GBC(0, 0, 2, 1).setAnchor(GBC.WEST).setInsets(10, 10, 15, 10));

        JLabel typeLabel = new JLabel("Data Type: ");
        JComboBox<BlockType> typeComboBox = new JComboBox<>(new BlockType[]{
                BlockType.FULL, BlockType.VFGS});
        centerPanel.add(typeLabel, new GBC(0, 1).setAnchor(GBC.EAST).setInsets(5, 10, 5, 5));
        centerPanel.add(typeComboBox, new GBC(1, 1).setAnchor(GBC.WEST).setInsets(5, 5, 5, 10));

        JLabel rateLabel = new JLabel("Sample Rate: ");
        JComboBox<Double> rateComboBox = new JComboBox<>(Arrays.stream(PSC.RATE_LIST)
                .boxed().toArray(Double[]::new));
        centerPanel.add(rateLabel, new GBC(0, 2).setAnchor(GBC.EAST).setInsets(5, 10, 5, 5));
        centerPanel.add(rateComboBox, new GBC(1, 2).setAnchor(GBC.WEST).setInsets(5, 5, 5, 10));

        JLabel deltaLabel = new JLabel("Delta: ");
        JComboBox<Integer> deltaComboBox = new JComboBox<>(Arrays.stream(PSC.DELTA_LIST)
                .boxed().toArray(Integer[]::new));
        centerPanel.add(deltaLabel, new GBC(0, 3).setAnchor(GBC.EAST).setInsets(5, 10, 5, 5));
        centerPanel.add(deltaComboBox, new GBC(1, 3).setAnchor(GBC.WEST).setInsets(5, 5, 5, 10));

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        /* bottom part interface */

        Box bottomBox = Box.createHorizontalBox();
        JButton cancelBtn = new JButton("Cancel");
        JButton okBtn = new JButton("OK");
        bottomBox.add(Box.createHorizontalGlue());
        bottomBox.add(cancelBtn);
        bottomBox.add(Box.createHorizontalStrut(20));
        bottomBox.add(okBtn);
        bottomBox.add(Box.createHorizontalGlue());
        bottomBox.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        mainPanel.add(bottomBox, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.setAlwaysOnTop(true);
        dialog.pack();

        /* logic */

        cancelBtn.addActionListener(e -> dialog.dispose());
        okBtn.addActionListener(e -> {

            dialog.dispose();
        });

        dialogList[mapIdx] = dialog;
        return dialog;
    }

//    private void refreshVisible(BlockType type, ) {
//
//    }
}
