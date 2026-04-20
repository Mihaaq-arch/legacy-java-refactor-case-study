package refactored.menu.model;

import refactored.core.access;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 * MenuItem — declarative data model for a menu entry.
 *
 * Replaces 900+ hand-coded buttons and their handlers with a single enum.
 * Each entry declares what it is (label, icon), who can see it (permission),
 * where it belongs (category), and what it opens (form class or custom action).
 *
 * Most items map to a form class. Items with non-standard logic supply
 * an explicit `action: Runnable` instead — keeping special cases visible
 * rather than hidden inside a 40,000-line file.
 *
 * NOTE: Only a representative sample of entries is shown below. The original
 * enum contains ~900 entries across 20 categories.
 */
public class MenuItem {

    public final String label;
    public final String iconPath;
    public final Class<?> formClass;
    public final BooleanSupplier permission;
    public final Category category;
    public final Runnable action;

    /** Standard constructor — most entries use this */
    public MenuItem(String label, String iconPath, Class<?> formClass,
                    BooleanSupplier permission, Category category) {
        this(label, iconPath, formClass, permission, category, null);
    }

    /** Extended constructor — for entries with non-standard logic */
    public MenuItem(String label, String iconPath, Class<?> formClass,
                    BooleanSupplier permission, Category category, Runnable action) {
        this.label = label;
        this.iconPath = iconPath;
        this.formClass = formClass;
        this.permission = permission;
        this.category = category;
        this.action = action;
    }

    public void executeAction() {
        if (action != null) {
            action.run();
        } else if (formClass != null) {
            openFormClass();
        }
    }

