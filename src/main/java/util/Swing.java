package util;

import app.SharedObject;
import model.Region;
import processing.core.PApplet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Swing {
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
        panel.add(finishSelectButton);
        panel.add(screenShotButton);
        panel.add(clearRegionButton);
        panel.add(exitButton);

        menuWindow.setAlwaysOnTop(true);
        menuWindow.setVisible(true);
    }
}