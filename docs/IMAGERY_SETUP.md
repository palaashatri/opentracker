# IMAGERY_SETUP.md
Configuring Earth Imagery for Digital Twin Earth (CesiumJS + Angular)

This guide explains how to correctly configure **imagery layers** in CesiumJS so the globe renders as a real Earth instead of a blue sphere.
All imagery sources listed here are **free**, **no-key**, and **MIT-compatible**.

---

## 1. Why the Earth Appears Blue

CesiumJS does **not** include a default basemap unless you explicitly add one.

If you see a blue globe, it means:

- No imagery provider was configured
- Cesium Ion was referenced without a key
- A provider failed to load due to CORS or missing credentials

To fix this, you must add at least one imagery layer.

---

## 2. Recommended Imagery Providers (Free, No API Key)

These providers work instantly and are safe for open-source projects.

---

### 2.1 OpenStreetMap (Project Default)

**Pros:** Free, no API key, political boundaries, cities, roads, reliable.

> **Note:** `OpenStreetMapImageryProvider` was deprecated in CesiumJS 1.104.
> Use `UrlTemplateImageryProvider` instead (already done in `globe-engine.service.ts`).

```ts
new Cesium.UrlTemplateImageryProvider({
  url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  credit: '© OpenStreetMap contributors',
  maximumLevel: 19,
})
```

---

### 2.2 Stadia Maps – Alidade Smooth (Clean Political Map)

**Pros:** Clean, modern cartography; free for light use; no key for basic tiles.

```ts
new Cesium.UrlTemplateImageryProvider({
  url: 'https://tiles.stadiamaps.com/tiles/alidade_smooth/{z}/{x}/{y}.png',
  maximumLevel: 20,
})
```

---

### 2.3 ESRI World Imagery (Satellite)

**Pros:** High-quality satellite imagery; free for non-commercial use; no key required.

```ts
new Cesium.UrlTemplateImageryProvider({
  url: 'https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
  maximumLevel: 19,
})
```

---

## 3. Optional: Add Terrain (Mountains, Elevation)

Cesium World Terrain is free and adds realistic elevation.
`createWorldTerrainAsync()` is the current API (async since CesiumJS 1.105).

```ts
Cesium.createWorldTerrainAsync().then(terrain => {
  viewer.terrainProvider = terrain;
});
```

---

## 4. Adding Multiple Imagery Layers

You can stack layers (e.g., satellite base + OSM labels):

```ts
const imageryLayers = viewer.imageryLayers;
imageryLayers.addImageryProvider(
  new Cesium.UrlTemplateImageryProvider({
    url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    maximumLevel: 19,
  })
);
```

---

## 5. Where This Lives in the Project

Imagery is configured in [globe-engine.service.ts](../frontend/digital-twin-earth-app/src/app/services/globe-engine.service.ts) inside the `init()` method, passed directly to the `Cesium.Viewer` constructor.

```ts
this.viewer = new Cesium.Viewer(container, {
  imageryProvider: new Cesium.UrlTemplateImageryProvider({
    url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    maximumLevel: 19,
  }),
  baseLayerPicker: false,
  // ...
});
```

Terrain is added asynchronously after the viewer is ready:

```ts
Cesium.createWorldTerrainAsync().then(terrain => {
  this.viewer.terrainProvider = terrain;
});
```

---

## 6. Troubleshooting

### Still seeing a blue globe?

1. Open browser DevTools → Network tab — check if tile requests are firing and succeeding
2. Check for CORS errors in the console
3. Verify `CESIUM_BASE_URL` is set to `/cesium` before `new Cesium.Viewer()` is called
4. Ensure the viewer is initialized inside `ngAfterViewInit()`, not `ngOnInit()`

### Tiles load but globe is blurry?

```ts
viewer.resolutionScale = window.devicePixelRatio;
```

### Imagery disappears when zooming?

Your tile provider may not support high zoom levels. OSM supports up to `maximumLevel: 19`; ESRI supports up to 23 in most areas.

---

## 7. Recommended Defaults for This Project

For a global traffic viewer (flights, ships, satellites), the best setup is:

| Layer | Provider | Key required |
|---|---|---|
| Base map | OpenStreetMap | No |
| Terrain | Cesium World Terrain | No |
| Labels | OSM (included in base) | No |

This gives clear political boundaries, readable city labels, realistic terrain, and no licensing issues.

---

## 8. License Notes

| Source | License |
|---|---|
| OpenStreetMap tiles | ODbL (Open Database License) — attribution required |
| Cesium World Terrain | Cesium ion Terms of Service (free tier) |
| Stadia Maps | Stadia Maps Terms (free for low-volume open-source) |
| ESRI World Imagery | ESRI Terms of Use (free for non-commercial) |

All sources are compatible with an MIT-licensed open-source project.
Always check provider terms before commercial distribution.