    private void openFormClass() {
        try {
            java.lang.reflect.Constructor<?> constructor = formClass.getDeclaredConstructor();
            Object formInstance = constructor.newInstance();
            if (formInstance instanceof JFrame) {
                ((JFrame) formInstance).setVisible(true);
            } else if (formInstance instanceof JDialog) {
                ((JDialog) formInstance).setVisible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Build the runtime list of menu items from the enum definition */
    public static List<MenuItem> getAllItems() {
        return Arrays.stream(MenuDefinition.values())
                .filter(def -> def.getAccessCondition().getAsBoolean())
                .map(def -> new MenuItem(
                        def.getLabel(),
                        def.getIcon(),
                        def.getTargetClass(),
                        def.getAccessCondition(),
                        def.getCategory(),
                        def.getAction()
                ))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // MenuDefinition — the single source of truth for all menu entries
    //
    // The original enum contains ~900 entries. A representative sample is
    // shown here: standard form-mapped entries, permission-gated entries,
    // and special-case entries with explicit action handlers.
    // =========================================================================

    private enum MenuDefinition {

        //region  [ A ]  Registration & Billing
        ROOM_INFORMATION("Room Information", "/icons/room.png",
                RoomInformationForm.class, access::canViewRoomInformation, Category.A),

        APPOINTMENT_BOOKING("Appointment Booking", "/icons/booking.png",
                AppointmentBookingForm.class, access::canBookAppointment, Category.A),

        REGISTRATION_BOOKING("Registration Booking", "/icons/register.png",
                RegistrationBookingForm.class, access::canBookRegistration, Category.A),

        DEPOSIT("Patient Deposit", "/icons/money.png",
                DepositForm.class, access::canManageDeposit, Category.A),

        DISCHARGE_REGISTRY("Discharge Registry", "/icons/discharge.png",
                DischargeRegistryForm.class, access::canManageDischarge, Category.A),
        //endregion

        //region  [ B ]  Payroll & Employee Management
        PAYROLL_DASHBOARD("Payroll Dashboard", "/icons/wallet.png",
                null, () -> access.isPayrollAdmin() || access.isPayrollUser(),
                Category.B, () -> openPayrollDashboard()),

        EMPLOYEE_REGISTRY("Employee Registry", "/icons/employee.png",
                EmployeeRegistryForm.class, access::canManageEmployees, Category.B),

        ATTENDANCE_LOG("Attendance Log", "/icons/clock.png",
                AttendanceLogForm.class, access::canViewAttendance, Category.B),
        //endregion

        //region  [ C ]  Inventory — Medical
        MEDICATION_CATALOG("Medication Catalog", "/icons/pill.png",
                MedicationCatalogForm.class, access::canViewMedicationCatalog, Category.C),

        MEDICATION_CATEGORY("Medication Category", "/icons/category.png",
                MedicationCategoryForm.class, access::canManageMedicationCategory, Category.C),

        MEDICATION_DISPENSE("Medication Dispense", "/icons/dispense.png",
                MedicationDispenseForm.class, access::canDispenseMedication, Category.C),
        //endregion

        //region  [ D ]  Inventory — Non-Medical
        EQUIPMENT_REGISTRY("Equipment Registry", "/icons/equipment.png",
                EquipmentRegistryForm.class, access::canManageEquipment, Category.D),

        SUPPLIER_REGISTRY("Supplier Registry", "/icons/supplier.png",
                SupplierRegistryForm.class, access::canManageSuppliers, Category.D),
        //endregion

        //region  [ E ]  Reports & Analytics
        DAILY_VISIT_REPORT("Daily Visit Report", "/icons/report.png",
                DailyVisitReportForm.class, access::canViewReports, Category.E),

        FINANCIAL_SUMMARY("Financial Summary", "/icons/finance.png",
                FinancialSummaryForm.class, access::canViewFinancials, Category.E),
        //endregion

        //region  [ F ]  External Integrations
        //
        // Special-case entries: use explicit `action` parameter for logic
        // that does not fit the "open form" pattern — role branching, URL
        // construction, external authentication, etc.
        //
        INSURANCE_CLAIM_MANUAL("Insurance Claim — Manual Entry",
                "/icons/claim.png",
                InsuranceClaimForm.class, access::canProcessInsuranceClaimManual,
                Category.F, () -> openInsuranceClaimManual()),

        INSURANCE_CLAIM_AUTO("Insurance Claim — Automatic",
                "/icons/claim-auto.png",
                InsuranceClaimForm.class, access::canProcessInsuranceClaimAuto,
                Category.F),

        NATIONAL_HEALTH_SEND_ENCOUNTER("Send Encounter to National Platform",
                "/icons/health-platform.png",
                NationalHealthEncounterForm.class, access::canSendEncounter,
                Category.F),
        //endregion

        //region  [ G ]  System Settings
        USER_MANAGEMENT("User Management", "/icons/users.png",
                UserManagementForm.class, access::canManageUsers, Category.G),

        APP_SETTINGS("Application Settings", "/icons/settings.png",
                AppSettingsForm.class, access::canManageAppSettings, Category.G),
        //endregion

        ;

        // --- Enum infrastructure ---

        private final String label;
        private final String icon;
        private final Class<?> targetClass;
        private final MenuAccessCondition accessCondition;
        private final Category category;
        private final Runnable action;

        MenuDefinition(String label, String icon, Class<?> targetClass,
                       MenuAccessCondition accessCondition, Category category) {
            this(label, icon, targetClass, accessCondition, category, null);
        }

        MenuDefinition(String label, String icon, Class<?> targetClass,
                       MenuAccessCondition accessCondition, Category category, Runnable action) {
            this.label = label;
            this.icon = icon;
            this.targetClass = targetClass;
            this.accessCondition = accessCondition;
            this.category = category;
            this.action = action;
        }

        public String getLabel() { return label; }
        public String getIcon() { return icon; }
        public Class<?> getTargetClass() { return targetClass; }
        public MenuAccessCondition getAccessCondition() { return accessCondition; }
        public Category getCategory() { return category; }
        public Runnable getAction() { return action; }
    }

    @FunctionalInterface
    public interface MenuAccessCondition extends BooleanSupplier {
        @Override
        boolean getAsBoolean();
    }

    // =========================================================================
    // Service — runtime filtering logic
    //
    // Replaces the original three identical filter methods (~15,000 lines)
    // with a single stream pipeline.
    // =========================================================================

    public static class Service {
        private final List<MenuItem> allItems = new ArrayList<>();

        public void loadAllItems() {
            allItems.clear();
            allItems.addAll(MenuItem.getAllItems());
        }

        public List<MenuItem> getFilteredItems(Category category, String searchText) {
            String searchLower = searchText.trim().toLowerCase();

            return allItems.stream()
                    .filter(item -> item.permission.getAsBoolean())
                    .filter(item -> shouldIncludeInCategory(item, category))
                    .filter(item -> shouldIncludeInSearch(item, searchLower))
                    .collect(Collectors.toList());
        }

        private boolean shouldIncludeInCategory(MenuItem item, Category category) {
            return category == Category.ALL || item.category == category;
        }

        private boolean shouldIncludeInSearch(MenuItem item, String searchLower) {
            return searchLower.isEmpty() || item.label.toLowerCase().contains(searchLower);
        }

        public List<MenuItem> getAllItems() {
            return new ArrayList<>(allItems);
        }

        public int getTotalItemCount() {
            return allItems.size();
        }
    }

    // =========================================================================
    // Category — menu taxonomy
    // =========================================================================

    public enum Category {
        A("[ A ]  Registration & Billing ",      " Patient registration, billing, and service delivery "),
        B("[ B ]  Payroll & Employee Management ", " Staff attendance, payroll, and HR operations "),
        C("[ C ]  Inventory — Medical ",         " Medications, medical supplies, and dispensing "),
        D("[ D ]  Inventory — Non-Medical ",     " Equipment, supplies, and supplier management "),
        E("[ E ]  Reports & Analytics ",         " Visit reports, financial summaries, analytics "),
        F("[ F ]  External Integrations ",       " Insurance claims, national health platform, third-party APIs "),
        G("[ G ]  System Settings ",             " User management, permissions, application configuration "),
        ALL("[ * ]  All ",                       " Show all accessible menu items ");

        private final String displayName;
        private final String tooltip;

        Category(String displayName, String tooltip) {
            this.displayName = displayName;
            this.tooltip = tooltip;
        }

        @Override
        public String toString() { return displayName; }

        public String getTooltip() { return tooltip; }
    }

    // =========================================================================
    // Special-case handlers — complex logic extracted from the original
    // 900 action handlers. These are the ~5% of cases that do not fit
    // the "open form" pattern.
    // =========================================================================

    private static void openPayrollDashboard() {
        if (!access.isPayrollAdmin() && !access.isPayrollUser()) {
            JOptionPane.showMessageDialog(null, "You do not have access to this module.");
            return;
        }

        try {
            String url = buildPayrollUrl();
            PayrollDashboard.show(url);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }

    private static String buildPayrollUrl() {
        String role = access.isPayrollAdmin() ? "admin" : "user";
        return "http://" + "example-host" + ":8080/payroll/login.php?act=login&role=" + role;
    }

    private static void openInsuranceClaimManual() {
        // Non-standard logic from the original codebase: role check,
        // conditional URL construction, fallback to error dialog.
        // Kept here as an explicit action rather than forced into
        // the standard form-opening pattern.
        if (access.getRoleCode().equals("SuperAdmin")) {
            InsuranceClaimForm.openWithSearchDialog();
        } else {
            String coderId = access.lookupCoderId();
            if (coderId != null && !coderId.isEmpty()) {
                InsuranceClaimForm.openWithCoderId(coderId);
            } else {
                JOptionPane.showMessageDialog(null,
                        "Coder ID not found — please contact the system administrator.");
            }
        }
    }

    // --- Placeholder form class references (demonstration only) ---
    private static class RoomInformationForm extends JDialog {}
    private static class AppointmentBookingForm extends JDialog {}
    private static class RegistrationBookingForm extends JDialog {}
    private static class DepositForm extends JDialog {}
    private static class DischargeRegistryForm extends JDialog {}
    private static class EmployeeRegistryForm extends JDialog {}
    private static class AttendanceLogForm extends JDialog {}
    private static class MedicationCatalogForm extends JDialog {}
    private static class MedicationCategoryForm extends JDialog {}
    private static class MedicationDispenseForm extends JDialog {}
    private static class EquipmentRegistryForm extends JDialog {}
    private static class SupplierRegistryForm extends JDialog {}
    private static class DailyVisitReportForm extends JDialog {}
    private static class FinancialSummaryForm extends JDialog {}
    private static class NationalHealthEncounterForm extends JDialog {}
    private static class UserManagementForm extends JDialog {}
    private static class AppSettingsForm extends JDialog {}

    private static class InsuranceClaimForm extends JDialog {
        static void openWithSearchDialog() {}
        static void openWithCoderId(String coderId) {}
    }

    private static class PayrollDashboard {
        static void show(String url) {}
    }
}
