package refactored.menu;

import refactored.menu.layout.MenuLayoutManager;
import refactored.menu.model.MenuItem;
import refactored.ui.ColorConfig;
import refactored.ui.GradientButton;
import refactored.core.access;
import refactored.core.Database;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MenuShell — the rendering layer of the menu system.
 *
 * This class is responsible only for displaying menu items and handling
 * navigation. It does not know what each menu item does — that logic lives
 * in MenuItem.java as declarative data.
 */
public class MenuShell extends JDialog {

    // --- Constants ---
    private static final Color BACKGROUND_COLOR = ColorConfig.getPrimaryColor();
    private static final Color FONT_COLOR = ColorConfig.getPrimaryFontColor();
    private static final Dimension BUTTON_SIZE = new Dimension(25, 23);
    private static final Dimension SEARCH_SIZE = new Dimension(300, 23);
    private static final String CATEGORY_ICON = "/icons/folder-document.png";

    // --- Services ---
    private final MenuItem.Service menuService = new MenuItem.Service();

    // --- UI Components ---
    private final JPanel menuPanel = new JPanel();
    private final JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 7));
    private final JScrollPane menuScrollPane = new JScrollPane();
    private final JButton closeButton = createCloseButton();
    private final JButton backButton = createBackButton();
    private final JLabel titleLabel = createTitleLabel();
    private final JTextField searchField = createSearchField();

    // --- State ---
    private boolean showingCategories = true;
    private MenuItem.Category currentCategory;
    private JLabel loadingLabel;

    public MenuShell(JFrame parent) {
        super(parent, "Main Menu", true);
        initializeDialog();
        setupWindowListener();
    }

    private void initializeDialog() {
        setupDialogProperties();
        setupScrollPane();
        setupMainPanel();
        setupHeaderPanel();
        setupEventHandlers();
    }

    private void setupDialogProperties() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setAlwaysOnTop(false);
        setUndecorated(true);
        setResizable(false);
        setSize(800, 600);
    }

    private void setupScrollPane() {
        menuScrollPane.setBackground(BACKGROUND_COLOR);
        menuScrollPane.setBorder(BorderFactory.createLineBorder(FONT_COLOR));
        menuScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    private void setupMainPanel() {
        menuPanel.setBackground(BACKGROUND_COLOR);
        menuPanel.setBorder(createTitledBorder());

        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setBorder(BorderFactory.createLineBorder(ColorConfig.getSecondaryColor()));
        headerPanel.setPreferredSize(new Dimension(100, 40));

        add(menuScrollPane, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);
        menuScrollPane.setViewportView(menuPanel);
    }

    private Border createTitledBorder() {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BACKGROUND_COLOR),
                "::[ Main Menu ]::",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font("Segoe UI", Font.PLAIN, 11),
                FONT_COLOR
        );
    }

    private void setupHeaderPanel() {
        headerPanel.add(closeButton);
        headerPanel.add(searchField);
        headerPanel.add(backButton);
        headerPanel.add(titleLabel);
    }

    // --- Component Creation ---
    private JButton createCloseButton() {
        JButton button = new JButton(new ImageIcon(getClass().getResource("/icons/exit.png")));
        button.setPreferredSize(BUTTON_SIZE);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        return button;
    }

    private JButton createBackButton() {
        JButton button = new JButton("← Back");
        button.setVisible(false);
        return button;
    }

    private JLabel createTitleLabel() {
        JLabel label = new JLabel("Menu:");
        label.setPreferredSize(new Dimension(505, 23));
        return label;
    }

    private JTextField createSearchField() {
        JTextField field = new JTextField();
        field.setPreferredSize(SEARCH_SIZE);
        return field;
    }

    private void setupEventHandlers() {
        closeButton.addActionListener(evt -> dispose());
        searchField.addKeyListener(new SearchKeyAdapter());
        backButton.addActionListener(e -> navigateBack());
    }

    private void setupWindowListener() {
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowActivated(java.awt.event.WindowEvent e) {
                reloadItems();
            }
        });
    }

    // --- Navigation ---
    public void setMenuPosition(Point position) {
        setLocation(position);
    }

    public void showRelativeTo(Component component) {
        if (component != null && component.isShowing()) {
            setLocation(calculateDialogPosition(component));
        } else {
            setLocationRelativeTo(getParent());
        }
        setVisible(true);
    }

    private Point calculateDialogPosition(Component component) {
        Point componentLocation = component.getLocationOnScreen();
        Dimension componentSize = component.getSize();
        Dimension dialogSize = getSize();
        Rectangle screenBounds = getGraphicsConfiguration().getBounds();

        int x = Math.max(screenBounds.x,
                Math.min(componentLocation.x, screenBounds.width - dialogSize.width));

        int y = componentLocation.y + componentSize.height;
        if (y + dialogSize.height > screenBounds.height) {
            y = componentLocation.y - dialogSize.height;
        }

        return new Point(x, Math.max(screenBounds.y, y));
    }

    // --- Menu Management ---
    public void refreshAccess() {
        if (showingCategories) {
            showCategories();
        } else {
            showSubMenu(currentCategory);
        }
    }

    private void showCategories() {
        loadingLabel = new JLabel("Loading categories...", SwingConstants.CENTER);
        loadingLabel.setForeground(ColorConfig.getAccentColor());
        loadingLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        clearMenuPanel();
        menuPanel.add(loadingLabel);
        refreshMenuPanel();
        setComponentsEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                return null;
            }

            @Override
            protected void done() {
                try {
                    clearMenuPanel();
                    MenuLayoutManager.applyCategoryLayout(menuPanel);

                    List<MenuItem.Category> categories = getDisplayCategories();
                    categories.forEach(MenuShell.this::addCategoryButton);

                    updateUIForCategories();
                } finally {
                    setComponentsEnabled(true);
                }
            }
        }.execute();
    }

    private List<MenuItem.Category> getDisplayCategories() {
        return java.util.Arrays.stream(MenuItem.Category.values())
                .filter(cat -> cat != MenuItem.Category.ALL)
                .collect(Collectors.toList());
    }

    private void addCategoryButton(MenuItem.Category category) {
        JButton button = new GradientButton(
                category.toString(),
                new ImageIcon(getClass().getResource(CATEGORY_ICON))
        );
        button.setToolTipText(category.getTooltip());
        button.addActionListener(e -> showSubMenu(category));
        menuPanel.add(button);
    }

    private void showSubMenu(MenuItem.Category category) {
        resetMenuState(category);
        titleLabel.setText("Loading...");

        loadingLabel = new JLabel("Loading menu " + category + "...", SwingConstants.CENTER);
        loadingLabel.setForeground(Color.BLUE);
        loadingLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        menuPanel.add(loadingLabel);
        refreshMenuPanel();
        setComponentsEnabled(false);

        new SwingWorker<List<JButton>, Void>() {
            @Override
            protected List<JButton> doInBackground() {
                String search = searchField.getText().trim().toLowerCase();
                return menuService.getFilteredItems(category, search).stream()
                        .map(MenuShell.this::createMenuButton)
                        .collect(Collectors.toList());
            }

            @Override
            protected void done() {
                try {
                    updateMenuWithButtons(get());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(MenuShell.this,
                            "Error loading menu: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setComponentsEnabled(true);
                }
            }
        }.execute();
    }

    private void updateMenuWithButtons(List<JButton> buttons) {
        clearMenuPanel();
        buttons.forEach(menuPanel::add);
        MenuLayoutManager.applyMenuItemsLayout(menuPanel, buttons.size());
        refreshMenuPanel();

        titleLabel.setText(String.format("Category: %s | Total: %d", currentCategory, buttons.size()));
        backButton.setVisible(true);
    }

    private void resetMenuState(MenuItem.Category category) {
        clearMenuPanel();
        showingCategories = false;
        currentCategory = category;
    }

    private void navigateBack() {
        showingCategories = true;
        backButton.setVisible(false);
        refreshAccess();
    }

    // --- UI Helpers ---
    private void clearMenuPanel() {
        menuPanel.removeAll();
    }

    private void refreshMenuPanel() {
        menuPanel.revalidate();
        menuPanel.repaint();
    }

    private void setComponentsEnabled(boolean enabled) {
        closeButton.setEnabled(enabled);
        backButton.setEnabled(enabled);
        searchField.setEnabled(enabled);

        setCursor(enabled
                ? Cursor.getDefaultCursor()
                : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    private void updateUIForCategories() {
        titleLabel.setText("Select a category:");
        backButton.setVisible(false);
        refreshMenuPanel();
    }

    // --- Button Creation ---
    private JButton createMenuButton(MenuItem item) {
        JButton button = new GradientButton(item.label,
                new ImageIcon(getClass().getResource(item.iconPath)));

        button.addActionListener(e -> executeMenuItem(item));
        return button;
    }

    private void executeMenuItem(MenuItem item) {
        try {
            if (item.action != null) {
                item.executeAction();
            } else if (item.formClass != null) {
                openForm(item.formClass, item.label);
            } else {
                logWarning("No action or formClass defined for menu: " + item.label);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // --- Form Management ---
    private void openForm(Class<?> formClass, String menuName) {
        if (formClass == null) {
            logWarning("No formClass defined for menu: " + menuName);
            return;
        }

        logFormOpening(formClass);
        trackMenuAccess(menuName);
        prepareForFormTransition();

        try {
            showFormDialog(formClass);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            resetCursor();
        }
    }

    private void showFormDialog(Class<?> formClass) throws Exception {
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog form = (JDialog) formClass
                .getConstructor(Frame.class, boolean.class)
                .newInstance(parentFrame, false);

        form.setSize(getWidth(), getHeight());
        form.setLocation(getLocation());
        form.setVisible(true);
    }

    private void prepareForFormTransition() {
        dispose();
        setVisible(false);
        access.setCurrentForm("dashboard");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    private void resetCursor() {
        setCursor(Cursor.getDefaultCursor());
    }

    // --- Tracking & Logging ---
    private void trackMenuAccess(String menuName) {
        try {
            Connection conn = Database.getConnection();
            String sql = "INSERT INTO menu_access_log (menu_name, access_time) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, menuName);
                ps.setObject(2, LocalDateTime.now());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            logError("Failed to track menu access: " + e.getMessage());
        }
    }

    private void logFormOpening(Class<?> formClass) {
        System.out.println("Opening form: " + formClass.getSimpleName());
    }

    private void logWarning(String message) {
        System.err.println("[WARNING] " + message);
    }

    private void logError(String message) {
        System.err.println("[ERROR] " + message);
    }

    // --- Data Reload ---
    private void reloadItems() {
        setComponentsEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                menuService.loadAllItems();
                return null;
            }

            @Override
            protected void done() {
                refreshAccess();
            }
        }.execute();
    }

    // --- Inner Classes ---
    private class SearchKeyAdapter extends KeyAdapter {
        @Override
        public void keyReleased(KeyEvent evt) {
            String searchText = searchField.getText().trim();

            if (searchText.isEmpty()) {
                resetToCategoryView();
            } else {
                showSearchResults();
            }
        }

        private void resetToCategoryView() {
            showingCategories = true;
            currentCategory = null;
            refreshAccess();
        }

        private void showSearchResults() {
            showingCategories = false;
            currentCategory = MenuItem.Category.ALL;

            loadingLabel = new JLabel("Searching...", SwingConstants.CENTER);
            loadingLabel.setForeground(Color.BLUE);
            loadingLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

            clearMenuPanel();
            menuPanel.add(loadingLabel);
            refreshMenuPanel();
            setComponentsEnabled(false);

            showSubMenu(MenuItem.Category.ALL);
        }
    }
}
