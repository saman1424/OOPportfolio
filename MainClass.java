package project;

import javax.swing.SwingUtilities;

public class MainClass {
    public static void main(String[] args) {
        // Launch Swing components safely on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            new MyRailway();
        });
    }
}