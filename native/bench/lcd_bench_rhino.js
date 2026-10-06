/**
 * lcd_bench_rhino.js — Rhino-compatible twin of lcd_bench.js.
 *
 * Same algorithm, same font, same frame content — but written with
 * the subset of ES6 Mozilla Rhino supports, and exposing
 * renderFrameExternal(ctx, state, wrapper) so the Java runner can
 * invoke it per frame exactly like JCM's ParsedScript does.
 *
 * Pixels use a plain JS Array of ints (stands in for the Java
 * GraphicsTexture buffer; each write costs the same as a JS array
 * write, without typed-array shortcuts Rhino may not support).
 */

var LCD_W = 128, LCD_H = 32;
var LCD_BG = 0x080400 | 0;
var LCD_ON = 0xffb000 | 0;
var LCD_DIM = 0x3a2600 | 0;
var LCD_GREEN = 0x00e676 | 0;

/* classic 5x7 font (same table as native/font5x7.hpp) */
var FONT = {
  " ": [0,0,0,0,0], "!": [0x00,0x00,0x5F,0x00,0x00], '"': [0x00,0x07,0x00,0x07,0x00],
  "#": [0x14,0x7F,0x14,0x7F,0x14], "$": [0x24,0x2A,0x7F,0x2A,0x12], "%": [0x23,0x13,0x08,0x64,0x62],
  "&": [0x36,0x49,0x55,0x22,0x50], "'": [0x00,0x05,0x03,0x00,0x00], "(": [0x00,0x1C,0x22,0x41,0x00],
  ")": [0x00,0x41,0x22,0x1C,0x00], "*": [0x08,0x2A,0x1C,0x2A,0x08], "+": [0x08,0x08,0x3E,0x08,0x08],
  ",": [0x00,0x50,0x30,0x00,0x00], "-": [0x08,0x08,0x08,0x08,0x08], ".": [0x00,0x60,0x60,0x00,0x00],
  "/": [0x20,0x10,0x08,0x04,0x02], "0": [0x3E,0x51,0x49,0x45,0x3E], "1": [0x00,0x42,0x7F,0x40,0x00],
  "2": [0x42,0x61,0x51,0x49,0x46], "3": [0x21,0x41,0x45,0x4B,0x31], "4": [0x18,0x14,0x12,0x7F,0x10],
  "5": [0x27,0x45,0x45,0x45,0x39], "6": [0x3C,0x4A,0x49,0x49,0x30], "7": [0x01,0x71,0x09,0x05,0x03],
  "8": [0x36,0x49,0x49,0x49,0x36], "9": [0x06,0x49,0x49,0x29,0x1E], ":": [0x00,0x36,0x36,0x00,0x00],
  ";": [0x00,0x56,0x36,0x00,0x00], "<": [0x08,0x14,0x22,0x41,0x00], "=": [0x14,0x14,0x14,0x14,0x14],
  ">": [0x00,0x41,0x22,0x14,0x08], "?": [0x02,0x01,0x51,0x09,0x06], "@": [0x32,0x49,0x79,0x41,0x3E],
  A: [0x7E,0x11,0x11,0x11,0x7E], B: [0x7F,0x49,0x49,0x49,0x36], C: [0x3E,0x41,0x41,0x41,0x22],
  D: [0x7F,0x41,0x41,0x22,0x1C], E: [0x7F,0x49,0x49,0x49,0x41], F: [0x7F,0x09,0x09,0x09,0x01],
  G: [0x3E,0x41,0x49,0x49,0x7A], H: [0x7F,0x08,0x08,0x08,0x7F], I: [0x00,0x41,0x7F,0x41,0x00],
  J: [0x20,0x40,0x41,0x3F,0x01], K: [0x7F,0x08,0x14,0x22,0x41], L: [0x7F,0x40,0x40,0x40,0x40],
  M: [0x7F,0x02,0x0C,0x02,0x7F], N: [0x7F,0x04,0x08,0x10,0x7F], O: [0x3E,0x41,0x41,0x41,0x3E],
  P: [0x7F,0x09,0x09,0x09,0x06], Q: [0x3E,0x41,0x51,0x21,0x5E], R: [0x7F,0x09,0x19,0x29,0x46],
  S: [0x46,0x49,0x49,0x49,0x31], T: [0x01,0x01,0x7F,0x01,0x01], U: [0x3F,0x40,0x40,0x40,0x3F],
  V: [0x1F,0x20,0x40,0x20,0x1F], W: [0x3F,0x40,0x38,0x40,0x3F], X: [0x63,0x14,0x08,0x14,0x63],
  Y: [0x07,0x08,0x78,0x08,0x07], Z: [0x61,0x51,0x49,0x45,0x43]
};

var CELL_W = 6, CELL_H = 8;

var state = {
  destinations: ["TSUEN WAN", "KWUN TONG"],
  destInterval: 120,
  runNumber: "T0389"
};

