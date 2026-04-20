# legacy-java-refactor-case-study

**Taming a 40,000-line Java class that ran a hospital.**

This is not a tutorial. This is a case study of what happens when you inherit a production system that nobody dares to touch — and decide to understand it anyway.

---

## What I Inherited

A Java Swing hospital management system built over years by multiple developers with no shared convention. The entry point: `MainForm.java`, a single file exceeding 40,000 lines.

| Problem              | Reality                                                 |
| -------------------- | ------------------------------------------------------- |
| Button count         | 900+ hardcoded buttons, each declared manually          |
| Duplicated filters   | 3 structurally identical filter methods (~5,000 lines each) |
| Action handlers      | Every button had its own method with the same boilerplate |
| Responsibility       | MainForm handled layout, access control, navigation, DB calls, and UI state — all at once |

The IDE warned me the file was too large to open comfortably. That was the first signal something was wrong.

---

## The First Layer — 900 Buttons, Each Declared by Hand

Every button in the system was created like this:

```java
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
// ... identical setup, different name
```

Then each button had its own action handler, all sharing the same body:

```java
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
    // ... identical body, different class
}
```

900 buttons. 900 declarations. 900 handlers. All doing the same thing with different class names.

---

## The Second Layer — Triple Access Filter

The system had three ways to filter the menu: empty search, active search, and category selection. Each was implemented as a completely separate method with identical structure:

```java
private void filterBySearch() {
    visibleMenuCount = 0;

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
    // ... repeated for every button, three times over
}
```

Estimated: ~5,000 lines per method × 3 methods = **~15,000 lines of structurally identical code**. Changing access logic for one menu item meant finding and editing three separate locations.

---

## The Third Layer — Hidden Logic in Action Handlers

Not every button was simple. While investigating, I found handlers like this hiding inside the uniform-looking list:

```java
private void onInsuranceClaimManualClicked(ActionEvent evt) {
    if (access.getRoleCode().equals("SuperAdmin")) {
        currentPage = "ManualClaimEntry";
        closeOpenDialogs();
        homeDialog.dispose();
        idSearchDialog.setVisible(true);
    } else {
        String coderId = Database.queryScalar(
            "SELECT coder_id FROM insurance_coder_registry WHERE user_id = ?",
            access.getRoleCode()
        );
        if (!coderId.equals("")) {
            externalClaimDialog.loadURL("http://" + Config.getWebHost() + "...");
            externalClaimDialog.setVisible(true);
            homeDialog.dispose();
        } else {
            JOptionPane.showMessageDialog(null, "Coder ID not found...");
        }
    }
}
```

This one could not be migrated into the standard pattern. It had branching role logic, URL construction, and a fallback dialog. Moving it blindly would have broken real behavior.

**This is the moment where most refactors go wrong — trying to force every case into one pattern.**

---

## What I Built

Instead of rewriting, I separated three concerns that had been living in one class:

### 1. Menu definition — *what* exists, *what* it opens, *who* can see it

```java
ROOM_INFORMATION(
    "Room Information",
    "/icons/room.png",
    RoomInformationForm.class,
    access::canViewRoomInformation,
    Category.A
),
```

### 2. Special cases — handlers with real branching logic get an explicit action parameter

```java
INSURANCE_CLAIM_MANUAL(
    "Insurance Claim — Manual Entry",
    "/icons/claim.png",
    InsuranceClaimForm.class,
    access::canProcessInsuranceClaimManual,
    Category.F,
    () -> openInsuranceClaimManual()   // explicit, not hidden
),
```

### 3. Rendering — `MenuShell` reads the definition and builds buttons. It does not know what each button does.

```java
private JButton createMenuButton(MenuItem item) {
    JButton button = new GradientButton(
        item.label,
        new ImageIcon(getClass().getResource(item.iconPath))
    );
    button.addActionListener(e -> executeMenuItem(item));
    return button;
}
```

### 4. Access filtering — one method, one place

```java
public List<MenuItem> getFilteredItems(Category category, String searchText) {
    return allItems.stream()
        .filter(item -> item.permission.getAsBoolean())
        .filter(item -> category == Category.ALL || item.category == category)
        .filter(item -> searchText.isEmpty() ||
            item.label.toLowerCase().contains(searchText.toLowerCase()))
        .collect(Collectors.toList());
}
```

Three filter methods, each around 5,000 lines, became a single stream pipeline of roughly 10 lines.

---

## Trade-off: What I Did *Not* Migrate

Some buttons were left with their original logic intact, accessed through the `action` parameter. The alternative — forcing every special case into the standard pattern — would have introduced subtle bugs in a production system still used daily.

When the system is live and patients depend on it, *"works correctly"* outranks *"looks clean."*

---

## Result

| Metric                      | Before                       | After                          |
| --------------------------- | ---------------------------- | ------------------------------ |
| Button declarations         | 900+ manual                  | 1 enum entry each              |
| Access filter methods       | 3 identical (~5,000 LOC each) | 1 stream (~10 LOC)             |
| Adding a new menu item      | 4 places to edit             | 1 enum constant                |
| Hidden logic                | Scattered across 900 handlers | Explicit `action` parameter    |
| `MainForm` responsibility   | Everything                   | Navigation entry point only    |

---

## Stack

Java 8 · Swing · JDBC · NetBeans project structure

No framework migrations. No rewrites. The system stayed in production throughout.

---

## Repository Structure

```
legacy-java-refactor-case-study/
├── README.md
├── before/
│   └── MainForm_fragment.java   ← reconstructed "before" state (dummy)
└── after/
    ├── MenuShell.java           ← rendering layer
    └── MenuItem.java            ← declarative menu model
```

All code in this repository is a **reconstruction for portfolio purposes**. No proprietary source from the original production system is included. The structural patterns shown are accurate to what was inherited.

---

## What This Demonstrates

- Reading and understanding code you did not write, with no documentation
- Identifying structural patterns inside noise (900 buttons, 15k lines of access logic)
- Knowing *when not* to apply a pattern (the insurance claim case)
- Refactoring a live production system without breaking it

---

*Part of the Mihaaq-arch portfolio — legacy system modernization for healthcare.*
