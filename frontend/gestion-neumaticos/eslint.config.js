import js from '@eslint/js'
import globals from 'globals'
import boundaries from 'eslint-plugin-boundaries'
import { createTypeScriptImportResolver } from 'eslint-import-resolver-typescript'
import { importX } from 'eslint-plugin-import-x'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
    },
  },

  // Orden de imports: react → externos → alias `@/` (alfabético) → relativos,
  // con una línea en blanco entre bloques. Deja de ser criterio de cada archivo.
  {
    files: ['src/**/*.{ts,tsx}'],
    plugins: { 'import-x': importX },
    settings: {
      'import-x/resolver-next': [
        createTypeScriptImportResolver({ project: './tsconfig.app.json' }),
      ],
    },
    rules: {
      'import-x/order': [
        'error',
        {
          groups: [
            'builtin',
            'external',
            'internal',
            'parent',
            'sibling',
            'index',
          ],
          pathGroups: [
            { pattern: 'react', group: 'external', position: 'before' },
            { pattern: 'react-dom/**', group: 'external', position: 'before' },
            { pattern: '@/**', group: 'internal' },
          ],
          pathGroupsExcludedImportTypes: ['react'],
          'newlines-between': 'always',
          alphabetize: { order: 'asc', caseInsensitive: true },
        },
      ],
    },
  },

  // Límites entre capas: el equivalente frontend de los módulos de Spring
  // Modulith del backend. La regla central es que un feature nunca importa a
  // otro feature; lo común (components/hooks/lib) tampoco conoce features.
  {
    files: ['src/**/*.{ts,tsx}'],
    plugins: { boundaries },
    settings: {
      // boundaries resuelve los imports con el resolver clásico de eslint-plugin-import.
      'import/resolver': { typescript: { project: './tsconfig.app.json' } },
      // main.tsx no es una capa: se clasifica como archivo, no como elemento.
      'boundaries/files': [{ category: 'entry', pattern: 'src/main.tsx' }],
      // El orden importa: gana el primer patrón que matchea, así que `shell`
      // (components/layout) va antes que el `shared` genérico de components.
      'boundaries/elements': [
        { type: 'app', pattern: 'src/app' },
        { type: 'feature', pattern: 'src/features/*', capture: ['featureName'] },
        { type: 'shell', pattern: 'src/components/layout' },
        { type: 'shared', pattern: 'src/components' },
        { type: 'shared', pattern: 'src/hooks' },
        { type: 'shared', pattern: 'src/lib' },
      ],
    },
    rules: {
      'boundaries/dependencies': [
        'error',
        {
          default: 'disallow',
          policies: [
            // La raíz compone: providers + router.
            {
              from: { file: { categories: 'entry' } },
              allow: {
                to: { element: { types: { anyOf: ['app', 'shared'] } } },
              },
            },
            // El router conoce todas las páginas; los providers, lo común.
            {
              from: { element: { type: 'app' } },
              allow: {
                to: {
                  element: {
                    types: { anyOf: ['app', 'feature', 'shell', 'shared'] },
                  },
                },
              },
            },
            // El shell puede consumir la API pública de `auth` (sesión/logout).
            {
              from: { element: { type: 'shell' } },
              allow: [
                {
                  to: { element: { types: { anyOf: ['shell', 'shared'] } } },
                },
                {
                  to: {
                    element: {
                      type: 'feature',
                      captured: { featureName: 'auth' },
                    },
                  },
                },
              ],
            },
            // Un feature usa lo común y su propio código, nunca otro feature.
            {
              from: { element: { type: 'feature' } },
              allow: [
                {
                  to: { element: { types: { anyOf: ['shared', 'shell'] } } },
                },
                {
                  to: {
                    element: {
                      type: 'feature',
                      captured: {
                        featureName: '{{from.captured.featureName}}',
                      },
                    },
                  },
                },
              ],
            },
            // Lo común no conoce features ni el shell.
            {
              from: { element: { type: 'shared' } },
              allow: { to: { element: { type: 'shared' } } },
            },
          ],
        },
      ],
    },
  },

  // src/components/ui es código vendorizado de shadcn: por diseño exporta
  // hooks y variantes (useSidebar, buttonVariants) junto a los componentes, y
  // el orden de sus imports lo decide el CLI.
  {
    files: ['src/components/ui/**/*.{ts,tsx}'],
    rules: {
      'react-refresh/only-export-components': 'off',
      'import-x/order': 'off',
    },
  },
])
