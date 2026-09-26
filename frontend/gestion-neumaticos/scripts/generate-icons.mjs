/**
 * Genera el set de íconos de marca a partir de `assets/brand/source-tire.jpg`.
 *
 *   npm run icons
 *
 * Salidas:
 *   - src/components/brand/tire-icon-path.ts  (path vectorial usado por <TireIcon />)
 *   - public/favicon.svg | favicon.ico | logo-light.svg | logo-dark.svg
 *   - public/icon-{192,512}.png (negro) e icon-{192,512}-dark.png (blanco)
 *   - public/icon-maskable-512.png | apple-touch-icon.png (fondo sólido)
 */
import { mkdir, writeFile } from "node:fs/promises"
import path from "node:path"
import { fileURLToPath } from "node:url"

import pngToIco from "png-to-ico"
import potrace from "potrace"
import sharp from "sharp"

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..")
const source = path.join(root, "assets/brand/source-tire.jpg")
const publicDir = path.join(root, "public")
const brandDir = path.join(root, "src/components/brand")

const CANVAS = 1024
// Margen interno para que el ícono no toque los bordes del lienzo.
const MARGIN = 0.05

function trace(buffer, options) {
  return new Promise((resolve, reject) => {
    potrace.trace(buffer, options, (error, svg) => {
      if (error) {
        reject(error)
        return
      }

      resolve(svg)
    })
  })
}

/** Recorta el fondo blanco, cuadra el recorte, suaviza el ruido y binariza. */
async function normalizeSource() {
  const trimmed = await sharp(source)
    .flatten({ background: "#ffffff" })
    .greyscale()
    .normalise()
    .blur(1.5)
    .threshold(128)
    .trim({ background: "#ffffff", threshold: 10 })
    .toBuffer({ resolveWithObject: true })

  const { width, height } = trimmed.info
  const side = Math.round(Math.max(width, height) * (1 + MARGIN * 2))

  // `extend` se resuelve en un pase aparte: sharp siempre aplica resize antes
  // de extend dentro de un mismo pipeline.
  const squared = await sharp(trimmed.data)
    .extend({
      top: Math.floor((side - height) / 2),
      bottom: Math.ceil((side - height) / 2),
      left: Math.floor((side - width) / 2),
      right: Math.ceil((side - width) / 2),
      background: "#ffffff",
    })
    .png()
    .toBuffer()

  return sharp(squared)
    .resize(CANVAS, CANVAS, { fit: "fill", kernel: "lanczos3" })
    .blur(2)
    .threshold(128)
    .png()
    .toBuffer()
}

/** Extrae los `d` de los paths que devuelve potrace. */
function extractPaths(svg) {
  const paths = [...svg.matchAll(/ d="([^"]+)"/g)].map((match) => match[1])

  if (paths.length === 0) {
    throw new Error("potrace no devolvió ningún path")
  }

  return paths
}

function buildSvg(paths, fill) {
  const body = paths
    .map((d) => `<path fill="${fill}" fill-rule="evenodd" d="${d}"/>`)
    .join("")

  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${CANVAS} ${CANVAS}">${body}</svg>\n`
}

/** favicon.svg: negro en tema claro, blanco en tema oscuro. */
function buildAdaptiveSvg(paths) {
  const body = paths.map((d) => `<path fill-rule="evenodd" d="${d}"/>`).join("")

  return (
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${CANVAS} ${CANVAS}">` +
    `<style>path{fill:#000}@media (prefers-color-scheme:dark){path{fill:#fff}}</style>` +
    `${body}</svg>\n`
  )
}

/** Rasteriza un SVG a PNG cuadrado, con padding y fondo opcionales. */
async function renderPng(svg, size, { padding = 0, background } = {}) {
  const inner = Math.round(size * (1 - padding * 2))
  const offset = Math.round((size - inner) / 2)

  const icon = await sharp(Buffer.from(svg), { density: 384 })
    .resize(inner, inner, {
      fit: "contain",
      background: { r: 0, g: 0, b: 0, alpha: 0 },
    })
    .png()
    .toBuffer()

  return sharp({
    create: {
      width: size,
      height: size,
      channels: 4,
      background: background ?? { r: 0, g: 0, b: 0, alpha: 0 },
    },
  })
    .composite([{ input: icon, top: offset, left: offset }])
    .png()
    .toBuffer()
}

async function main() {
  await mkdir(publicDir, { recursive: true })
  await mkdir(brandDir, { recursive: true })

  const normalized = await normalizeSource()
  const traced = await trace(normalized, {
    threshold: 128,
    turdSize: 160,
    optCurve: true,
    optTolerance: 0.8,
    alphaMax: 1,
    color: "#000000",
    background: "transparent",
  })
  const paths = extractPaths(traced)

  const blackSvg = buildSvg(paths, "#000000")
  const whiteSvg = buildSvg(paths, "#ffffff")

  // Path reutilizable por el componente React (se pinta con currentColor).
  await writeFile(
    path.join(brandDir, "tire-icon-path.ts"),
    `// Archivo generado por scripts/generate-icons.mjs — no editar a mano.\n` +
      `export const TIRE_ICON_VIEW_BOX = "0 0 ${CANVAS} ${CANVAS}"\n\n` +
      `export const TIRE_ICON_PATHS = [\n` +
      paths.map((d) => `  ${JSON.stringify(d)},\n`).join("") +
      `]\n`
  )

  await writeFile(path.join(publicDir, "favicon.svg"), buildAdaptiveSvg(paths))
  await writeFile(path.join(publicDir, "logo-light.svg"), blackSvg)
  await writeFile(path.join(publicDir, "logo-dark.svg"), whiteSvg)

  const white = { r: 255, g: 255, b: 255, alpha: 1 }

  const outputs = [
    ["icon-192.png", await renderPng(blackSvg, 192)],
    ["icon-512.png", await renderPng(blackSvg, 512)],
    ["icon-192-dark.png", await renderPng(whiteSvg, 192)],
    ["icon-512-dark.png", await renderPng(whiteSvg, 512)],
    [
      "icon-maskable-512.png",
      await renderPng(blackSvg, 512, { padding: 0.2, background: white }),
    ],
    [
      "apple-touch-icon.png",
      await renderPng(blackSvg, 180, { padding: 0.1, background: white }),
    ],
  ]

  for (const [name, buffer] of outputs) {
    await writeFile(path.join(publicDir, name), buffer)
  }

  const icoSizes = await Promise.all(
    [16, 32, 48].map((size) => renderPng(blackSvg, size))
  )
  await writeFile(path.join(publicDir, "favicon.ico"), await pngToIco(icoSizes))

  console.log(
    `Íconos generados (${paths.length} path(s)) en public/ y ${path.relative(root, brandDir)}/`
  )
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
