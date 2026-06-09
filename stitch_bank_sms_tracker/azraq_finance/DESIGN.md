# Design System Document: The Precision Vault

## 1. Overview & Creative North Star
The "Precision Vault" is the creative north star of this design system. In an industry often cluttered with complexity, this system moves beyond standard financial utility to achieve a "High-End Editorial" experience. We treat financial data not as raw numbers, but as a curated gallery of wealth management. 

By leveraging the structural integrity of Material Design 3 and infusing it with a bespoke, layered aesthetic, we create an environment that feels both indestructible and fluid. The design breaks the "template" look through intentional white space, high-contrast typography scales, and a departure from traditional "boxed" layouts in favor of tonal depth.

**RTL Priority:** As an Arabic-first system, the layout logic is flipped to prioritize the right-to-left reading pattern. The most critical data anchors the top-right, while secondary actions gracefully lead the eye toward the left.

---

## 2. Colors
Our palette is rooted in a deep, authoritative navy (`primary`) contrasted by a sophisticated, energetic teal (`secondary`). This creates a "Tech-Wealth" aesthetic that signals both legacy and innovation.

### The "No-Line" Rule
Explicitly prohibited: 1px solid borders for sectioning or containment. Boundaries must be defined solely through background color shifts. For example, a `surface-container-low` section sitting on a `surface` background provides all the definition needed without the visual "noise" of a stroke.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers—like stacked sheets of fine paper. 
- **Layer 1 (Background):** `surface` (#f8f9fa)
- **Layer 2 (Content Areas):** `surface-container-low` (#f3f4f5)
- **Layer 3 (Elevated Cards):** `surface-container-highest` (#e1e3e4)

### The "Glass & Gradient" Rule
To elevate the experience above a standard Android app, use Glassmorphism for floating navigation bars or modal headers. Utilize semi-transparent versions of `surface` with a 20px-30px backdrop blur. 
- **Signature Texture:** For primary CTAs and Hero sections, apply a subtle linear gradient (45 degrees) from `primary` (#000614) to `primary-container` (#001e41). This adds "soul" and depth to the deep navy.

---

## 3. Typography
We utilize **Inter** (or a high-quality Arabic equivalent like **IBM Plex Sans Arabic**) to bridge the gap between technical precision and editorial elegance.

- **Display (Large/Medium):** Used for total balance figures. These are the "Hero" numbers of the app. Bold, high-contrast, and unapologetically large.
- **Headlines:** Set the stage for new sections (e.g., "النشاط الأخير"). They provide the "Editorial" structure.
- **Body:** Focused on readability. Use `body-lg` for transactional descriptions and `body-md` for metadata.
- **Labels:** Reserved for navigation and small data tags.

**Financial Hierarchy:** The use of `title-lg` for currency symbols and `display-md` for integers creates a "tiered" reading experience that makes wealth status immediately clear.

---

## 4. Elevation & Depth
Depth is achieved through **Tonal Layering** rather than traditional structural lines or heavy shadows.

### The Layering Principle
Stacking surface-container tiers creates a natural lift. A card using `surface-container-lowest` placed on a `surface-container-low` background creates a soft, tactile separation that feels native to a premium OS environment.

### Ambient Shadows
When a "floating" element (like a FAB or a modal) is required, use extra-diffused shadows:
- **Blur:** 24px to 40px.
- **Opacity:** 4% - 8%.
- **Tint:** The shadow must be tinted with the `on-surface` color (#191c1d) rather than pure black to mimic natural, ambient lighting.

### The "Ghost Border" Fallback
If a border is required for extreme accessibility or form fields, use a "Ghost Border": the `outline-variant` token at **15% opacity**. Never use 100% opaque borders.

---

## 5. Components

### Buttons
- **Primary:** High-pill shape (`full` roundness). Uses the Signature Gradient (Primary to Primary-Container). Text is `on-primary`.
- **Secondary:** Surface-based with no border. Uses `secondary-container` background with `on-secondary-container` text.
- **Tertiary:** Text-only, using `primary` color. No container.

### Input Fields
- **Architecture:** Abandon the "boxed" input. Use a `surface-container-low` background with a `primary` indicator line (2px) only on focus.
- **Alignment:** Labels must be right-aligned (RTL) and use `label-md` in `on-surface-variant`.

### Cards & Lists (The Dividerless Rule)
- **Dividers are forbidden.** Separate list items using 12dp or 16dp of vertical white space. 
- For complex lists, use a subtle background shift (e.g., alternating between `surface` and `surface-container-low`) to create a striped "Zebra" effect that guides the eye without "cutting" the screen with lines.

### Specialized Financial Components
- **Trend Indicators:** Instead of simple arrows, use micro-sparklines (miniature charts) using `tertiary` (#000802) for growth and `error` (#ba1a1a) for decline.
- **Balance Scrubber:** A custom slider for navigating historical data, using `secondary` (Teal) for the thumb to provide a "pop" of action against the Navy backdrop.

---

## 6. Do's and Don'ts

### Do
- **Do** prioritize white space over containment. If a layout feels crowded, remove a container before you shrink the text.
- **Do** use the Teal (`secondary`) sparingly. It is a surgical tool used to draw the eye to "Growth" or "Action."
- **Do** ensure all typography is right-aligned to respect the natural flow of the Arabic script.

### Don't
- **Don't** use 1px solid dividers. They "shatter" the editorial flow and make the app look like a legacy system.
- **Don't** use high-saturation reds for financial loss. Use the defined `error` token which is sophisticated and readable.
- **Don't** mix rounded corner values. Stick to the `md` (0.75rem) for cards and `full` for buttons to maintain a consistent visual language.

---

## 7. Implementation Tokens (Summary)
- **Primary Action:** Gradient (#000614 → #001e41)
- **Data Highlight:** `secondary` (#006a6a)
- **Surface Nesting:** `surface` (#f8f9fa) → `surface-container-low` (#f3f4f5)
- **Border Fallback:** `outline-variant` @ 15% opacity.
- **Corner Radius:** Cards (12px), Buttons (Global Max).