export default {
  extends: ['stylelint-config-standard'],
  plugins: ['@projectwallace/stylelint-plugin'],
  rules: {
    // Preserve the site's established, compatibility-oriented CSS notation.
    'alpha-value-notation': null,
    'color-function-notation': null,
    'color-function-alias-notation': 'with-alpha',
    'color-hex-length': null,
    'media-feature-range-notation': null,
    'property-no-vendor-prefix': null,

    // Measured against the authored stylesheet; see README.md for the baseline.
    'max-nesting-depth': 1,
    'selector-max-specificity': '0,3,0',
    'selector-max-compound-selectors': 3,
    'selector-max-combinators': 2,
    'projectwallace/max-selector-complexity': 5,
    'projectwallace/max-average-selector-complexity': 2,
    'projectwallace/max-declarations-per-rule': 30,
    'projectwallace/max-selectors-per-rule': 6,
    'projectwallace/max-important-ratio': 0.01
  }
};
