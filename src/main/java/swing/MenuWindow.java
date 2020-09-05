package swing;

import app.SharedObject;
import draw.TrajDrawManager;
import processing.core.PApplet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MenuWindow extends JWindow {

    public MenuWindow(int width, int height, PApplet pApplet) {
        setSize(width, height);
        setLocation(0, 0);

        int buttonWidth = width / 7;
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
        oButton.setSize(buttonWidth, height);


        JButton dButton = new JButton("Destination");
        ActionListener dButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharedObject.getInstance().updateRegionPreList(1);
            }
        };
        dButton.addActionListener(dButtonActionListen);
        dButton.setSize(buttonWidth, height);

        JButton wButton = new JButton("WayPoint");
        ActionListener wButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharedObject.getInstance().updateRegionPreList(2);
            }
        };
        wButton.addActionListener(wButtonActionListen);
        wButton.setSize(buttonWidth, height);

        JButton wLayerButton = new JButton("NextWayPointLayer");
        ActionListener wLayerButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharedObject.getInstance().updateWLayer();
            }
        };
        wLayerButton.addActionListener(wLayerButtonActionListen);
        wLayerButton.setSize(buttonWidth, height);

        JButton dragButton = new JButton("DragRegionOff");
        ActionListener dragButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharedObject.getInstance().setDragRegion();
                if (SharedObject.getInstance().isDragRegion()) {
                    dragButton.setText("DragRegionOn");
                    dragButton.setBackground(Color.DARK_GRAY);

                } else {
                    dragButton.setText("DragRegionOff");
                    dragButton.setBackground(Color.GRAY);

                }
            }
        };
        dragButton.addActionListener(dragButtonActionListen);
        dragButton.setSize(buttonWidth, height);

        JButton finishSelectButton = new JButton("FinishSelect");
        ActionListener finishSelectButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharedObject.getInstance().calTrajSelectResList();
            }
        };
        finishSelectButton.addActionListener(finishSelectButtonActionListen);
        finishSelectButton.setSize(buttonWidth, height);

        JButton screenShotButton = new JButton("ScreenShot");
        ActionListener screenShotButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharedObject.getInstance().setScreenShot(true);
            }
        };
        screenShotButton.addActionListener(screenShotButtonActionListen);
        screenShotButton.setSize(buttonWidth, height);

        JButton clearRegionButton = new JButton("ClearAllRegions");
        ActionListener clearRegionActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("clear all!");
                TrajDrawManager tdm = SharedObject.getInstance().getTrajDrawManager();
                tdm.cleanAllImg(TrajDrawManager.SLT);
                SharedObject.getInstance().cleanRegions();
            }
        };
        clearRegionButton.addActionListener(clearRegionActionListen);
        clearRegionButton.setSize(buttonWidth, height);

        JButton exitButton = new JButton("Exit");
        ActionListener exitButtonActionListen = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pApplet.exit();
            }
        };

        oButton.setBackground(Color.GRAY);
        dButton.setBackground(Color.GRAY);
        wButton.setBackground(Color.GRAY);
        wLayerButton.setBackground(Color.GRAY);
        dragButton.setBackground(Color.GRAY);
        finishSelectButton.setBackground(Color.GRAY);
        screenShotButton.setBackground(Color.GRAY);
        clearRegionButton.setBackground(Color.GRAY);
        exitButton.setBackground(Color.GRAY);

        exitButton.addActionListener(exitButtonActionListen);
        exitButton.setSize(buttonWidth, height);

        Container panel2 = getContentPane();
        panel2.add(new JLabel("[--]menu function"), BorderLayout.PAGE_START);
        panel2.add(panel, BorderLayout.CENTER);

        panel.add(oButton);
        panel.add(dButton);
        panel.add(wButton);
        panel.add(wLayerButton);
        panel.add(dragButton);
        panel.add(finishSelectButton);
        panel.add(screenShotButton);
        panel.add(clearRegionButton);
        panel.add(exitButton);

        setAlwaysOnTop(true);
    }
}
