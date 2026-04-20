package legacy;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * DUMMY FILE — reconstructed for portfolio purposes.
 * No original proprietary code is included.
 *
 * This fragment demonstrates three structural problems found in the original
 * MainForm.java (~40,000 lines), condensed to a readable example.
 *
 * Problems illustrated:
 *   1. Manual button creation — repeated 900+ times
 *   2. Triple identical access-filter methods (~5,000 lines each)
 *   3. Action handlers hiding non-uniform logic
 */
public class MainForm_fragment extends JFrame {

    // -------------------------------------------------------------------------
    // PROBLEM 1: Manual button declarations — one field per button, 900+ total
    // -------------------------------------------------------------------------

    private BigButton documentCategoryButton;
    private BigButton documentTypeButton;
    private BigButton documentCollectionButton;
    private BigButton memberRegistryButton;
    private BigButton circulationButton;

    private BigButton insuranceClaimManualButton;
    private BigButton insuranceCoderIdButton;

    private BigButton recordTransferButton;
    private BigButton userSettingsButton;
    private BigButton appSettingsButton;

    // Every button required its own field. The NetBeans GUI Builder generated
    // all of these at once — touching one risked breaking the generated block.


    // -------------------------------------------------------------------------
    // PROBLEM 1 (continued): Button initialization — copy-paste per button
    // -------------------------------------------------------------------------

    private void initButtons() {

        // --- Document module buttons ---

        documentCategoryButton = new BigButton();
        documentCategoryButton.setIcon(new ImageIcon(getClass().getResource("/icons/document-open.png")));
        documentCategoryButton.setText("Document Category");
        documentCategoryButton.setIconTextGap(0);
        documentCategoryButton.setName("documentCategoryButton");
        documentCategoryButton.setPreferredSize(new Dimension(200, 90));
        documentCategoryButton.addActionListener(this::onDocumentCategoryClicked);

        documentTypeButton = new BigButton();
        documentTypeButton.setIcon(new ImageIcon(getClass().getResource("/icons/document-type.png")));
        documentTypeButton.setText("Document Type");
        documentTypeButton.setIconTextGap(0);
        documentTypeButton.setName("documentTypeButton");
        documentTypeButton.setPreferredSize(new Dimension(200, 90));
        documentTypeButton.addActionListener(this::onDocumentTypeClicked);

        documentCollectionButton = new BigButton();
        documentCollectionButton.setIcon(new ImageIcon(getClass().getResource("/icons/collection.png")));
        documentCollectionButton.setText("Document Collection");
        documentCollectionButton.setIconTextGap(0);
        documentCollectionButton.setName("documentCollectionButton");
        documentCollectionButton.setPreferredSize(new Dimension(200, 90));
        documentCollectionButton.addActionListener(this::onDocumentCollectionClicked);

        memberRegistryButton = new BigButton();
        memberRegistryButton.setIcon(new ImageIcon(getClass().getResource("/icons/user.png")));
        memberRegistryButton.setText("Member Registry");
        memberRegistryButton.setIconTextGap(0);
        memberRegistryButton.setName("memberRegistryButton");
        memberRegistryButton.setPreferredSize(new Dimension(200, 90));
        memberRegistryButton.addActionListener(this::onMemberRegistryClicked);

        // ... this pattern repeats ~900 times in the original file

        // --- Integration / special-case buttons ---

        insuranceClaimManualButton = new BigButton();
        insuranceClaimManualButton.setIcon(new ImageIcon(getClass().getResource("/icons/claim.png")));
        insuranceClaimManualButton.setText("Insurance Claim — Manual Entry");
        insuranceClaimManualButton.setIconTextGap(0);
        insuranceClaimManualButton.setName("insuranceClaimManualButton");
        insuranceClaimManualButton.setPreferredSize(new Dimension(200, 90));
        insuranceClaimManualButton.addActionListener(this::onInsuranceClaimManualClicked);

        insuranceCoderIdButton = new BigButton();
        insuranceCoderIdButton.setIcon(new ImageIcon(getClass().getResource("/icons/id-card.png")));
        insuranceCoderIdButton.setText("Insurance Coder ID");
        insuranceCoderIdButton.setIconTextGap(0);
        insuranceCoderIdButton.setName("insuranceCoderIdButton");
        insuranceCoderIdButton.setPreferredSize(new Dimension(200, 90));
        insuranceCoderIdButton.addActionListener(this::onInsuranceCoderIdClicked);
    }


    // -------------------------------------------------------------------------
    // PROBLEM 2: Triple access filter — three methods, structurally identical
    //
    // The original had three UI states for menu filtering:
    //   filterEmpty()      — search field is empty, show all permitted buttons
    //   filterBySearch()   — search field has text, filter by label match
    //   filterByCategory() — category combo is selected, filter by category
    //
    // Each method iterated every button with the same pattern.
    // Total: ~15,000 lines across three methods for the same logical operation.
    // -------------------------------------------------------------------------

    private JPanel menuPanel;
    private JPanel documentModulePanel; // one panel per category, also hardcoded
    private JTextField searchField;
    private int visibleMenuCount;

