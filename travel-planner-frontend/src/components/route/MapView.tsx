import { useEffect } from 'react'
import { MapContainer, TileLayer, Polyline, Marker, Popup, useMap } from 'react-leaflet'
import L from 'leaflet'
import type { AttractionResponse } from '../../types'

// Fix Leaflet default marker icons broken by bundlers
delete (L.Icon.Default.prototype as any)._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
})

interface MapViewProps {
  geometry: [number, number][]
  attractions: AttractionResponse[]
}

function FitBounds({ geometry }: { geometry: [number, number][] }) {
  const map = useMap()
  useEffect(() => {
    if (geometry.length > 0) {
      map.fitBounds(geometry, { padding: [40, 40] })
    }
  }, [map, geometry])
  return null
}

export default function MapView({ geometry, attractions }: MapViewProps) {
  const center: [number, number] = geometry.length > 0
    ? geometry[Math.floor(geometry.length / 2)]
    : [25.0330, 121.5654]  // Taipei default

  const markers = attractions.filter(a => a.latitude != null && a.longitude != null)

  return (
    <MapContainer
      center={center}
      zoom={13}
      style={{ height: '400px', width: '100%', borderRadius: '0.5rem' }}
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {geometry.length > 1 && (
        <Polyline positions={geometry} color="#3b82f6" weight={4} opacity={0.8} />
      )}

      {markers.map((a, idx) => (
        <Marker key={a.id} position={[a.latitude!, a.longitude!]}>
          <Popup>
            <div className="text-sm font-medium">{idx + 1}. {a.name}</div>
            <div className="text-xs text-gray-500">{a.startTime} – {a.endTime}</div>
          </Popup>
        </Marker>
      ))}

      {geometry.length > 0 && <FitBounds geometry={geometry} />}
    </MapContainer>
  )
}
