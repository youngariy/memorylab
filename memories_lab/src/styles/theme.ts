/**
 * Unified Design System
 * Consistent colors, spacing, typography, and breakpoints
 */

export const theme = {
  // Colors
  colors: {
    // Primary
    primary: '#007bff',
    primaryHover: '#0056b3',
    primaryLight: 'rgba(0, 123, 255, 0.1)',

    // Secondary
    secondary: '#6c757d',
    secondaryHover: '#5a6268',

    // Background
    bgPrimary: '#1a1a1f',
    bgSecondary: '#2a2a2f',
    bgTertiary: '#35353a',
    bgHover: 'rgba(255, 255, 255, 0.05)',

    // Text
    textPrimary: '#ffffff',
    textSecondary: '#ababab',
    textTertiary: '#888888',
    textMuted: '#666666',

    // Status
    success: '#28a745',
    successHover: '#218838',
    error: '#ff4444',
    errorHover: '#cc0000',
    warning: '#ffc107',
    info: '#17a2b8',

    // UI Elements
    border: '#444444',
    borderLight: '#555555',
    divider: '#35353a',

    // Overlays
    overlay: 'rgba(0, 0, 0, 0.5)',
    overlayLight: 'rgba(0, 0, 0, 0.3)',
  },

  // Spacing (in px)
  spacing: {
    xs: '4px',
    sm: '8px',
    md: '16px',
    lg: '24px',
    xl: '32px',
    xxl: '40px',
    xxxl: '48px',
  },

  // Typography
  typography: {
    fontFamily: {
      base: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
      mono: 'Menlo, Monaco, "Courier New", monospace',
    },
    fontSize: {
      xs: '12px',
      sm: '14px',
      md: '16px',
      lg: '18px',
      xl: '20px',
      xxl: '24px',
      xxxl: '32px',
      display: '48px',
    },
    fontWeight: {
      normal: 400,
      medium: 500,
      semibold: 600,
      bold: 700,
    },
    lineHeight: {
      tight: 1.2,
      normal: 1.5,
      relaxed: 1.75,
    },
  },

  // Border radius
  borderRadius: {
    sm: '4px',
    md: '8px',
    lg: '12px',
    xl: '16px',
    full: '9999px',
  },

  // Shadows
  shadows: {
    sm: '0 1px 2px rgba(0, 0, 0, 0.05)',
    md: '0 4px 6px rgba(0, 0, 0, 0.1)',
    lg: '0 10px 15px rgba(0, 0, 0, 0.1)',
    xl: '0 20px 25px rgba(0, 0, 0, 0.15)',
  },

  // Transitions
  transitions: {
    fast: '0.15s ease',
    normal: '0.2s ease',
    slow: '0.3s ease',
  },

  // Z-index
  zIndex: {
    dropdown: 1000,
    sticky: 1020,
    fixed: 1030,
    modalBackdrop: 1040,
    modal: 1050,
    popover: 1060,
    tooltip: 1070,
  },

  // Breakpoints (in px)
  breakpoints: {
    mobile: 480,
    tablet: 768,
    desktop: 1024,
    wide: 1280,
  },
} as const;

// Media queries helper
export const media = {
  mobile: `@media (max-width: ${theme.breakpoints.mobile}px)`,
  tablet: `@media (max-width: ${theme.breakpoints.tablet}px)`,
  desktop: `@media (min-width: ${theme.breakpoints.desktop}px)`,
  wide: `@media (min-width: ${theme.breakpoints.wide}px)`,
} as const;

// Export type for TypeScript
export type Theme = typeof theme;
