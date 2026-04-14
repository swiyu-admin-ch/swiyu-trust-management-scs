module.exports = {
  $schema: "https://json.schemastore.org/prettierrc",
  printWidth: 120,
  plugins: ["prettier-plugin-organize-imports"],
  overrides: [
    {
      files: "*.ts",
      options: {
        semi: true,
        singleQuote: true,
        bracketSpacing: false,
        useTabs: false,
        trailingComma: "none",
        arrowParens: "avoid",
      },
    },
    {
      files: "**/i18n/*.json",
      options: {
        plugins: ["prettier-plugin-sort-json"],
      },
    },
    {
      files: "*.html",
      options: {
        parser: "angular",
      },
    },
  ],
};