    /** Called when search field is empty — show all accessible buttons */
    private void filterEmpty() {
        visibleMenuCount = 0;

        if (access.canViewDocumentCollection()) {
            menuPanel.add(documentCollectionButton);
            visibleMenuCount++;
        }
        if (access.canViewDocumentCategory()) {
            menuPanel.add(documentCategoryButton);
            visibleMenuCount++;
        }
        if (access.canViewDocumentType()) {
            menuPanel.add(documentTypeButton);
            visibleMenuCount++;
        }
        if (access.canViewMemberRegistry()) {
            menuPanel.add(memberRegistryButton);
            visibleMenuCount++;
        }
        if (access.canProcessInsuranceClaimManual()) {
            menuPanel.add(insuranceClaimManualButton);
            visibleMenuCount++;
        }

        // ... repeated for all 900 buttons
    }

    /** Called when search field has text — filter by label */
    private void filterBySearch() {
        visibleMenuCount = 0;

        // Identical structure to filterEmpty, plus a label-match check.
        // Every button check is duplicated from the method above.

        if (access.canViewDocumentCollection()) {
            if (documentCollectionButton.getText().toLowerCase().trim()
                    .contains(searchField.getText().toLowerCase().trim())) {
                menuPanel.add(documentCollectionButton);
                visibleMenuCount++;
            }
        }
        if (access.canViewDocumentCategory()) {
            if (documentCategoryButton.getText().toLowerCase().trim()
                    .contains(searchField.getText().toLowerCase().trim())) {
                menuPanel.add(documentCategoryButton);
                visibleMenuCount++;
            }
        }
        if (access.canViewDocumentType()) {
            if (documentTypeButton.getText().toLowerCase().trim()
                    .contains(searchField.getText().toLowerCase().trim())) {
                menuPanel.add(documentTypeButton);
                visibleMenuCount++;
            }
        }
        if (access.canProcessInsuranceClaimManual()) {
            if (insuranceClaimManualButton.getText().toLowerCase().trim()
                    .contains(searchField.getText().toLowerCase().trim())) {
                menuPanel.add(insuranceClaimManualButton);
                visibleMenuCount++;
            }
        }

        // ... repeated for all 900 buttons
    }

    /** Called when combo category is selected — filter by category panel */
    private void filterByCategory() {
        visibleMenuCount = 0;

        // Identical structure again. Same buttons, same access checks,
        // different panel target based on combo selection.

        if (access.canViewDocumentCollection()) {
            if (documentCollectionButton.getText().toLowerCase().trim()
                    .contains(searchField.getText().toLowerCase().trim())) {
                documentModulePanel.add(documentCollectionButton);
                visibleMenuCount++;
            }
        }
        if (access.canViewDocumentCategory()) {
            if (documentCategoryButton.getText().toLowerCase().trim()
                    .contains(searchField.getText().toLowerCase().trim())) {
                documentModulePanel.add(documentCategoryButton);
                visibleMenuCount++;
            }
        }

        // ... repeated for all 900 buttons, spread across category panels
    }


    // -------------------------------------------------------------------------
    // PROBLEM 3: Action handlers — look uniform, hide different logic
    //
    // Most handlers share identical boilerplate. But some embed role checks,
    // URL construction, or fallback dialogs — invisible from the outside.
    // -------------------------------------------------------------------------

    /** Standard handler — this pattern repeats ~850 times */
    private void onDocumentCategoryClicked(ActionEvent evt) {
        closeOpenDialogs();
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        DocumentCategoryForm form = new DocumentCategoryForm(this, false);
        form.validateAccess();
        form.clearFields();
        form.setSize(mainPanel.getWidth(), mainPanel.getHeight());
        form.setLocationRelativeTo(mainPanel);
        form.setVisible(true);
        homeDialog.dispose();
        this.setCursor(Cursor.getDefaultCursor());
    }

    private void onDocumentTypeClicked(ActionEvent evt) {
        closeOpenDialogs();
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        DocumentTypeForm form = new DocumentTypeForm(this, false);
        form.validateAccess();
        form.clearFields();
        form.setSize(mainPanel.getWidth(), mainPanel.getHeight());
        form.setLocationRelativeTo(mainPanel);
        form.setVisible(true);
        homeDialog.dispose();
        this.setCursor(Cursor.getDefaultCursor());
    }

    private void onDocumentCollectionClicked(ActionEvent evt) {
        closeOpenDialogs();
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        DocumentCollectionForm form = new DocumentCollectionForm(this, false);
        form.validateAccess();
        form.clearFields();
        form.setSize(mainPanel.getWidth(), mainPanel.getHeight());
        form.setLocationRelativeTo(mainPanel);
        form.setVisible(true);
        homeDialog.dispose();
        this.setCursor(Cursor.getDefaultCursor());
    }

