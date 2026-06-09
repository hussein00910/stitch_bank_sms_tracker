# Design System Document: The Lucid Ledger

## 1. Overview & Creative North Star
### Creative North Star: "The Ethereal Vault"
In the traditional financial sector, "trust" is often represented by heavy stone pillars and dark, oppressive wood. This design system rejects that weight. Our North Star, **The Ethereal Vault**, balances the absolute security of financial data with a breathable, airy interface that feels like high-end editorial glass.

We break the "standard template" look through **Intentional Asymmetry** and **Tonal Depth**. By utilizing Material 3 logic but stripping away the rigid "boxes within boxes," we create a signature experience where data doesn't feel trapped—it feels curated. We favor white space over lines and light over shadows.

---

## 2. Colors: Tonal Atmosphere
The palette is rooted in `surface` (#f8f9fa) and `primary` (#006067). This is not just a light theme; it is a high-luminance environment designed to reduce cognitive load.

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders to define sections. Layout boundaries must be defined solely through background color shifts. 
*   *Example:* A `surface-container-low` (#f3f4f5) sidebar sitting against a `surface` (#f8f9fa) main stage.

### Surface Hierarchy & Nesting
Treat the UI as physical layers of frosted glass. Use the following hierarchy to "stack" importance:
1.  **Base Layer:** `surface` (#f8f9fa)
2.  **Sectioning:** `surface-container-low` (#f3f4f5)
3.  **Interactive Cards:** `surface-container-lowest` (#ffffff) — This creates a natural "pop" against the off-white background.
4.  **Floating Elements:** `surface-bright` (#f8f9fa) with Glassmorphism.

### The "Glass & Gradient" Rule
To move beyond a "flat" financial app, use Glassmorphism for navigation bars and floating action menus. Apply `backdrop-blur: 20px` with a semi-transparent `surface-container-lowest` at 80% opacity.
*   **Signature Textures:** Use a subtle linear gradient from `primary` (#006067) to `primary-container` (#007b83) for primary CTAs. This provides a "jewel-toned" depth that feels premium rather than utilitarian.

---

## 3. Typography: Editorial Authority
We utilize a dual-typeface system to balance character with readability.

*   **Display & Headlines (Manrope):** Chosen for its geometric confidence. Use `display-lg` (3.5rem) for high-impact balance summaries. The wide apertures of Manrope convey transparency.
*   **Body & UI (Inter):** The workhorse. Use `body-md` (0.875rem) for all financial tables and data entry. Its tall x-height ensures legibility in dense transaction lists.
*   **The "Hierarchy Shift":** Create editorial rhythm by pairing a `headline-sm` title with a significantly smaller, all-caps `label-md` in `on-surface-variant` for metadata. Avoid middle-ground sizes; lean into high contrast.

---

## 4. Elevation & Depth: Tonal Layering
Traditional drop shadows are banned. Depth is achieved through **Tonal Layering**.

*   **The Layering Principle:** Place a `surface-container-lowest` (#ffffff) card on top of a `surface-container` (#edeeef) background. The contrast in hex values provides all the "lift" required.
*   **Ambient Shadows:** If a floating element (like a modal) requires a shadow, use: `box-shadow: 0 20px 40px rgba(25, 28, 29, 0.05)`. It must be a tinted version of `on-surface`, extra-diffused to mimic natural light.
*   **The "Ghost Border" Fallback:** If a border is required for accessibility (e.g., in a high-density table), use `outline-variant` (#bdc9ca) at **15% opacity**.
*   **Glassmorphism:** Use `surface-tint` (#006970) at 5% opacity on glass layers to give a subtle "sky blue" cast to the transparency.

---

## 5. Components

### Buttons
*   **Primary:** Gradient of `primary` to `primary-container`. `xl` roundedness (0.75rem). No shadow.
*   **Secondary:** `secondary-container` (#d6e5e6) background with `on-secondary-container` (#596768) text.
*   **Tertiary:** Ghost style. No background. Use `primary` (#006067) text with a subtle `surface-container-high` hover state.

### Input Fields
*   **Styling:** Forgo the "outlined" box. Use a "Filled" style with `surface-container-highest` (#e1e3e4) and a 2px bottom stroke of `outline` (#6e797a) that transforms into `primary` on focus.
*   **Roundedness:** `md` (0.375rem) on top corners.

### Cards & Lists
*   **Constraint:** Forbid the use of divider lines.
*   **Execution:** Use 24px of vertical white space (from the Spacing Scale) to separate transaction items. For high-density lists, use alternating row backgrounds of `surface` and `surface-container-low`.

### Data Visualization (Signature Component)
*   **The "Pulse" Chart:** Financial charts should use `primary` for the line, with a soft gradient fill of `primary-fixed-dim` (#7ad5dd) at 10% opacity bleeding into the background.

---

## 6. Do's and Don'ts

### Do
*   **Do** use `9999px` (full) roundedness for chips and tags to create an "organic" feel.
*   **Do** utilize `on-surface-variant` for secondary information to maintain the "airy" aesthetic.
*   **Do** embrace asymmetry. In a dashboard, a large `display-md` balance can sit offset to the left, with action chips staggered to the right.

### Don't
*   **Don't** use 100% black (#000000). Always use `on-surface` (#191c1d) for text to keep the contrast "soft."
*   **Don't** use dark-themed cards. Even for "Total Balance" sections, use `primary` text on a `primary-fixed` (#96f1fa) background instead of a dark block.
*   **Don't** use standard Material 3 "Elevated" cards. Stick to "Tonal" or "Filled" logic to maintain the flat, editorial aesthetic.