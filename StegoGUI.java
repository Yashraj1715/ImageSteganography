import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class StegoGUI extends JFrame {

    // ---- Colors and fonts used throughout the GUI ----
    private static final Color BG_COLOR = new Color(245, 247, 250);
    private static final Color ACCENT_COLOR = new Color(41, 98, 255);
    private static final Color ACCENT_HOVER = new Color(30, 78, 220);
    private static final Color TEXT_COLOR = new Color(33, 37, 41);
    private static final Font HEADING_FONT = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 13);

    // ---- Encode tab components ----
    private JLabel encodeImagePathLabel;
    private JTextArea encodeMessageArea;
    private JPasswordField encodePasswordField;
    private JLabel encodeCapacityLabel;
    private BufferedImage encodeSelectedImage; // holds the actual loaded cover image

    // ---- Decode tab components ----
    private JLabel decodeImagePathLabel;
    private JPasswordField decodePasswordField;
    private JTextArea decodeResultArea;
    private BufferedImage decodeSelectedImage;

    public StegoGUI() {
        // Basic window setup
        setTitle("Image Steganography Tool - AES Encrypted LSB");
        setSize(620, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // centers window on screen

        // Tabs container
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(HEADING_FONT);
        tabbedPane.setBackground(BG_COLOR);
        tabbedPane.addTab("  Encode  ", buildEncodePanel());
        tabbedPane.addTab("  Decode  ", buildDecodePanel());

        add(tabbedPane);
    }

    // Creates a consistently-styled button: accent color, white bold text, hand cursor
    private JButton styledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(BUTTON_FONT);
        btn.setBackground(ACCENT_COLOR);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        btn.setOpaque(true);
        btn.setBorderPainted(false);

        // Slight hover-darken effect
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(ACCENT_HOVER); }
            public void mouseExited(java.awt.event.MouseEvent e) { btn.setBackground(ACCENT_COLOR); }
        });
        return btn;
    }

    // Creates a section heading label (bold, dark, slightly larger)
    private JLabel headingLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(HEADING_FONT);
        label.setForeground(TEXT_COLOR);
        return label;
    }

    // ============================================================
    // ENCODE TAB
    // ============================================================
    private JPanel buildEncodePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(BG_COLOR);

        // --- Choose image button ---
        JButton chooseImageBtn = styledButton("Choose Cover Image (PNG)");
        encodeImagePathLabel = new JLabel("No image selected");
        encodeImagePathLabel.setFont(LABEL_FONT);
        encodeCapacityLabel = new JLabel(" ");
        encodeCapacityLabel.setFont(LABEL_FONT);
        encodeCapacityLabel.setForeground(new Color(90, 90, 90));

        chooseImageBtn.addActionListener(this::onChooseEncodeImage);

        // --- Message input ---
        JLabel messageLabel = headingLabel("Secret Message");
        encodeMessageArea = new JTextArea(6, 30);
        encodeMessageArea.setLineWrap(true);
        encodeMessageArea.setWrapStyleWord(true);
        encodeMessageArea.setFont(LABEL_FONT);
        encodeMessageArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JScrollPane messageScroll = new JScrollPane(encodeMessageArea);
        messageScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        // --- Password input ---
        JLabel passwordLabel = headingLabel("Password");
        encodePasswordField = new JPasswordField(20);
        encodePasswordField.setFont(LABEL_FONT);
        encodePasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        encodePasswordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        // --- Encode button ---
        JButton encodeBtn = styledButton("Encode & Save Stego Image");
        encodeBtn.addActionListener(this::onEncodeClicked);

        // Assemble everything vertically, left-aligned
        panel.add(alignLeft(chooseImageBtn));
        panel.add(alignLeft(encodeImagePathLabel));
        panel.add(alignLeft(encodeCapacityLabel));
        panel.add(Box.createVerticalStrut(10));
        panel.add(alignLeft(messageLabel));
        panel.add(messageScroll);
        panel.add(Box.createVerticalStrut(10));
        panel.add(alignLeft(passwordLabel));
        panel.add(alignLeft(encodePasswordField));
        panel.add(Box.createVerticalStrut(15));
        panel.add(alignLeft(encodeBtn));

        return panel;
    }

    // Called when "Choose Cover Image" is clicked
    private void onChooseEncodeImage(ActionEvent e) {
        File selected = chooseImageFile();
        if (selected == null) return; // user cancelled

        try {
            encodeSelectedImage = ImageIO.read(selected);
            encodeImagePathLabel.setText("Selected: " + selected.getName());

            // Capacity calculation: 3 bits per pixel, minus 32-bit length header, / 8 for bytes
            long capacityBits = (long) encodeSelectedImage.getWidth() * encodeSelectedImage.getHeight() * 3;
            long capacityChars = (capacityBits - 32) / 8;
            encodeCapacityLabel.setText("Max message capacity: ~" + capacityChars + " characters");

        } catch (Exception ex) {
            showError("Could not read image: " + ex.getMessage());
        }
    }

    // Called when "Encode & Save" is clicked
    private void onEncodeClicked(ActionEvent e) {
        if (encodeSelectedImage == null) {
            showError("Please choose a cover image first.");
            return;
        }
        String message = encodeMessageArea.getText().trim();
        if (message.isEmpty()) {
            showError("Please enter a message to hide.");
            return;
        }
        String password = new String(encodePasswordField.getPassword());
        if (password.isEmpty()) {
            showError("Please enter a password.");
            return;
        }

        try {
            BufferedImage stegoImage = Encoder.encodeWithEncryption(encodeSelectedImage, message, password);

            // Ask user where to save the resulting stego image
            JFileChooser saveChooser = new JFileChooser();
            saveChooser.setDialogTitle("Save Stego Image As");
            saveChooser.setSelectedFile(new File("stego.png"));
            int result = saveChooser.showSaveDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File outFile = saveChooser.getSelectedFile();
                // Make sure it ends with .png
                if (!outFile.getName().toLowerCase().endsWith(".png")) {
                    outFile = new File(outFile.getAbsolutePath() + ".png");
                }
                ImageIO.write(stegoImage, "png", outFile);
                JOptionPane.showMessageDialog(this,
                        "Success! Stego image saved to:\n" + outFile.getAbsolutePath(),
                        "Encoding Complete", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (IllegalArgumentException ex) {
            // This is thrown by checkCapacity() when message is too big for the image
            showError("Message too large for this image:\n" + ex.getMessage());
        } catch (Exception ex) {
            showError("Encoding failed: " + ex.getMessage());
        }
    }

    // ============================================================
    // DECODE TAB
    // ============================================================
    private JPanel buildDecodePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(BG_COLOR);

        JButton chooseImageBtn = styledButton("Choose Stego Image (PNG)");
        decodeImagePathLabel = new JLabel("No image selected");
        decodeImagePathLabel.setFont(LABEL_FONT);
        chooseImageBtn.addActionListener(this::onChooseDecodeImage);

        JLabel passwordLabel = headingLabel("Password");
        decodePasswordField = new JPasswordField(20);
        decodePasswordField.setFont(LABEL_FONT);
        decodePasswordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        decodePasswordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        JButton decodeBtn = styledButton("Decode Message");
        decodeBtn.addActionListener(this::onDecodeClicked);

        JLabel resultLabel = headingLabel("Hidden Message");
        decodeResultArea = new JTextArea(6, 30);
        decodeResultArea.setLineWrap(true);
        decodeResultArea.setWrapStyleWord(true);
        decodeResultArea.setEditable(false);
        decodeResultArea.setFont(LABEL_FONT);
        decodeResultArea.setBackground(Color.WHITE);
        decodeResultArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JScrollPane resultScroll = new JScrollPane(decodeResultArea);
        resultScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        panel.add(alignLeft(chooseImageBtn));
        panel.add(alignLeft(decodeImagePathLabel));
        panel.add(Box.createVerticalStrut(10));
        panel.add(alignLeft(passwordLabel));
        panel.add(alignLeft(decodePasswordField));
        panel.add(Box.createVerticalStrut(15));
        panel.add(alignLeft(decodeBtn));
        panel.add(Box.createVerticalStrut(15));
        panel.add(alignLeft(resultLabel));
        panel.add(resultScroll);

        return panel;
    }

    private void onChooseDecodeImage(ActionEvent e) {
        File selected = chooseImageFile();
        if (selected == null) return;

        try {
            decodeSelectedImage = ImageIO.read(selected);
            decodeImagePathLabel.setText("Selected: " + selected.getName());
        } catch (Exception ex) {
            showError("Could not read image: " + ex.getMessage());
        }
    }

    private void onDecodeClicked(ActionEvent e) {
        if (decodeSelectedImage == null) {
            showError("Please choose a stego image first.");
            return;
        }
        String password = new String(decodePasswordField.getPassword());
        if (password.isEmpty()) {
            showError("Please enter the password.");
            return;
        }

        try {
            String message = Decoder.decodeWithDecryption(decodeSelectedImage, password);
            decodeResultArea.setText(message);
        } catch (Exception ex) {
            // Wrong password or corrupted/non-stego image lands here
            decodeResultArea.setText("");
            showError("Decoding failed. Wrong password or this image has no hidden message.");
        }
    }

    // ============================================================
    // Shared helper methods
    // ============================================================
    private File chooseImageFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null; // user clicked Cancel
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Utility to left-align components inside a vertical BoxLayout
    private Component alignLeft(JComponent comp) {
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        return comp;
    }

    // ============================================================
    // Entry point
    // ============================================================
    public static void main(String[] args) {
        // Apply Nimbus Look and Feel for a more modern appearance than default Swing.
        // If Nimbus isn't available for some reason, we just fall back to the default
        // look and feel instead of crashing the whole app.
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Nimbus not available, using default look and feel.");
        }

        // SwingUtilities.invokeLater ensures GUI is built on the correct thread
        SwingUtilities.invokeLater(() -> {
            StegoGUI gui = new StegoGUI();
            gui.setVisible(true);
        });
    }
}
