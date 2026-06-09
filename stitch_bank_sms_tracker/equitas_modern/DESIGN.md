# Design System Specification: The Architectural Trust

## 1. Overview & Creative North Star
### The Creative North Star: "The Financial Atelier"
This design system rejects the "commoditized" look of modern fintech in favor of a high-end, editorial experience. We treat financial data not as a series of rows and columns, but as a curated gallery of personal growth. 

The aesthetic is built on **Architectural Layering**—moving away from the flat, boxed-in web of the past decade toward an environment that feels spatial, quiet, and profoundly secure. By utilizing intentional asymmetry in layout and a "No-Line" philosophy, we create an interface that feels less like software and more like a private banking lounge.

## 2. Colors: Tonal Depth & Soul
We use color to communicate stability (Deep Blues) and prosperity (Emerald Greens). The goal is a "high-contrast, high-clarity" environment that feels premium.

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders for sectioning or grouping. 
*   Boundaries must be defined by **background shifts**. Use `surface-container-low` for secondary sections sitting on a `surface` background.
*   The layout should breathe; let the eye find the edge through the transition of light, not a stroke.

### Surface Hierarchy & Nesting
Instead of a flat grid, treat the UI as stacked sheets of fine paper.
*   **Base:** `surface` (#f7f9fb)
*   **Structural Sections:** `surface-container-low` (#f2f4f6)
*   **Interactive Components:** `surface-container-lowest` (#ffffff) for high-elevation cards.

### The "Glass & Gradient" Rule
To inject "soul" into the professional aesthetic:
*   **Primary CTAs:** Use a subtle linear gradient from `primary` (#00346f) to `primary_container` (#004a99) at a 135-degree angle. This adds a tactile, "weighted" feel to buttons.
*   **Overlays:** For floating navigation or modals, use `surface_container_lowest` at 80% opacity with a `24px` backdrop-blur. This ensures the deep blues of the brand peek through, maintaining context.

## 3. Typography: Editorial Authority
The type system pairs the geometric precision of **Inter** with the sophisticated, wide character set of **Manrope**.

*   **The Display Scale (Manrope):** Use `display-lg` to `display-sm` for account balances and "Big Wins." The high x-height of Manrope conveys modern transparency.
*   **The Headline Scale (Manrope):** Use for section headers. These should be set with tighter letter-spacing (-0.02em) to feel like a premium financial broadsheet.
*   **The Body & Label Scale (Inter):** Used for all transactional data and micro-copy. Inter’s legibility at small sizes ensures that even the densest ledger remains scannable.

## 4. Elevation & Depth: Tonal Layering
Traditional drop shadows are too "digital." We utilize **Tonal Layering** to create a sense of physical security.

*   **The Layering Principle:** To lift a card, do not add a shadow immediately. First, try placing a `surface-container-lowest` card on a `surface-container-low` background. The slight shift from #f2f4f6 to #ffffff creates a "soft lift."
*   **Ambient Shadows:** If a floating state is required (e.g., a bottom sheet), use a shadow with a blur of `40px`, Y-offset of `12px`, and an opacity of 4% using the `on-surface` color. It should feel like a soft glow of light, not a dark smudge.
*   **The Ghost Border Fallback:** For input fields where definition is mandatory, use `outline-variant` (#c2c6d3) at 20% opacity. It should be "felt, not seen."

## 5. Components
### Cards & Transactions
*   **Rule:** Forbid divider lines between transactions. 
*   **Execution:** Use `body-md` for the merchant name and `label-md` for the category. Separate transactions using `16px` of vertical whitespace. If a visual break is needed, use a subtle background tint change on hover.
*   **Transaction Status:** Use `secondary` (#006c47) for growth/credits and `error` (#ba1a1a) for debits, but apply them to small, high-chroma indicator dots or typography, never large disruptive blocks.

### Buttons
*   **Primary:** Gradient fill (`primary` to `primary_container`). Border radius `lg` (0.5rem). Text is `on_primary` (#ffffff).
*   **Secondary:** No background. Use `primary` text with a `Ghost Border` (outline-variant at 20%).
*   **Tertiary:** No background, no border. Use `primary` text.

### Inputs & Forms
*   **Default State:** `surface-container-highest` background with a bottom-only `Ghost Border`. This mimics the "signature line" on a check.
*   **Focus State:** The bottom border transitions to 2px `primary` color. No "halo" or outer glow.

### Financial Charts
*   **Area Charts:** Use `secondary` (#006c47) with a gradient fade to 0% opacity. 
*   **Data Points:** Use `secondary_fixed` (#8df7c1) for "current value" indicators to provide a "glow" effect against the deep blue backgrounds.

## 6. Do’s and Don’ts

### Do
*   **Do** use extreme whitespace (32px+) between major modules to create a sense of "calm luxury."
*   **Do** use `on_surface_variant` (#424751) for secondary data like dates and timestamps to keep the hierarchy clear.
*   **Do** ensure all charts have an "active" state that uses glassmorphism tooltips.

### Don't
*   **Don't** use pure black (#000000) anywhere. It breaks the sophisticated tonal range of the deep blues.
*   **Don't** use sharp corners. Use the `lg` (0.5rem) or `xl` (0.75rem) roundedness tokens to make the security feel "approachable."
*   **Don't** use standard 1px borders to separate the navigation bar. Use a `surface-dim` background shift or a backdrop-blur.

## 7. Spacing Scale
The spacing system is strictly 8pt-based, but for this system, we favor **Large-Scale Breathing Room**:
*   **Section Gaps:** 48px, 64px.
*   **Component Padding:** 24px (consistent across all cards).
*   **Inline Spacing:** 8px, 12px for related data points.