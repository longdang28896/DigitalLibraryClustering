package com.dmml.library;

import com.dmml.library.ui.MainLibraryFrame;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;

public class DigitalLibraryApplication {

    public static void main(String[] args) {
        // Ensure standard console output uses UTF-8 to prevent mojibake on Windows CMD
        try {
            System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
            System.setErr(new java.io.PrintStream(System.err, true, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }

        // Configure Look and Feel (FlatLaf modern UI, fallback to Windows system)
        try {
            // Modern FlatLaf theme configurations
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("ScrollBar.width", 10);
            FlatLightLaf.setup();
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // Ignore fallback error
            }
        }

        System.out.println("================================================================");
        System.out.println("  HỆ THỐNG QUẢN LÝ THƯ VIỆN SỐ TÍCH HỢP PHÂN CỤM TÀI LIỆU (JAVA)");
        System.out.println("  BÀI TẬP LỚN: NHẬP MÔN KHAI PHÁ DỮ LIỆU VÀ MÁY HỌC - ĐỀ 2508");
        System.out.println("  PHIÊN BẢN: DESKTOP APPLICATION (JAVA SWING)");
        System.out.println("================================================================");

        SwingUtilities.invokeLater(() -> {
            MainLibraryFrame frame = new MainLibraryFrame();
            frame.setVisible(true);
            System.out.println(">>> Cửa sổ ứng dụng Desktop đã hiển thị thành công! <<<");
        });
    }
}
