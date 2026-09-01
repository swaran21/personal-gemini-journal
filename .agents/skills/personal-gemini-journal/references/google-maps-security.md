# Google Maps security directive

Use this directive before adding or changing Maps functionality.

1. Prefer keyless Google Maps URLs for opening an approved coordinate pin when an embedded map is unnecessary.
2. Request browser geolocation only after an explicit user gesture. Location is optional; denial must not block journaling.
3. Validate finite latitude `[-90,90]`, longitude `[-180,180]`, and a bounded plain-text label. Never accept UID with location data.
4. Do not infer home/work, continuously track, reverse-geocode, or send coordinates to AI unless the UI obtains explicit purpose-specific consent.
5. Treat a Maps JavaScript API key as public configuration, not a secret: restrict it by exact HTTPS referrers and only required APIs, use separate dev/prod keys, set quotas, and never grant server API access.
6. Keep server-side Maps keys in Google Secret Manager and access them through workload identity. Never place them in Vite variables, source, logs, URLs, or container layers.
7. Use separate browser and server keys. Rotate after suspected exposure and monitor quota anomalies.
8. Add Maps Platform billing alerts and conservative quotas before enabling billed APIs. A keyless Maps URL does not require Maps API billing.
9. Export and delete approved location data with the owning journal entry. Do not include it in audit logs.
