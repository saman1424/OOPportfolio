package project;

import uk.ac.leedsbeckett.oop.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class MyRailway extends OOPrailwaySim {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_X = 4;
    private static final int DEFAULT_Y = 3;
    private static final int MAX_SPEED = 40;
    private static final int DEFAULT_CROSSING_ID = 0;

    private final ArrayList<String> commandHistory = new ArrayList<>();
    private final ArrayList<Integer> locomotiveIds = new ArrayList<>();
    private final HashMap<Integer, Integer> locomotiveSpeeds = new HashMap<>();
    private final HashMap<Integer, Boolean> locomotiveDerailed = new HashMap<>();
    private final Set<Integer> derailmentReported = new HashSet<>();
    private final HashMap<Integer, Boolean> crossingStates = new HashMap<>();

    private JFrame controlFrame;
    private JPanel trainPanel;
    private JButton crossingBtn;

    private int lastLocoId = -1;

    public MyRailway() {
        super();
        setVisible(true);

        JOptionPane.showMessageDialog(this,
                "2D railway simulation - version 1.92\nObject Oriented Programming Component 2\nCreated by saman bastakoti",
                "About",
                JOptionPane.INFORMATION_MESSAGE);

        JOptionPane.showMessageDialog(this,
                "Type commands in the text box.\n\n" +
                "Core commands:\n" +
                "about\nstart\nstop\nreset\n" +
                "addloco [x] [y]\naddslowloco [x] [y]\n" +
                "attachcarriage [locoId]\ndetachcarriage [locoId]\n" +
                "speed <locoId> <0-40>\ncrossing <crossingId>\n\n" +
                "Extra commands:\n" +
                "loadcommands <filename>\nhistory\nsavehistory <filename>\nopenpanel",
                "Railway Simulator",
                JOptionPane.INFORMATION_MESSAGE);

        createControlPanel();
        System.out.println("Simulation started.");
    }

    // =========================
    // REQUIREMENT 5 - Inheritance
    // Override about() and add name
    // =========================
    @Override
    public void about() {
        JOptionPane.showMessageDialog(this,
                "2D railway simulation - version 1.92\nObject Oriented Programming Component 2\nCreated by saman bastakoti",
                "About",
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void processCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            showError("Empty command entered.");
            return;
        }

        command = command.trim();
        commandHistory.add(command);
        System.out.println("Command entered: " + command);

        String[] parts = command.split("\\s+");
        String cmd = parts[0].toLowerCase();

        try {
            switch (cmd) {
                case "about":
                    requireExactLength(parts, 1, "about does not take parameters.");
                    about();
                    break;

                case "start":
                    requireExactLength(parts, 1, "start does not take parameters.");
                    startSimulation();
                    break;

                case "stop":
                    requireExactLength(parts, 1, "stop does not take parameters.");
                    stopSimulation();
                    break;

                case "reset":
                    requireExactLength(parts, 1, "reset does not take parameters.");
                    handleReset();
                    break;

                case "addloco":
                    handleAddLoco(parts);
                    break;

                case "addslowloco":
                    handleAddSlowLoco(parts);
                    break;

                case "attachcarriage":
                    handleAttachCarriage(parts);
                    break;

                case "detachcarriage":
                    handleDetachCarriage(parts);
                    break;

                case "speed":
                    handleSpeed(parts);
                    break;

                case "crossing":
                    handleCrossing(parts);
                    break;

                case "loadcommands":
                    handleLoadCommands(parts);
                    break;

                case "history":
                    requireExactLength(parts, 1, "history does not take parameters.");
                    showHistory();
                    break;

                case "savehistory":
                    handleSaveHistory(parts);
                    break;

                case "openpanel":
                    requireExactLength(parts, 1, "openpanel does not take parameters.");
                    if (controlFrame != null) {
                        controlFrame.setVisible(true);
                        controlFrame.toFront();
                    }
                    refreshControlPanel();
                    break;

                default:
                    showError("Invalid command: " + cmd);
            }

        } catch (NumberFormatException e) {
            showError("Non-numeric data given where a number was expected.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    }

    @Override
    public void onTick() {
        super.onTick();
        monitorTrains();
    }

    private void monitorTrains() {
        ArrayList<Integer> ids = new ArrayList<>(locomotiveIds);

        for (int locoId : ids) {
            try {
                boolean derailed = isDerailed(locoId);
                locomotiveDerailed.put(locoId, derailed);

                if (derailed && !derailmentReported.contains(locoId)) {
                    derailmentReported.add(locoId);
                    locomotiveSpeeds.put(locoId, 0);

                    String message = "Locomotive " + locoId + " has derailed at its current track position.";
                    System.out.println(message);

                    JOptionPane.showMessageDialog(
                            this,
                            message,
                            "Train Derailed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            } catch (Exception e) {
                System.out.println("Error checking loco " + locoId + ": " + e.getMessage());
            }
        }

        refreshControlPanel();
    }

    @Override
    public boolean isDerailed(int locoId) {
        ensureValidTrackedLocomotive(locoId);

        try {
            return super.isDerailed(locoId - 1);
        } catch (Exception e) {
            System.out.println("Custom isDerailed failed for loco " + locoId + ": " + e.getMessage());
            return false;
        }
    }

    private void handleReset() {
        try {
            stopSimulation();
        } catch (Exception ignored) {
        }

        locomotiveIds.clear();
        locomotiveSpeeds.clear();
        locomotiveDerailed.clear();
        derailmentReported.clear();
        crossingStates.clear();
        lastLocoId = -1;

        try {
            dispose();
            if (controlFrame != null) {
                controlFrame.dispose();
            }
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            try {
                new MyRailway();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Reset failed while rebuilding the simulator window.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    private void handleAddLoco(String[] parts) {
        int[] coords = parseCoordinates(parts, "addloco");

        try {
            Locomotive loco = new Locomotive(getWorld(), coords[0], coords[1]);
            int id = addLocomotive(loco);

            lastLocoId = id;
            locomotiveIds.add(id);
            locomotiveSpeeds.put(id, 0);
            locomotiveDerailed.put(id, false);
            derailmentReported.remove(id);

            refreshControlPanel();
            System.out.println("Locomotive added with ID: " + id);
        } catch (Exception e) {
            showError("Invalid track coordinates given.");
        }
    }

    private void handleAddSlowLoco(String[] parts) {
        int[] coords = parseCoordinates(parts, "addslowloco");

        try {
            NewLoco slowLoco = new NewLoco(getWorld(), coords[0], coords[1]);
            int id = addLocomotive(slowLoco);

            lastLocoId = id;
            locomotiveIds.add(id);
            locomotiveSpeeds.put(id, 3);
            locomotiveDerailed.put(id, false);
            derailmentReported.remove(id);

            setLocomotiveSpeed(id, 3);

            refreshControlPanel();
            System.out.println("Slow locomotive added with ID: " + id);
        } catch (Exception e) {
            showError("Invalid track coordinates given.");
        }
    }

    private void handleAttachCarriage(String[] parts) {
        int locoId = resolveLocoId(parts, "attachcarriage");
        ensureValidTrackedLocomotive(locoId);
        ensureNotDerailed(locoId);

        try {
            addCarriageToLocomotive(locoId, new Carriage(getWorld()));
            refreshControlPanel();
            System.out.println("Carriage attached to locomotive " + locoId);
        } catch (Exception e) {
            showError("Invalid locomotive ID.");
        }
    }

    private void handleDetachCarriage(String[] parts) {
        int locoId = resolveLocoId(parts, "detachcarriage");
        ensureValidTrackedLocomotive(locoId);
        ensureNotDerailed(locoId);

        try {
            detachCarriageFromLocomotive(locoId);
            refreshControlPanel();
            System.out.println("Last carriage detached from locomotive " + locoId);
        } catch (Exception e) {
            showError("Invalid locomotive ID or no carriage attached.");
        }
    }

    private void handleSpeed(String[] parts) {
        if (parts.length != 3) {
            showError("Usage: speed <loco> <speed>");
            return;
        }

        int locoId = Integer.parseInt(parts[1]);
        int speed = Integer.parseInt(parts[2]);

        ensureValidTrackedLocomotive(locoId);

        if (speed < 0) {
            showError("Speed must not be negative.");
            return;
        }

        if (speed > MAX_SPEED) {
            showError("Maximum speed of 40 is reached.");
            return;
        }

        if (locomotiveDerailed.getOrDefault(locoId, false)) {
            showError("Locomotive " + locoId + " has already derailed. Reset the simulation first.");
            return;
        }

        try {
            setLocomotiveSpeed(locoId, speed);
            locomotiveSpeeds.put(locoId, speed);
            refreshControlPanel();
            System.out.println("Speed of locomotive " + locoId + " set to " + speed);
        } catch (Exception e) {
            showError("Invalid locomotive ID.");
        }
    }

    private void handleCrossing(String[] parts) {
        if (parts.length != 2) {
            showError("Usage: crossing <crossing>");
            return;
        }

        int crossingId = Integer.parseInt(parts[1]);

        if (crossingId < 0) {
            showError("Crossing number must not be negative.");
            return;
        }

        try {
            toggleCrossing(crossingId);

            boolean changed = crossingStates.getOrDefault(crossingId, false);
            crossingStates.put(crossingId, !changed);

            JOptionPane.showMessageDialog(
                    this,
                    "Crossing " + crossingId + " changed successfully.",
                    "Crossing Updated",
                    JOptionPane.INFORMATION_MESSAGE
            );

            System.out.println("Crossing " + crossingId + " changed.");
            refreshCrossingButtonText();
        } catch (Exception e) {
            showError("Invalid crossing number.");
        }
    }

    private void handleLoadCommands(String[] parts) {
        if (parts.length != 2) {
            showError("Usage: loadcommands <filename>");
            return;
        }

        loadCommandsFromFile(parts[1]);
    }

    private void loadCommandsFromFile(String filename) {
        File file = new File(filename);

        if (!file.exists()) {
            showError("File not found: " + filename);
            return;
        }

        try (Scanner fileScanner = new Scanner(file)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();

                if (!line.isEmpty()) {
                    System.out.println("Running command from file: " + line);
                    processCommand(line);
                }
            }

            JOptionPane.showMessageDialog(
                    this,
                    "Commands loaded successfully from file: " + filename,
                    "File Loaded",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (FileNotFoundException e) {
            showError("Could not open file: " + filename);
        } catch (Exception e) {
            showError("Error while reading commands from file.");
        }
    }

    private void showHistory() {
        if (commandHistory.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "No commands have been entered yet.",
                    "Command History",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        StringBuilder historyText = new StringBuilder();

        for (int i = 0; i < commandHistory.size(); i++) {
            historyText.append(i + 1).append(". ").append(commandHistory.get(i)).append("\n");
        }

        JTextArea textArea = new JTextArea(historyText.toString(), 15, 30);
        textArea.setEditable(false);
        textArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(textArea);

        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                "Command History",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void handleSaveHistory(String[] parts) {
        if (parts.length != 2) {
            showError("Usage: savehistory <filename>");
            return;
        }

        saveHistoryToFile(parts[1]);
    }

    private void saveHistoryToFile(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            for (String cmd : commandHistory) {
                writer.println(cmd);
            }

            JOptionPane.showMessageDialog(
                    this,
                    "Command history saved to file: " + filename,
                    "History Saved",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IOException e) {
            showError("Unable to save history to file.");
        }
    }

    private void createControlPanel() {
        controlFrame = new JFrame("Train Control Panel");
        controlFrame.setSize(760, 380);
        controlFrame.setLayout(new BorderLayout());
        controlFrame.setLocation(120, 120);
        controlFrame.setResizable(false);

        controlFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controlFrame.setVisible(false);
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));

        JButton addTrainBtn = new JButton("Add Train");
        JButton addSlowTrainBtn = new JButton("Add Slow Train");
        JButton startBtn = new JButton("Start");
        JButton stopBtn = new JButton("Stop");
        JButton resetBtn = new JButton("Reset");
        crossingBtn = new JButton();

        addTrainBtn.addActionListener(e -> processCommand("addloco"));
        addSlowTrainBtn.addActionListener(e -> processCommand("addslowloco"));
        startBtn.addActionListener(e -> processCommand("start"));
        stopBtn.addActionListener(e -> processCommand("stop"));
        resetBtn.addActionListener(e -> processCommand("reset"));
        crossingBtn.addActionListener(e -> processCommand("crossing " + DEFAULT_CROSSING_ID));

        topPanel.add(addTrainBtn);
        topPanel.add(addSlowTrainBtn);
        topPanel.add(startBtn);
        topPanel.add(stopBtn);
        topPanel.add(resetBtn);
        topPanel.add(crossingBtn);

        trainPanel = new JPanel();
        trainPanel.setLayout(new BoxLayout(trainPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(trainPanel);
        scrollPane.setPreferredSize(new Dimension(720, 280));

        controlFrame.add(topPanel, BorderLayout.NORTH);
        controlFrame.add(scrollPane, BorderLayout.CENTER);

        refreshCrossingButtonText();
        controlFrame.setVisible(true);
        refreshControlPanel();
    }

    private void refreshControlPanel() {
        if (trainPanel == null) {
            return;
        }

        trainPanel.removeAll();

        if (locomotiveIds.isEmpty()) {
            JPanel empty = new JPanel(new FlowLayout(FlowLayout.LEFT));
            empty.add(new JLabel("No active trains. Use Add Train or Add Slow Train to create one."));
            trainPanel.add(empty);
        }

        for (int locoId : new ArrayList<>(locomotiveIds)) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));

            int currentSpeed = locomotiveSpeeds.getOrDefault(locoId, 0);
            boolean derailed = locomotiveDerailed.getOrDefault(locoId, false);

            JLabel label = new JLabel(
                    "Train " + locoId + " | Speed: " + currentSpeed + (derailed ? " | DERAILED" : " | ACTIVE")
            );

            JButton plusBtn = new JButton("+");
            JButton minusBtn = new JButton("-");
            JButton attachBtn = new JButton("Attach");
            JButton detachBtn = new JButton("Detach");
            JButton deleteBtn = new JButton("Delete");

            plusBtn.addActionListener(e -> {
                int newSpeed = locomotiveSpeeds.getOrDefault(locoId, 0) + 1;
                if (newSpeed <= MAX_SPEED) {
                    processCommand("speed " + locoId + " " + newSpeed);
                }
            });

            minusBtn.addActionListener(e -> {
                int newSpeed = locomotiveSpeeds.getOrDefault(locoId, 0) - 1;
                if (newSpeed >= 0) {
                    processCommand("speed " + locoId + " " + newSpeed);
                }
            });

            attachBtn.addActionListener(e -> processCommand("attachcarriage " + locoId));
            detachBtn.addActionListener(e -> processCommand("detachcarriage " + locoId));

            deleteBtn.addActionListener(e -> {
                try {
                    if (locomotiveSpeeds.getOrDefault(locoId, 0) != 0) {
                        processCommand("speed " + locoId + " 0");
                    }

                    locomotiveIds.remove(Integer.valueOf(locoId));
                    locomotiveSpeeds.remove(locoId);
                    locomotiveDerailed.remove(locoId);
                    derailmentReported.remove(locoId);

                    if (lastLocoId == locoId) {
                        lastLocoId = locomotiveIds.isEmpty() ? -1 : locomotiveIds.get(locomotiveIds.size() - 1);
                    }

                    refreshControlPanel();
                } catch (Exception ex) {
                    showError("Unable to delete locomotive " + locoId);
                }
            });

            plusBtn.setEnabled(!derailed);
            minusBtn.setEnabled(!derailed);
            attachBtn.setEnabled(!derailed);
            detachBtn.setEnabled(!derailed);

            row.add(label);
            row.add(plusBtn);
            row.add(minusBtn);
            row.add(attachBtn);
            row.add(detachBtn);
            row.add(deleteBtn);

            trainPanel.add(row);
        }

        refreshCrossingButtonText();
        trainPanel.revalidate();
        trainPanel.repaint();
    }

    private void refreshCrossingButtonText() {
        if (crossingBtn != null) {
            boolean changed = crossingStates.getOrDefault(DEFAULT_CROSSING_ID, false);
            crossingBtn.setText("Crossing " + DEFAULT_CROSSING_ID + " : " + (changed ? "Changed" : "Default"));
        }
    }

    private int[] parseCoordinates(String[] parts, String commandName) {
        if (parts.length > 3) {
            throw new IllegalArgumentException("Too many parameters for " + commandName + ".");
        }

        int x = DEFAULT_X;
        int y = DEFAULT_Y;

        if (parts.length >= 2) {
            x = Integer.parseInt(parts[1]);
        }

        if (parts.length == 3) {
            y = Integer.parseInt(parts[2]);
        }

        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Coordinates must not be negative.");
        }

        return new int[]{x, y};
    }

    private int resolveLocoId(String[] parts, String commandName) {
        if (locomotiveIds.isEmpty()) {
            throw new IllegalArgumentException("No locomotives exist.");
        }

        if (parts.length > 2) {
            throw new IllegalArgumentException("Too many parameters for " + commandName + ".");
        }

        int locoId = (parts.length == 1) ? lastLocoId : Integer.parseInt(parts[1]);

        if (locoId <= 0) {
            throw new IllegalArgumentException("Locomotive ID must be positive.");
        }

        return locoId;
    }

    private void ensureValidTrackedLocomotive(int locoId) {
        if (locoId <= 0) {
            throw new IllegalArgumentException("Locomotive ID must be positive.");
        }

        if (!locomotiveIds.contains(locoId)) {
            throw new IllegalArgumentException("Invalid locomotive ID.");
        }
    }

    private void ensureNotDerailed(int locoId) {
        if (locomotiveDerailed.getOrDefault(locoId, false)) {
            throw new IllegalArgumentException(
                    "Locomotive " + locoId + " has derailed. Reset the simulation before editing this train."
            );
        }
    }

    private void requireExactLength(String[] parts, int expected, String message) {
        if (parts.length != expected) {
            throw new IllegalArgumentException(message);
        }
    }

    private void showError(String message) {
        System.out.println(message);
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}