    private void onMemberRegistryClicked(ActionEvent evt) {
        closeOpenDialogs();
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        MemberRegistryForm form = new MemberRegistryForm(this, false);
        form.validateAccess();
        form.clearFields();
        form.setSize(mainPanel.getWidth(), mainPanel.getHeight());
        form.setLocationRelativeTo(mainPanel);
        form.setVisible(true);
        homeDialog.dispose();
        this.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Non-standard handler — looks the same from the outside,
     * but contains role-based branching, URL construction, and a fallback dialog.
     * Cannot be migrated to the standard pattern without losing this logic.
     */
    private void onInsuranceClaimManualClicked(ActionEvent evt) {
        if (access.getRoleCode().equals("SuperAdmin")) {
            // Admin path: open ID search dialog first
            currentPage = "ManualClaimEntry";
            formTitle = "::[ Manual Claim Entry from Insurance Data ]::";
            closeOpenDialogs();
            homeDialog.dispose();
            idSearchDialog.setSize(mainPanel.getWidth(), mainPanel.getHeight());
            idSearchDialog.setLocationRelativeTo(mainPanel);
            idSearchDialog.setVisible(true);
        } else {
            // Non-admin path: look up coder ID from DB first
            String coderId = Database.queryScalar(
                "SELECT coder_id FROM insurance_coder_registry WHERE user_id = ?",
                access.getRoleCode()
            );
            if (!coderId.equals("")) {
                closeOpenDialogs();
                this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    externalClaimDialog.loadURL(
                        "http://" + Config.getWebHost() + ":" +
                        config.getProperty("WEB_PORT") + "/" +
                        config.getProperty("WEB_CONTEXT") +
                        "/claims/login.php?act=login" +
                        "&user=" + Config.getWebUser() +
                        "&password=" + Config.getWebPassword() +
                        "&page=ManualClaimEntry&coderId=" + coderId
                    );
                } catch (Exception ex) {
                    System.out.println("Notification: " + ex);
                }
                externalClaimDialog.setSize(mainPanel.getWidth(), mainPanel.getHeight());
                externalClaimDialog.setLocationRelativeTo(mainPanel);
                externalClaimDialog.setVisible(true);
                homeDialog.dispose();
                this.setCursor(Cursor.getDefaultCursor());
            } else {
                // Fallback: coder not registered
                closeOpenDialogs();
                homeDialog.dispose();
                JOptionPane.showMessageDialog(null,
                    "Coder ID not found — please contact the system administrator.");
            }
        }
    }

    private void onInsuranceCoderIdClicked(ActionEvent evt) {
        closeOpenDialogs();
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        InsuranceCoderIdForm form = new InsuranceCoderIdForm(this, false);
        form.clearFields();
        form.validateAccess();
        form.setSize(mainPanel.getWidth(), mainPanel.getHeight());
        form.setLocationRelativeTo(mainPanel);
        form.setVisible(true);
        homeDialog.dispose();
        this.setCursor(Cursor.getDefaultCursor());
    }


    // -------------------------------------------------------------------------
    // Supporting stubs — referenced above, not the focus here
    // -------------------------------------------------------------------------

    private Component mainPanel;
    private JDialog homeDialog;
    private Object idSearchDialog;
    private Object externalClaimDialog;
    private String currentPage;
    private String formTitle;
    private java.util.Properties config = new java.util.Properties();

    private void closeOpenDialogs() { /* close other open dialogs */ }

    // Placeholder for custom widget — represents the original project's BigButton
    private static class BigButton extends JButton {}

    // Placeholder references — represent real dependencies in the original system
    private static class access {
        static boolean canViewDocumentCollection() { return true; }
        static boolean canViewDocumentCategory() { return true; }
        static boolean canViewDocumentType() { return true; }
        static boolean canViewMemberRegistry() { return true; }
        static boolean canProcessInsuranceClaimManual() { return true; }
        static String getRoleCode() { return ""; }
    }

    private static class Config {
        static String getWebHost() { return "localhost"; }
        static String getWebUser() { return "user"; }
        static String getWebPassword() { return "password"; }
    }

    private static class Database {
        static String queryScalar(String sql, String param) { return ""; }
    }

    // Placeholder form classes
    private static class DocumentCategoryForm extends JDialog {
        DocumentCategoryForm(Frame f, boolean modal) { super(f, modal); }
        void validateAccess() {}
        void clearFields() {}
    }
    private static class DocumentTypeForm extends JDialog {
        DocumentTypeForm(Frame f, boolean modal) { super(f, modal); }
        void validateAccess() {}
        void clearFields() {}
    }
    private static class DocumentCollectionForm extends JDialog {
        DocumentCollectionForm(Frame f, boolean modal) { super(f, modal); }
        void validateAccess() {}
        void clearFields() {}
    }
    private static class MemberRegistryForm extends JDialog {
        MemberRegistryForm(Frame f, boolean modal) { super(f, modal); }
        void validateAccess() {}
        void clearFields() {}
    }
    private static class InsuranceCoderIdForm extends JDialog {
        InsuranceCoderIdForm(Frame f, boolean modal) { super(f, modal); }
        void validateAccess() {}
        void clearFields() {}
    }
}