var stopNames = ["CENTRAL","ADMIRALTY","WAN CHAI","CAUSEWAY BAY","TIU KENG LENG","YAU TONG",
                 "LAM TIN","KWUN TONG","NGAU TAU KOK","KOWLOON BAY","CHOI HUNG","PO LAM"];
var stops = [];
for (var si = 0; si < 12; si++) {
  stops.push({ name: stopNames[si], distance: 400 * (si + 1) });
}

var carCount = 8;
var cars = [];
for (var ci = 0; ci < carCount; ci++) {
  cars.push({ leftDoorOpen: ci % 2 === 0, rightDoorOpen: ci % 2 === 1, rendered: true });
}

/* GraphicsTexture stand-in: plain JS Array */
var pixels = new Array(LCD_W * LCD_H);
for (var pi = 0; pi < pixels.length; pi++) pixels[pi] = LCD_BG;

var records = [];
var matrices = [1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1];

var frameCounter = 0;

function setPixel(x, y, argb) {
  if (x < 0 || y < 0 || x >= LCD_W || y >= LCD_H) return;
  pixels[y * LCD_W + x] = argb;
}

function drawText(xDot, yDot, str, onColor, offColor) {
  var x = xDot;
  for (var i = 0; i < str.length; i++) {
    var ch = str.charAt(i);
    var glyph = FONT[ch];
    if (ch === " ") { x += CELL_W; continue; }
    if (!glyph) { x += CELL_W; continue; }
    for (var col = 0; col < 5; col++) {
      var bits = glyph[col];
      for (var row = 0; row < 7; row++) {
        setPixel(x + col, yDot + row, (bits >> row) & 1 ? onColor : offColor);
      }
    }
    x += CELL_W;
  }
  return x - xDot;
}

function drawArrow(x, y, left) {
  for (var row = 0; row < 7; row++) {
    var d = row < 3 ? 3 - row : row - 3;
    var col = left ? 1 + d : 5 - d;
    setPixel(x + col, y + row, LCD_ON);
    setPixel(x + col + 1, y + row, LCD_ON);
  }
}

/* One frame — same content as native paint_lcd + draw loop. */
function renderFrame(gameMillis, gameTick, speedKmh, doorValue, nextStopIndex) {
  /* 1) paint LCD */
  for (var p = 0; p < pixels.length; p++) pixels[p] = LCD_BG;

  var dest = state.destinations[
    Math.floor(gameTick / state.destInterval) % state.destinations.length];
  drawText(2, 1, state.runNumber, LCD_GREEN, LCD_DIM);
  drawText(46, 1, dest, LCD_ON, LCD_DIM);

  if (nextStopIndex < stops.length) {
    drawText(2, 12, "NEXT: " + stops[nextStopIndex].name, LCD_ON, LCD_DIM);
  } else {
    drawText(2, 12, "TERMINUS - PLEASE EXIT", LCD_ON, LCD_DIM);
  }

  var blinkOn = Math.floor(gameMillis / 600) % 2 === 0;
  if (doorValue > 0 && blinkOn) {
    if (cars[0].leftDoorOpen) drawArrow(2, 24, true);
    if (cars[0].rightDoorOpen) drawArrow(120, 24, false);
  }
  var tail = Math.round(speedKmh) + " KMH";
  var tw = tail.length * CELL_W;
  drawText(LCD_W - 4 - tw, 24, tail, LCD_ON, LCD_DIM);

  /* 2) texture upload record */
  records.push({ kind: "upload", w: LCD_W, h: LCD_H, pixels: pixels });

  /* 3) car model draws (8 cars x 2 sides) with matrices */
  for (var car = 0; car < carCount; car++) {
    if (!cars[car].rendered) continue;
    for (var side = 0; side < 2; side++) {
      records.push({ kind: "model", car: car, matrices: matrices.slice() });
    }
  }
}

/* Entry point the Java runner calls per frame, mirroring
   ParsedScript.invokeRenderFunctions -> func.call(cx, scope, ...). */
function renderFrameExternal(ctx, stateObj, wrapper) {
  records = [];
  var i = frameCounter++;
  renderFrame(1730000000000 + i * 50, i, 80 + (i % 7), 0.85, 5);
}

/* Standalone run (bun / node) for engine-only comparison. */
if (typeof __RHINO_RUNNER__ === "undefined" &&
    typeof process !== "undefined" && typeof performance !== "undefined") {
  var frames = parseInt(process.argv[2] || "20000", 10);
  for (var w = 0; w < 200; w++) renderFrameExternal(null, null, null);
  var t0 = performance.now();
  for (var f = 0; f < frames; f++) renderFrameExternal(null, null, null);
  var t1 = performance.now();
  var ms = t1 - t0;
  console.log("frames=" + frames + " total=" + ms.toFixed(3) + " ms");
  console.log("per-frame=" + (ms * 1000 / frames).toFixed(2) + " us (" + (ms * 1e6 / frames).toFixed(0) + " ns)");
  console.log("draw-calls-last-frame=" + records.length);
}
